package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class MotionCategory(val isValueAdding: Boolean) {
    REACH(false),
    PICK(false),
    PLACE(true),
    WALK(false),
    TURN(false),
    BEND(false),
    SEARCH(false),
    WAIT(false),
    TOOL_MOVEMENT(true),
    MATERIAL_MOVEMENT(false),
    INSPECTION(false), // Often NNVA
    HOLD(false)
}

data class MotionElement(
    val id: String = UUID.randomUUID().toString(),
    val category: MotionCategory,
    val description: String,
    val timeSec: Double
)

class MotionViewModel : ViewModel() {
    private val _motions = MutableStateFlow<List<MotionElement>>(emptyList())
    val motions: StateFlow<List<MotionElement>> = _motions.asStateFlow()

    private val _isAnalyzingVideo = MutableStateFlow(false)
    val isAnalyzingVideo: StateFlow<Boolean> = _isAnalyzingVideo.asStateFlow()

    private val _aiSuggestions = MutableStateFlow<List<MotionElement>>(emptyList())
    val aiSuggestions: StateFlow<List<MotionElement>> = _aiSuggestions.asStateFlow()

    fun addMotion(category: MotionCategory, description: String, timeSec: Double) {
        val newMotion = MotionElement(category = category, description = description, timeSec = timeSec)
        _motions.update { it + newMotion }
    }

    fun deleteMotion(id: String) {
        _motions.update { list -> list.filter { it.id != id } }
    }

    fun analyzeVideoMock(videoDescription: String) {
        _isAnalyzingVideo.value = true
        viewModelScope.launch {
            delay(2000) // Simulate Gemini Video Analysis
            
            val suggestions = listOf(
                MotionElement(category = MotionCategory.REACH, description = "Reach for drill on top shelf", timeSec = 1.2),
                MotionElement(category = MotionCategory.PICK, description = "Grasp drill", timeSec = 0.5),
                MotionElement(category = MotionCategory.TURN, description = "Turn back to workbench", timeSec = 0.8),
                MotionElement(category = MotionCategory.WALK, description = "Walk 2 steps to assembly fixture", timeSec = 1.5),
                MotionElement(category = MotionCategory.TOOL_MOVEMENT, description = "Drive 3 screws", timeSec = 4.2),
                MotionElement(category = MotionCategory.PLACE, description = "Return drill to holster", timeSec = 1.0)
            )
            _aiSuggestions.value = suggestions
            _isAnalyzingVideo.value = false
        }
    }

    fun acceptSuggestion(motion: MotionElement) {
        _motions.update { it + motion }
        _aiSuggestions.update { list -> list.filter { it.id != motion.id } }
    }

    fun rejectSuggestion(motionId: String) {
        _aiSuggestions.update { list -> list.filter { it.id != motionId } }
    }
    
    fun clearSuggestions() {
        _aiSuggestions.value = emptyList()
    }
}
