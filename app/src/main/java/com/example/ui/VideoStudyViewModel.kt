package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ProposedStatus { PROPOSED, ACCEPTED, REJECTED, LOW_CONFIDENCE }

data class ProposedElement(
    val id: String,
    val name: String,
    val description: String,
    val startTime: Double,
    val endTime: Double,
    val classification: ValueClassification,
    val wasteCategory: WasteCategory,
    val confidence: Double,
    val status: ProposedStatus = ProposedStatus.PROPOSED,
    val tools: List<String> = emptyList(),
    val materials: List<String> = emptyList(),
    val notes: String = ""
) {
    val duration: Double get() = endTime - startTime
}

class VideoStudyViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _videoUri = MutableStateFlow<String?>(null)
    val videoUri: StateFlow<String?> = _videoUri.asStateFlow()

    private val _proposedElements = MutableStateFlow<List<ProposedElement>>(emptyList())
    val proposedElements: StateFlow<List<ProposedElement>> = _proposedElements.asStateFlow()

    private val _videoTime = MutableStateFlow(0.0)
    val videoTime: StateFlow<Double> = _videoTime.asStateFlow()
    
    private val _duration = MutableStateFlow(60.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0)
    val playbackSpeed: StateFlow<Double> = _playbackSpeed.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun uploadVideo() {
        _videoUri.value = "mock_assembly_video.mp4"
        _videoTime.value = 0.0
        _proposedElements.value = emptyList()
    }

    fun simulateAiAnalysis() {
        if (_videoUri.value == null) return
        _isAnalyzing.value = true
        
        viewModelScope.launch {
            delay(2000)
            // Mocking a Gemini Video Understanding API response
            _proposedElements.value = listOf(
                ProposedElement(
                    id = UUID.randomUUID().toString(),
                    name = "Retrieve housing from bin",
                    description = "Operator reaches for and grasps housing.",
                    startTime = 0.5,
                    endTime = 2.5,
                    classification = ValueClassification.NNVA,
                    wasteCategory = WasteCategory.MOTION,
                    confidence = 0.95,
                    status = ProposedStatus.PROPOSED,
                    materials = listOf("Housing")
                ),
                ProposedElement(
                    id = UUID.randomUUID().toString(),
                    name = "Position housing in fixture",
                    description = "Operator places housing onto the assembly fixture.",
                    startTime = 2.5,
                    endTime = 4.0,
                    classification = ValueClassification.NNVA,
                    wasteCategory = WasteCategory.NONE,
                    confidence = 0.88,
                    status = ProposedStatus.PROPOSED
                ),
                ProposedElement(
                    id = UUID.randomUUID().toString(),
                    name = "Fasten 4 screws",
                    description = "Operator drives 4 screws using torque tool.",
                    startTime = 4.0,
                    endTime = 11.5,
                    classification = ValueClassification.VA,
                    wasteCategory = WasteCategory.NONE,
                    confidence = 0.92,
                    status = ProposedStatus.PROPOSED,
                    tools = listOf("Torque Wrench"),
                    materials = listOf("Screws x4")
                ),
                ProposedElement(
                    id = UUID.randomUUID().toString(),
                    name = "Unclear movement / Inspection",
                    description = "Operator turns away. Low confidence — IE validation required.",
                    startTime = 11.5,
                    endTime = 14.0,
                    classification = ValueClassification.NVA,
                    wasteCategory = WasteCategory.WAITING,
                    confidence = 0.45,
                    status = ProposedStatus.LOW_CONFIDENCE,
                    notes = "Low confidence — IE validation required."
                ),
                ProposedElement(
                    id = UUID.randomUUID().toString(),
                    name = "Move to next station",
                    description = "Moves assembled part to downstream conveyor.",
                    startTime = 14.0,
                    endTime = 16.0,
                    classification = ValueClassification.NNVA,
                    wasteCategory = WasteCategory.TRANSPORTATION,
                    confidence = 0.98,
                    status = ProposedStatus.PROPOSED
                )
            )
            _isAnalyzing.value = false
        }
    }

    fun acceptElement(id: String) {
        val element = _proposedElements.value.find { it.id == id } ?: return
        
        _proposedElements.update { list ->
            list.map { if (it.id == id) it.copy(status = ProposedStatus.ACCEPTED) else it }
        }
        
        // Commit to repository
        val project = repository.projects.firstOrNull() ?: return
        val we = WorkElement(
            id = "WE-AI-${System.currentTimeMillis()}-${(0..999).random()}",
            projectId = project.id,
            modelId = repository.models.firstOrNull { it.projectId == project.id }?.id ?: "MDL-1",
            processId = repository.processes.firstOrNull()?.id ?: "PRC-1",
            stationId = repository.stations.firstOrNull()?.id ?: "ST-04",
            operatorId = repository.operators.firstOrNull()?.id ?: "OP-1",
            sequence = (repository.workElements.maxOfOrNull { it.sequence } ?: 0) + 10,
            name = element.name,
            description = "AI Extracted: ${element.description}",
            startTime = element.startTime,
            endTime = element.endTime,
            observedTime = element.duration,
            normalTime = element.duration,
            standardTime = element.duration * 1.1, // 10% allowance assumption
            performanceRating = 1.0,
            allowance = 0.1,
            valueClassification = element.classification,
            wasteCategory = element.wasteCategory,
            toolIds = element.tools,
            materialIds = element.materials,
            predecessorIds = emptyList(),
            successorIds = emptyList(),
            transferable = true,
            combinable = true,
            parallelizable = false,
            eliminable = false,
            automationOpportunity = false,
            confidence = element.confidence,
            evidenceReference = "Video_Timestamp_${element.startTime}",
            notes = element.notes,
            timeSource = TimeSource.OBSERVED
        )
        try {
            repository.addWorkElement(we)
            // Add a corresponding observation to the repo
            val newObs = Observation(
                id = "OBS-AI-${System.currentTimeMillis()}-${(0..999).random()}",
                workElementId = we.id,
                cycleId = "CYC-AI-1",
                observedTime = element.duration,
                notes = "From AI Video Analysis"
            )
            repository.observations.add(newObs)
        } catch (e: Exception) {
            // Ignore for mock safety if relationships are missing
        }
    }
    
    fun editElement(id: String, newName: String, newStart: Double, newEnd: Double, classification: ValueClassification) {
        _proposedElements.update { list ->
            list.map { 
                if (it.id == id) {
                    it.copy(name = newName, startTime = newStart, endTime = newEnd, classification = classification, status = ProposedStatus.PROPOSED)
                } else it 
            }
        }
    }

    fun rejectElement(id: String) {
        _proposedElements.update { list ->
            list.map { if (it.id == id) it.copy(status = ProposedStatus.REJECTED) else it }
        }
    }
    
    fun togglePlay() { _isPlaying.value = !_isPlaying.value }
    fun stepForward() { _videoTime.value = minOf(_duration.value, _videoTime.value + (1.0 / 30.0)) }
    fun stepBackward() { _videoTime.value = maxOf(0.0, _videoTime.value - (1.0 / 30.0)) }
    fun seek(time: Double) { _videoTime.value = time.coerceIn(0.0, _duration.value) }
    fun setSpeed(speed: Double) { _playbackSpeed.value = speed }
}
