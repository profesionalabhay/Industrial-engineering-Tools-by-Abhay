package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun loadStudy(studyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val study = repository.videoStudies.find { it.id == studyId }
            val templates = repository.timeStudyTemplates
            val observations = repository.videoCandidates.filter { it.studyId == studyId }
            
            _uiState.value = _uiState.value.copy(
                study = study,
                templates = templates,
                selectedTemplate = templates.firstOrNull(),
                observations = observations,
                isLoading = false,
                currentCycle = observations.maxOfOrNull { it.cycleNumber } ?: 1
            )
        }
    }

    fun updateTimestamp(timestamp: Double) {
        _uiState.value = _uiState.value.copy(currentTimestamp = timestamp)
    }

    fun togglePlayback() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun selectTemplate(template: TimeStudyTemplate) {
        _uiState.value = _uiState.value.copy(selectedTemplate = template)
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
        
        repository.videoCandidates.add(newObservation)
        
        _uiState.value = state.copy(
            observations = state.observations + newObservation,
            lastMarkedTimestamp = currentTs
        )
    }

    fun startNewCycle() {
        _uiState.value = _uiState.value.copy(
            currentCycle = _uiState.value.currentCycle + 1,
            lastMarkedTimestamp = null
        )
    }
    
    fun deleteObservation(obsId: String) {
        repository.videoCandidates.removeAll { it.id == obsId }
        _uiState.value = _uiState.value.copy(
            observations = _uiState.value.observations.filter { it.id != obsId }
        )
    }
}
