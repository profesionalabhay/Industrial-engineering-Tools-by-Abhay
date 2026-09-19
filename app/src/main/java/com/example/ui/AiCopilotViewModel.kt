package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.*
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun initialize(projectId: String) {
        viewModelScope.launch {
            val providers = repository.getAiProviders()
            val models = repository.getAiModels()
            val history = repository.getAiChatMessages(projectId)
            
            val defaultProvider = providers.find { it.isActive && it.type == AIProviderType.GEMINI } ?: providers.firstOrNull()
            val defaultModel = models.find { it.providerId == defaultProvider?.id && it.isDefault } ?: models.find { it.providerId == defaultProvider?.id }

            _uiState.value = _uiState.value.copy(
                messages = history,
                providers = providers,
                models = models,
                selectedProvider = defaultProvider,
                selectedModel = defaultModel,
                currentProjectId = projectId
            )
        }
    }

    fun selectProvider(provider: AIProviderConfig) {
        val models = _uiState.value.models.filter { it.providerId == provider.id }
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            selectedModel = models.find { it.isDefault } ?: models.firstOrNull()
        )
    }

    fun selectModel(model: AIModelConfig) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
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

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isAnalyzing = true,
            error = null
        )
        repository.saveAiChatMessage(userMessage)

        viewModelScope.launch {
            // Gather all IE context
            val project = repository.projects.find { it.id == projectId } ?: return@launch
            val models = repository.models.filter { it.projectId == projectId }
            val stations = repository.stations.filter { it.processId == (repository.processes.find { it.lineId == project.lineId }?.id ?: "") }
            val elements = repository.workElements.filter { it.projectId == projectId }
            val plan = repository.productionPlans.find { it.projectId == projectId }
            val mixItems = plan?.let { repository.getModelMixForPlan(it.id) } ?: emptyList()

            // OpEx Context
            val oeeRecords = repository.getOeeRecords(projectId)
            val oeeMetrics = oeeRecords.map { repository.calculateOeeMetrics(it) }
            val losses = oeeRecords.flatMap { repository.getLossEvents(it.id) }
            val rcas = repository.getRcaRecords(projectId)
            val kaizens = repository.getKaizenRecords(projectId)

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
                history = _uiState.value.messages.dropLast(1), // excluding current user msg already handled
                modelConfig = model,
                providerConfig = provider
            )

            result.onSuccess { assistantMsg ->
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMsg,
                    isAnalyzing = false
                )
                repository.saveAiChatMessage(assistantMsg)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = "AI Analysis Failed: ${e.message}"
                )
            }
        }
    }

    fun clearHistory() {
        val projectId = _uiState.value.currentProjectId ?: return
        repository.deleteAiHistory(projectId)
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }
}
