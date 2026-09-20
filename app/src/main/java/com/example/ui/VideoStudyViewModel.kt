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

class VideoStudyViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _projectId = MutableStateFlow<String?>(null)

    private val _videoUri = MutableStateFlow<String?>("workstation_st04_cycle_study.mp4")
    val videoUri: StateFlow<String?> = _videoUri.asStateFlow()

    private val _selectedStudyId = MutableStateFlow<String?>(null)
    val selectedStudyId: StateFlow<String?> = _selectedStudyId.asStateFlow()

    val studies: StateFlow<List<VideoStudyMetadata>> = _projectId.filterNotNull().flatMapLatest { pid ->
        repository.getVideoStudies(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStudy: StateFlow<VideoStudyMetadata?> = _selectedStudyId.filterNotNull().flatMapLatest { sid ->
        flow { emit(repository.getVideoStudyById(sid)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val candidates: StateFlow<List<AICandidateElement>> = _selectedStudyId.filterNotNull().flatMapLatest { sid ->
        repository.getVideoCandidates(sid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycles: StateFlow<List<StudyCycle>> = _selectedStudyId.filterNotNull().flatMapLatest { sid ->
        repository.getStudyCycles(sid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun initialize(projectId: String) {
        _projectId.value = projectId
        viewModelScope.launch {
            studies.collectLatest { sList ->
                if (_selectedStudyId.value == null && sList.isNotEmpty()) {
                    selectStudy(sList.first().id)
                }
            }
        }
    }

    fun selectStudy(studyId: String) {
        _selectedStudyId.value = studyId
        viewModelScope.launch {
            val study = repository.getVideoStudyById(studyId)
            study?.let {
                _duration.value = it.videoDurationSeconds
                _videoUri.value = it.videoFileName
            }
        }
    }

    val filteredCandidates: StateFlow<List<AICandidateElement>> = combine(
        candidates,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycleStats: StateFlow<CycleStatistics> = combine(cycles, candidates) { cycleList, candidateList ->
        VideoStudyEngine.calculateCycleStatistics(cycleList, candidateList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CycleStatistics(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, null, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

    val opportunities: StateFlow<List<VideoImprovementOpportunity>> = combine(candidates, cycles, cycleStats) { candList, cycleList, stats ->
        val studyId = _selectedStudyId.value ?: ""
        VideoStudyEngine.scanImprovementOpportunities(studyId, candList, cycleList, stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiReport: StateFlow<VideoStudyAiReport?> = combine(activeStudy, candidates, cycles, cycleStats, opportunities) { study, candList, cycleList, stats, opps ->
        study?.let {
            VideoStudyEngine.generateAiReport(it, candList, cycleList, stats, opps)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun togglePlay() { _isPlaying.value = !_isPlaying.value }
    fun stepForward() { _videoTime.value = minOf(_duration.value, _videoTime.value + (1.0 / 30.0)) }
    fun stepBackward() { _videoTime.value = maxOf(0.0, _videoTime.value - (1.0 / 30.0)) }
    fun seek(time: Double) { _videoTime.value = time.coerceIn(0.0, _duration.value) }
    fun setSpeed(speed: Double) { _playbackSpeed.value = speed }

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
        val cand = candidates.value.find { it.id == candidateId }
        if (cand != null) {
            seek(cand.startTime)
        }
    }

    fun setFilter(newFilter: CandidateFilter) { _filter.value = newFilter }
    fun setCycleFilter(cycleNumber: Int?) { _selectedCycleNumber.value = cycleNumber }

    fun simulateAiAnalysis() {
        val studyId = _selectedStudyId.value ?: return
        _isAnalyzing.value = true
        viewModelScope.launch {
            delay(1500)
            // Mock AI logic...
            _isAnalyzing.value = false
            _statusMessage.value = "AI analysis completed."
        }
    }

    fun acceptCandidate(candidateId: String) {
        val candidate = candidates.value.find { it.id == candidateId } ?: return
        viewModelScope.launch {
            repository.insertVideoCandidate(candidate.copy(validationStatus = ValidationStatus.USER_VALIDATED))
        }
    }

    fun editCandidate(candidate: AICandidateElement) {
        viewModelScope.launch {
            repository.insertVideoCandidate(candidate.copy(validationStatus = ValidationStatus.USER_EDITED))
        }
    }

    fun rejectCandidate(candidateId: String) {
        val candidate = candidates.value.find { it.id == candidateId } ?: return
        viewModelScope.launch {
            repository.insertVideoCandidate(candidate.copy(validationStatus = ValidationStatus.REJECTED))
        }
    }

    fun deleteCandidate(candidateId: String) {
        viewModelScope.launch {
            repository.deleteVideoCandidate(candidateId)
        }
    }

    fun toggleCycleExclusion(cycle: StudyCycle) {
        viewModelScope.launch {
            repository.insertStudyCycle(cycle.copy(isExcluded = !cycle.isExcluded))
        }
    }

    fun commitValidatedElementToMaster(candidateId: String, targetStationId: String? = null) {
        val candidate = candidates.value.find { it.id == candidateId } ?: return
        viewModelScope.launch {
            repository.commitCandidateToWorkElement(candidate, targetStationId)
            _statusMessage.value = "Committed to Master Database."
        }
    }

    fun createNewStudy(name: String, fileName: String, modelId: String, stationId: String, duration: Double) {
        val newStudy = VideoStudyMetadata(
            id = "VS-${System.currentTimeMillis()}",
            projectId = _projectId.value ?: "P-001",
            name = name,
            videoFileName = fileName,
            modelId = modelId,
            stationId = stationId,
            videoDurationSeconds = duration,
            status = VideoStudyStatus.AWAITING_VALIDATION
        )
        viewModelScope.launch {
            repository.insertVideoStudy(newStudy)
            selectStudy(newStudy.id)
        }
    }
}
