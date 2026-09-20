package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

data class ElementStats(
    val element: WorkElement,
    val observations: List<Observation>,
    val activeObservations: List<Observation>
) {
    val average: Double = if (activeObservations.isNotEmpty()) activeObservations.map { it.observedTime }.average() else 0.0
    val median: Double = if (activeObservations.isNotEmpty()) {
        val sorted = activeObservations.map { it.observedTime }.sorted()
        if (sorted.size % 2 == 0) (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0 else sorted[sorted.size / 2]
    } else 0.0
    val min: Double = activeObservations.minOfOrNull { it.observedTime } ?: 0.0
    val max: Double = activeObservations.maxOfOrNull { it.observedTime } ?: 0.0
    val stdDev: Double = if (activeObservations.size > 1) {
        val mean = average
        sqrt(activeObservations.sumOf { (it.observedTime - mean).pow(2) } / (activeObservations.size - 1))
    } else 0.0
    val cv: Double = if (average > 0) (stdDev / average) * 100.0 else 0.0
}

class TimeStudyViewModel(private val repository: ManufacturingRepository) : ViewModel() {

    private val _projectId = MutableStateFlow<String?>(null)
    
    val workElements: StateFlow<List<WorkElement>> = _projectId
        .flatMapLatest { id -> 
            if (id != null) repository.getWorkElementsForProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedElement = MutableStateFlow<WorkElement?>(null)
    val selectedElement: StateFlow<WorkElement?> = _selectedElement.asStateFlow()

    val observations: StateFlow<List<Observation>> = _selectedElement
        .flatMapLatest { el ->
            if (el != null) repository.getObservationsForElement(el.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(id: String) {
        _projectId.value = id
    }

    fun selectElement(element: WorkElement) {
        _selectedElement.value = element
    }

    fun getStatsForElement(element: WorkElement, obs: List<Observation>): ElementStats {
        return ElementStats(element, obs, obs.filter { !it.isRejected })
    }

    fun toggleObservationRejection(observation: Observation) {
        viewModelScope.launch {
            repository.insertObservation(observation.copy(isRejected = !observation.isRejected))
        }
    }

    fun updatePerformanceRating(element: WorkElement, rating: Double, obs: List<Observation>) {
        viewModelScope.launch {
            val activeObs = obs.filter { !it.isRejected }
            val avg = if (activeObs.isNotEmpty()) activeObs.map { it.observedTime }.average() else element.observedTime
            val normalTime = avg * rating
            val standardTime = normalTime * (1 + element.allowance)
            
            val updated = element.copy(
                performanceRating = rating,
                observedTime = avg,
                normalTime = normalTime,
                standardTime = standardTime
            )
            repository.insertWorkElement(updated)
            _selectedElement.value = updated
        }
    }

    fun updateAllowance(element: WorkElement, allowance: Double) {
        viewModelScope.launch {
            val standardTime = element.normalTime * (1 + allowance)
            val updated = element.copy(
                allowance = allowance,
                standardTime = standardTime
            )
            repository.insertWorkElement(updated)
            _selectedElement.value = updated
        }
    }
}
