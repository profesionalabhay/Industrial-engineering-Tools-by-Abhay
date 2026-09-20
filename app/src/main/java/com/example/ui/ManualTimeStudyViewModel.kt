package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ManualTimeStudyUiState(
    val study: VideoStudyMetadata? = null,
    val templates: List<TimeStudyTemplate> = emptyList(),
    val selectedTemplate: TimeStudyTemplate? = null,
    val observations: List<AICandidateElement> = emptyList(),
    val currentTimestamp: Double = 0.0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val currentCycle: Int = 1,
    val isLoading: Boolean = false,
    val lastMarkedTimestamp: Double? = null
)

class ManualTimeStudyViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ManualTimeStudyUiState())
    val uiState: StateFlow<ManualTimeStudyUiState> = _uiState.asStateFlow()

    private var observationsJob: kotlinx.coroutines.Job? = null

    fun initialize(studyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val study = repository.getVideoStudyById(studyId)
            
            // Collect templates
            val templates = repository.getAllTimeStudyTemplates().first()
            
            _uiState.update { it.copy(
                study = study,
                templates = templates,
                selectedTemplate = templates.firstOrNull(),
                isLoading = false
            ) }

            // Observe observations reactively
            observationsJob?.cancel()
            observationsJob = repository.getVideoCandidates(studyId).onEach { obs ->
                _uiState.update { it.copy(
                    observations = obs,
                    currentCycle = obs.maxOfOrNull { o -> o.cycleNumber } ?: 1
                ) }
            }.launchIn(viewModelScope)
        }
    }

    fun updateTimestamp(timestamp: Double) {
        _uiState.update { it.copy(currentTimestamp = timestamp) }
    }

    fun togglePlayback() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun selectTemplate(template: TimeStudyTemplate) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun markElement() {
        val state = _uiState.value
        val study = state.study ?: return
        val template = state.selectedTemplate ?: return
        val currentTs = state.currentTimestamp
        
        val startTime = state.lastMarkedTimestamp ?: 0.0
        val duration = (currentTs - startTime).coerceAtLeast(0.0)
        
        val newObservation = AICandidateElement(
            id = "MAN-${UUID.randomUUID()}",
            studyId = study.id,
            cycleNumber = state.currentCycle,
            sequence = (state.observations.size + 1) * 10,
            name = template.name,
            startTime = startTime,
            endTime = currentTs,
            duration = duration,
            activityCategory = template.activityCategory,
            suggestedClassification = template.defaultClassification,
            finalClassification = template.defaultClassification,
            wasteCategory = template.defaultWasteCategory,
            validationStatus = ValidationStatus.USER_VALIDATED,
            notes = "Manual observation"
        )
        
        viewModelScope.launch {
            repository.insertVideoCandidate(newObservation)
            _uiState.update { it.copy(lastMarkedTimestamp = currentTs) }
        }
    }

    fun startNewCycle() {
        _uiState.update { it.copy(
            currentCycle = it.currentCycle + 1,
            lastMarkedTimestamp = null
        ) }
    }
    
    fun deleteObservation(obsId: String) {
        viewModelScope.launch {
            repository.deleteVideoCandidate(obsId)
        }
    }
}
