package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.*
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class AiCopilotUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val providers: List<AIProviderConfig> = emptyList(),
    val models: List<AIModelConfig> = emptyList(),
    val selectedProvider: AIProviderConfig? = null,
    val selectedModel: AIModelConfig? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val currentProjectId: String? = null
)

class AiCopilotViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AiCopilotUiState())
    val uiState: StateFlow<AiCopilotUiState> = _uiState.asStateFlow()

    private val aiEngine = AIEngine(AIContextBuilder())
    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize(projectId: String) {
        _uiState.update { it.copy(currentProjectId = projectId) }
        
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            // Collect all necessary Flows
            combine(
                repository.getAiProviders(),
                repository.getAiModels(),
                repository.getAiChatMessages(projectId)
            ) { providers, models, history ->
                val defaultProvider = providers.find { it.isActive && it.type == AIProviderType.GEMINI } ?: providers.firstOrNull()
                val defaultModel = models.find { it.providerId == defaultProvider?.id && it.isDefault } ?: models.find { it.providerId == defaultProvider?.id }

                _uiState.update { it.copy(
                    messages = history,
                    providers = providers,
                    models = models,
                    selectedProvider = it.selectedProvider ?: defaultProvider,
                    selectedModel = it.selectedModel ?: defaultModel
                ) }
            }.collect()
        }
    }

    fun selectProvider(provider: AIProviderConfig) {
        viewModelScope.launch {
            val models = repository.getAiModels().first().filter { it.providerId == provider.id }
            _uiState.update { it.copy(
                selectedProvider = provider,
                selectedModel = models.find { it.isDefault } ?: models.firstOrNull()
            ) }
        }
    }

    fun selectModel(model: AIModelConfig) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun sendMessage(content: String) {
        val projectId = _uiState.value.currentProjectId ?: return
        val provider = _uiState.value.selectedProvider ?: return
        val model = _uiState.value.selectedModel ?: return

        val userMessage = AiChatMessage(
            id = "user-${UUID.randomUUID()}",
            projectId = projectId,
            role = AiRole.USER,
            content = content
        )

        _uiState.update { it.copy(isAnalyzing = true, error = null) }
        
        viewModelScope.launch {
            repository.saveAiChatMessage(userMessage)

            // Gather all IE context from database Flows (take first for current snapshot)
            val project = repository.getProjectById(projectId) ?: return@launch
            val models = repository.getModelsForProject(projectId).first()
            val processes = repository.getProcessesForLine(project.lineId).first()
            val stations = repository.getAllStations().first().filter { st -> processes.any { it.id == st.processId } }
            val elements = repository.getWorkElementsForProject(projectId).first()
            val plan = repository.getProductionPlans(projectId).first().firstOrNull()
            val mixItems = plan?.let { repository.getModelMixForPlan(it.id).first() } ?: emptyList()

            // OpEx Context
            val oeeRecords = repository.getOeeRecords(projectId).first()
            val oeeMetrics = oeeRecords.map { repository.calculateOeeMetrics(it) }
            val losses = oeeRecords.flatMap { repository.getLossEvents(it.id).first() }
            val rcas = repository.getRcaRecords(projectId).first()
            val kaizens = repository.getAllKaizenRecords().first()

            val result = aiEngine.analyze(
                prompt = content,
                project = project,
                models = models,
                stations = stations,
                elements = elements,
                plan = plan,
                mixItems = mixItems,
                oee = oeeMetrics,
                losses = losses,
                rcas = rcas,
                kaizens = kaizens,
                history = _uiState.value.messages,
                modelConfig = model,
                providerConfig = provider
            )

            result.onSuccess { assistantMsg ->
                repository.saveAiChatMessage(assistantMsg)
                _uiState.update { it.copy(isAnalyzing = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(
                    isAnalyzing = false,
                    error = "AI Analysis Failed: ${e.message}"
                ) }
            }
        }
    }

    fun clearHistory() {
        val projectId = _uiState.value.currentProjectId ?: return
        viewModelScope.launch {
            repository.deleteAiHistory(projectId)
        }
    }
}
