package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MotionViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)
    private val _workElementId = MutableStateFlow<String?>(null)

    val activeStudy: StateFlow<MotionStudy?> = combine(_projectId.filterNotNull(), _workElementId.filterNotNull()) { pid, eid ->
        repository.getMotionStudies(pid).map { list -> list.find { it.workElementId == eid } }
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isAnalyzingVideo = MutableStateFlow(false)
    val isAnalyzingVideo: StateFlow<Boolean> = _isAnalyzingVideo.asStateFlow()

    private val _aiSuggestions = MutableStateFlow<List<MotionEvent>>(emptyList())
    val aiSuggestions: StateFlow<List<MotionEvent>> = _aiSuggestions.asStateFlow()

    fun initialize(projectId: String, workElementId: String) {
        _projectId.value = projectId
        _workElementId.value = workElementId
    }

    fun addMotion(category: MotionCategory, description: String, timeSec: Double) {
        val study = activeStudy.value ?: return
        val newEvent = MotionEvent(
            id = UUID.randomUUID().toString(),
            workElementId = study.workElementId,
            therblig = category.name,
            timeMs = (timeSec * 1000).toLong(),
            description = description
        )
        viewModelScope.launch {
            repository.insertMotionStudy(study.copy(events = study.events + newEvent))
        }
    }

    fun deleteMotion(id: String) {
        val study = activeStudy.value ?: return
        viewModelScope.launch {
            repository.insertMotionStudy(study.copy(events = study.events.filter { it.id != id }))
        }
    }

    fun analyzeVideoMock(videoDescription: String) {
        _isAnalyzingVideo.value = true
        viewModelScope.launch {
            delay(2000) // Simulate Gemini Video Analysis
            
            val suggestions = listOf(
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.REACH.name, timeMs = 1200, description = "Reach for screw"),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.GRASP.name, timeMs = 500, description = "Grasp screw"),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.MOVE.name, timeMs = 800, description = "Move to assembly"),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.POSITION.name, timeMs = 1500, description = "Position screw"),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.ASSEMBLE.name, timeMs = 4200, description = "Drive screw"),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = MotionCategory.RELEASE.name, timeMs = 1000, description = "Release screwdriver")
            )
            _aiSuggestions.value = suggestions
            _isAnalyzingVideo.value = false
        }
    }

    fun acceptSuggestion(event: MotionEvent) {
        val study = activeStudy.value ?: return
        viewModelScope.launch {
            repository.insertMotionStudy(study.copy(events = study.events + event))
            _aiSuggestions.update { list -> list.filter { it.id != event.id } }
        }
    }

    fun rejectSuggestion(eventId: String) {
        _aiSuggestions.update { list -> list.filter { it.id != eventId } }
    }
    
    fun clearSuggestions() {
        _aiSuggestions.value = emptyList()
    }
}
