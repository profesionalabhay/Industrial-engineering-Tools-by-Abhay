package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.VideoStudyEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class CandidateFilter {
    ALL,
    AI_SUGGESTED,
    USER_VALIDATED,
    USER_EDITED,
    REJECTED,
    ABNORMAL
}

class VideoStudyViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _videoUri = MutableStateFlow<String?>("workstation_st04_cycle_study.mp4")
    val videoUri: StateFlow<String?> = _videoUri.asStateFlow()

    private val _selectedStudyId = MutableStateFlow("VS-001")
    val selectedStudyId: StateFlow<String> = _selectedStudyId.asStateFlow()

    private val _studies = MutableStateFlow<List<VideoStudyMetadata>>(emptyList())
    val studies: StateFlow<List<VideoStudyMetadata>> = _studies.asStateFlow()

    private val _activeStudy = MutableStateFlow<VideoStudyMetadata?>(null)
    val activeStudy: StateFlow<VideoStudyMetadata?> = _activeStudy.asStateFlow()

    private val _candidates = MutableStateFlow<List<AICandidateElement>>(emptyList())
    val candidates: StateFlow<List<AICandidateElement>> = _candidates.asStateFlow()

    private val _cycles = MutableStateFlow<List<StudyCycle>>(emptyList())
    val cycles: StateFlow<List<StudyCycle>> = _cycles.asStateFlow()

    private val _selectedCycleNumber = MutableStateFlow<Int?>(null)
    val selectedCycleNumber: StateFlow<Int?> = _selectedCycleNumber.asStateFlow()

    private val _filter = MutableStateFlow(CandidateFilter.ALL)
    val filter: StateFlow<CandidateFilter> = _filter.asStateFlow()

    private val _selectedCandidateId = MutableStateFlow<String?>(null)
    val selectedCandidateId: StateFlow<String?> = _selectedCandidateId.asStateFlow()

    // Video Player State
    private val _videoTime = MutableStateFlow(0.0)
    val videoTime: StateFlow<Double> = _videoTime.asStateFlow()

    private val _duration = MutableStateFlow(64.5)
    val duration: StateFlow<Double> = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0)
    val playbackSpeed: StateFlow<Double> = _playbackSpeed.asStateFlow()

    private val _startMarker = MutableStateFlow<Double?>(null)
    val startMarker: StateFlow<Double?> = _startMarker.asStateFlow()

    private val _endMarker = MutableStateFlow<Double?>(null)
    val endMarker: StateFlow<Double?> = _endMarker.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadProjectStudies("P-001")
    }

    fun loadProjectStudies(projectId: String) {
        val loadedStudies = repository.getVideoStudies(projectId)
        _studies.value = loadedStudies
        val currentStudy = loadedStudies.find { it.id == _selectedStudyId.value } ?: loadedStudies.firstOrNull()
        if (currentStudy != null) {
            selectStudy(currentStudy.id)
        }
    }

    fun selectStudy(studyId: String) {
        _selectedStudyId.value = studyId
        val study = repository.videoStudies.find { it.id == studyId }
        _activeStudy.value = study
        if (study != null) {
            _duration.value = study.videoDurationSeconds
            _videoUri.value = study.videoFileName
            _candidates.value = repository.getVideoCandidates(studyId).sortedBy { it.startTime }
            _cycles.value = repository.getStudyCycles(studyId).sortedBy { it.cycleNumber }
        }
    }

    // Filtered candidates based on selection
    val filteredCandidates: StateFlow<List<AICandidateElement>> = combine(
        _candidates,
        _filter,
        _selectedCycleNumber
    ) { candidateList, filterType, cycleNum ->
        candidateList.filter { cand ->
            val matchesCycle = cycleNum == null || cand.cycleNumber == cycleNum
            val matchesFilter = when (filterType) {
                CandidateFilter.ALL -> true
                CandidateFilter.AI_SUGGESTED -> cand.validationStatus == ValidationStatus.AI_SUGGESTED
                CandidateFilter.USER_VALIDATED -> cand.validationStatus == ValidationStatus.USER_VALIDATED
                CandidateFilter.USER_EDITED -> cand.validationStatus == ValidationStatus.USER_EDITED
                CandidateFilter.REJECTED -> cand.validationStatus == ValidationStatus.REJECTED
                CandidateFilter.ABNORMAL -> cand.isAbnormal
            }
            matchesCycle && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Cycle statistics calculated deterministically by VideoStudyEngine
    val cycleStats: StateFlow<CycleStatistics> = combine(_cycles, _candidates) { cycleList, candidateList ->
        VideoStudyEngine.calculateCycleStatistics(cycleList, candidateList)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CycleStatistics(
            cycleCount = 0, validCycleCount = 0, excludedCycleCount = 0,
            averageCycleTime = 0.0, minCycleTime = 0.0, maxCycleTime = 0.0, medianCycleTime = 0.0, rangeCycleTime = 0.0,
            stdDevCycleTime = null, cvPercent = null, averageVaTime = 0.0, averageNnvaTime = 0.0, averageNvaTime = 0.0,
            vaPercent = 0.0, nnvaPercent = 0.0, nvaPercent = 0.0
        )
    )

    // Improvement Opportunities Scanner
    val opportunities: StateFlow<List<VideoImprovementOpportunity>> = combine(_candidates, _cycles, cycleStats) { candList, cycleList, stats ->
        val studyId = _selectedStudyId.value
        VideoStudyEngine.scanImprovementOpportunities(studyId, candList, cycleList, stats)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Strict 7-Section AI Report
    val aiReport: StateFlow<VideoStudyAiReport?> = combine(_activeStudy, _candidates, _cycles, cycleStats, opportunities) { study, candList, cycleList, stats, opps ->
        study?.let {
            VideoStudyEngine.generateAiReport(it, candList, cycleList, stats, opps)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // -------------------------------------------------------------------------
    // VIDEO PLAYBACK CONTROLS
    // -------------------------------------------------------------------------

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun stepForward() {
        _videoTime.value = minOf(_duration.value, _videoTime.value + (1.0 / 30.0))
    }

    fun stepBackward() {
        _videoTime.value = maxOf(0.0, _videoTime.value - (1.0 / 30.0))
    }

    fun seek(time: Double) {
        _videoTime.value = time.coerceIn(0.0, _duration.value)
    }

    fun setSpeed(speed: Double) {
        _playbackSpeed.value = speed
    }

    fun setStartMarker() {
        _startMarker.value = _videoTime.value
        _statusMessage.value = "Start marker set at ${String.format(Locale.US, "%.2fs", _videoTime.value)}"
    }

    fun setEndMarker() {
        val current = _videoTime.value
        val start = _startMarker.value ?: 0.0
        if (current >= start) {
            _endMarker.value = current
            _statusMessage.value = "End marker set at ${String.format(Locale.US, "%.2fs", current)}"
        } else {
            _statusMessage.value = "End marker cannot precede start marker."
        }
    }

    fun clearMarkers() {
        _startMarker.value = null
        _endMarker.value = null
    }

    fun selectCandidate(candidateId: String?) {
        _selectedCandidateId.value = candidateId
        val cand = _candidates.value.find { it.id == candidateId }
        if (cand != null) {
            seek(cand.startTime)
        }
    }

    fun setFilter(newFilter: CandidateFilter) {
        _filter.value = newFilter
    }

    fun setCycleFilter(cycleNumber: Int?) {
        _selectedCycleNumber.value = cycleNumber
    }

    // -------------------------------------------------------------------------
    // AI VIDEO PARSER INFERENCE
    // -------------------------------------------------------------------------

    fun simulateAiAnalysis() {
        val study = _activeStudy.value ?: return
        _isAnalyzing.value = true

        viewModelScope.launch {
            delay(1500)
            // If candidates are empty, repopulate with fresh AI-suggested candidates
            val existing = repository.getVideoCandidates(study.id)
            if (existing.isEmpty()) {
                val newCandidates = listOf(
                    AICandidateElement(
                        id = UUID.randomUUID().toString(), studyId = study.id, cycleNumber = 1, sequence = 10,
                        name = "Reach & pick bracket", description = "Reaches into parts tray and grasps bracket",
                        startTime = 0.0, endTime = 2.4, activityCategory = VideoActivityCategory.PICK,
                        suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                        wasteCategory = WasteCategory.MOTION, confidenceScore = 0.94, validationStatus = ValidationStatus.AI_SUGGESTED,
                        materialNames = listOf("Bracket")
                    ),
                    AICandidateElement(
                        id = UUID.randomUUID().toString(), studyId = study.id, cycleNumber = 1, sequence = 20,
                        name = "Insert bracket onto pins", description = "Locates bracket on guide pins",
                        startTime = 2.4, endTime = 5.2, activityCategory = VideoActivityCategory.INSERT,
                        suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                        wasteCategory = WasteCategory.NONE, confidenceScore = 0.91, validationStatus = ValidationStatus.AI_SUGGESTED
                    ),
                    AICandidateElement(
                        id = UUID.randomUUID().toString(), studyId = study.id, cycleNumber = 1, sequence = 30,
                        name = "Drive fastener with torque gun", description = "Fastens screw with pneumatic driver",
                        startTime = 5.2, endTime = 12.8, activityCategory = VideoActivityCategory.FASTEN,
                        suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                        wasteCategory = WasteCategory.NONE, confidenceScore = 0.96, validationStatus = ValidationStatus.AI_SUGGESTED,
                        toolNames = listOf("Torque Gun"), materialNames = listOf("M4 Screws x4")
                    ),
                    AICandidateElement(
                        id = UUID.randomUUID().toString(), studyId = study.id, cycleNumber = 1, sequence = 40,
                        name = "Visual gap inspection", description = "Examines seating clearance",
                        startTime = 12.8, endTime = 15.0, activityCategory = VideoActivityCategory.INSPECT,
                        suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                        wasteCategory = WasteCategory.NONE, confidenceScore = 0.85, validationStatus = ValidationStatus.AI_SUGGESTED
                    ),
                    AICandidateElement(
                        id = UUID.randomUUID().toString(), studyId = study.id, cycleNumber = 1, sequence = 50,
                        name = "Unnecessary travel to cart", description = "Steps away to retrieve next batch container",
                        startTime = 15.0, endTime = 21.0, activityCategory = VideoActivityCategory.WALK,
                        suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA,
                        wasteCategory = WasteCategory.MOTION, confidenceScore = 0.92, validationStatus = ValidationStatus.AI_SUGGESTED,
                        notes = "Excessive walking identified."
                    )
                )
                newCandidates.forEach { repository.saveCandidate(it) }
            }
            _candidates.value = repository.getVideoCandidates(study.id).sortedBy { it.startTime }
            _isAnalyzing.value = false
            _statusMessage.value = "AI candidate segmentation completed. Awaiting IE human validation."
        }
    }

    // -------------------------------------------------------------------------
    // HUMAN VALIDATION WORKFLOW
    // -------------------------------------------------------------------------

    fun acceptCandidate(candidateId: String) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        val updated = candidate.copy(validationStatus = ValidationStatus.USER_VALIDATED)
        repository.saveCandidate(updated)
        _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
        _statusMessage.value = "Candidate '${candidate.name}' accepted as USER-VALIDATED."
    }

    fun acceptAllCandidates() {
        val studyId = _selectedStudyId.value
        _candidates.value.forEach { cand ->
            if (cand.validationStatus == ValidationStatus.AI_SUGGESTED) {
                repository.saveCandidate(cand.copy(validationStatus = ValidationStatus.USER_VALIDATED))
            }
        }
        _candidates.value = repository.getVideoCandidates(studyId).sortedBy { it.startTime }
        _statusMessage.value = "All AI-suggested candidates accepted as USER-VALIDATED."
    }

    fun editCandidate(
        candidateId: String,
        newName: String,
        newStart: Double,
        newEnd: Double,
        newCategory: VideoActivityCategory,
        newClassification: ValueClassification,
        toolNames: List<String>,
        materialNames: List<String>,
        notes: String
    ) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        val originalSnapshot = candidate.originalAiSuggestion ?: ImmutableCandidateSnapshot(
            name = candidate.name,
            startTime = candidate.startTime,
            endTime = candidate.endTime,
            duration = candidate.duration,
            activityCategory = candidate.activityCategory,
            classification = candidate.suggestedClassification,
            confidenceScore = candidate.confidenceScore
        )

        val updated = candidate.copy(
            name = newName,
            startTime = newStart,
            endTime = newEnd,
            duration = (newEnd - newStart).coerceAtLeast(0.0),
            activityCategory = newCategory,
            finalClassification = newClassification,
            toolNames = toolNames,
            materialNames = materialNames,
            notes = notes,
            validationStatus = ValidationStatus.USER_EDITED,
            originalAiSuggestion = originalSnapshot
        )
        repository.saveCandidate(updated)
        _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
        _statusMessage.value = "Candidate '${candidate.name}' updated as USER-EDITED."
    }

    fun rejectCandidate(candidateId: String) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        val updated = candidate.copy(validationStatus = ValidationStatus.REJECTED)
        repository.saveCandidate(updated)
        _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
        _statusMessage.value = "Candidate '${candidate.name}' marked as REJECTED."
    }

    fun splitCandidate(candidateId: String, splitTime: Double, part1Name: String? = null, part2Name: String? = null) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        try {
            val (part1, part2) = VideoStudyEngine.splitElement(candidate, splitTime, part1Name, part2Name)
            repository.removeCandidate(candidateId)
            repository.saveCandidate(part1)
            repository.saveCandidate(part2)
            _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
            _statusMessage.value = "Element split at ${String.format(Locale.US, "%.2fs", splitTime)}."
        } catch (e: Exception) {
            _statusMessage.value = "Split failed: ${e.message}"
        }
    }

    fun mergeCandidates(firstId: String, secondId: String, mergedName: String? = null) {
        val first = _candidates.value.find { it.id == firstId } ?: return
        val second = _candidates.value.find { it.id == secondId } ?: return
        val merged = VideoStudyEngine.mergeElements(first, second, mergedName)
        repository.removeCandidate(firstId)
        repository.removeCandidate(secondId)
        repository.saveCandidate(merged)
        _candidates.value = repository.getVideoCandidates(first.studyId).sortedBy { it.startTime }
        _statusMessage.value = "Elements merged into '${merged.name}'."
    }

    fun deleteCandidate(candidateId: String) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        repository.removeCandidate(candidateId)
        _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
        _statusMessage.value = "Candidate removed from study."
    }

    fun createElementFromMarkers(
        name: String,
        activity: VideoActivityCategory,
        classification: ValueClassification,
        tools: List<String> = emptyList(),
        materials: List<String> = emptyList()
    ) {
        val start = _startMarker.value ?: 0.0
        val end = _endMarker.value ?: (_videoTime.value.coerceAtLeast(start + 0.5))
        val study = _activeStudy.value ?: return

        val newElement = AICandidateElement(
            id = UUID.randomUUID().toString(),
            studyId = study.id,
            cycleNumber = _selectedCycleNumber.value ?: 1,
            sequence = ((_candidates.value.maxOfOrNull { it.sequence } ?: 0) + 10),
            name = name.ifBlank { "Manual Element" },
            startTime = start,
            endTime = end,
            duration = end - start,
            activityCategory = activity,
            suggestedClassification = classification,
            finalClassification = classification,
            confidenceScore = 1.0,
            validationStatus = ValidationStatus.USER_VALIDATED,
            toolNames = tools,
            materialNames = materials,
            notes = "Created manually via video markers"
        )
        repository.saveCandidate(newElement)
        _candidates.value = repository.getVideoCandidates(study.id).sortedBy { it.startTime }
        clearMarkers()
        _statusMessage.value = "Created element '$name' (${String.format(Locale.US, "%.2fs", end - start)})."
    }

    fun toggleAbnormalStatus(candidateId: String, reason: String) {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return
        val updated = candidate.copy(
            isAbnormal = !candidate.isAbnormal,
            abnormalEventReason = if (!candidate.isAbnormal) reason else ""
        )
        repository.saveCandidate(updated)
        _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
    }

    fun toggleCycleExclusion(cycleId: String, reason: String = "IE Exclusion") {
        val cycle = _cycles.value.find { it.id == cycleId } ?: return
        val updated = cycle.copy(
            isExcluded = !cycle.isExcluded,
            exclusionReason = if (!cycle.isExcluded) reason else ""
        )
        repository.saveCycle(updated)
        _cycles.value = repository.getStudyCycles(cycle.studyId).sortedBy { it.cycleNumber }
    }

    // -------------------------------------------------------------------------
    // INTEGRATION WITH ELEMENTAL TIME STUDY & WHAT-IF
    // -------------------------------------------------------------------------

    fun commitValidatedElementToMaster(candidateId: String, targetStationId: String? = null): WorkElement? {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return null
        return try {
            val we = repository.commitCandidateToWorkElement(candidate, targetStationId)
            _candidates.value = repository.getVideoCandidates(candidate.studyId).sortedBy { it.startTime }
            _statusMessage.value = "Element '${we.name}' committed to Master Database at Station ${we.stationId}."
            we
        } catch (e: Exception) {
            _statusMessage.value = "Failed to commit element: ${e.message}"
            null
        }
    }

    fun commitAllValidatedElements(): Int {
        val validated = _candidates.value.filter {
            it.validationStatus == ValidationStatus.USER_VALIDATED || it.validationStatus == ValidationStatus.USER_EDITED
        }
        var count = 0
        validated.forEach { cand ->
            try {
                repository.commitCandidateToWorkElement(cand)
                count++
            } catch (_: Exception) {}
        }
        _statusMessage.value = "Committed $count validated work elements to master database."
        return count
    }

    fun sendToWhatIf(candidateId: String, targetStationId: String): Scenario? {
        val candidate = _candidates.value.find { it.id == candidateId } ?: return null
        return try {
            val scn = repository.sendCandidateToWhatIfScenario(candidate, "Relocate ${candidate.name}", targetStationId)
            _statusMessage.value = "Created What-If scenario '${scn.name}' targeting Station $targetStationId."
            scn
        } catch (e: Exception) {
            _statusMessage.value = "What-If transfer error: ${e.message}"
            null
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun createNewStudy(
        name: String,
        fileName: String,
        modelId: String,
        variant: String,
        stationId: String,
        operatorId: String,
        durationSeconds: Double,
        taktSeconds: Double
    ) {
        val newStudy = VideoStudyMetadata(
            id = "VS-${System.currentTimeMillis().toString().takeLast(5)}",
            projectId = "P-001",
            name = name.ifBlank { "New Workstation Study" },
            modelId = modelId,
            variant = variant,
            stationId = stationId,
            operatorId = operatorId,
            videoFileName = fileName.ifBlank { "workstation_capture.mp4" },
            videoDurationSeconds = durationSeconds,
            expectedTaktSeconds = taktSeconds,
            status = VideoStudyStatus.AWAITING_VALIDATION
        )
        repository.saveVideoStudy(newStudy)
        loadProjectStudies("P-001")
        selectStudy(newStudy.id)
        _statusMessage.value = "Created new video study '${newStudy.name}'."
    }
}
