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

    fun addMotion(therblig: String, timeMs: Long) {
        val study = activeStudy.value ?: return
        val newEvent = MotionEvent(
            id = UUID.randomUUID().toString(),
            workElementId = study.workElementId,
            therblig = therblig,
            timeMs = timeMs
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
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "REACH", timeMs = 1200),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "GRASP", timeMs = 500),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "MOVE", timeMs = 800),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "POSITION", timeMs = 1500),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "ASSEMBLE", timeMs = 4200),
                MotionEvent(id = UUID.randomUUID().toString(), workElementId = _workElementId.value ?: "", therblig = "RELEASE", timeMs = 1000)
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
