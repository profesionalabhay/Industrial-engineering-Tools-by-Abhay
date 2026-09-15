package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class TimeStudyViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _workElements = MutableStateFlow<List<WorkElement>>(emptyList())
    val workElements: StateFlow<List<WorkElement>> = _workElements.asStateFlow()

    private val _observations = MutableStateFlow<List<Observation>>(emptyList())
    val observations: StateFlow<List<Observation>> = _observations.asStateFlow()

    private val _selectedElement = MutableStateFlow<WorkElement?>(null)
    val selectedElement: StateFlow<WorkElement?> = _selectedElement.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _workElements.value = repository.workElements.toList()
        _observations.value = repository.observations.toList()
        if (_selectedElement.value == null && _workElements.value.isNotEmpty()) {
            _selectedElement.value = _workElements.value.first()
        }
    }

    fun selectElement(element: WorkElement) {
        _selectedElement.value = element
    }

    fun getStatsForElement(elementId: String): ElementStats? {
        val element = _workElements.value.find { it.id == elementId } ?: return null
        val obs = _observations.value.filter { it.workElementId == elementId }
        return ElementStats(element, obs, obs.filter { !it.isRejected })
    }

    fun toggleObservationRejection(observationId: String) {
        val index = repository.observations.indexOfFirst { it.id == observationId }
        if (index != -1) {
            val obs = repository.observations[index]
            repository.observations[index] = obs.copy(isRejected = !obs.isRejected)
            loadData()
        }
    }

    fun updatePerformanceRating(elementId: String, rating: Double) {
        val index = repository.workElements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = repository.workElements[index]
            val obs = repository.observations.filter { it.workElementId == elementId && !it.isRejected }
            val avg = if (obs.isNotEmpty()) obs.map { it.observedTime }.average() else el.observedTime
            val normalTime = avg * rating
            val standardTime = normalTime * (1 + el.allowance)
            
            repository.workElements[index] = el.copy(
                performanceRating = rating,
                observedTime = avg,
                normalTime = normalTime,
                standardTime = standardTime
            )
            loadData()
            _selectedElement.value = repository.workElements[index]
        }
    }

    fun updateAllowance(elementId: String, allowance: Double) {
        val index = repository.workElements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = repository.workElements[index]
            val standardTime = el.normalTime * (1 + allowance)
            repository.workElements[index] = el.copy(
                allowance = allowance,
                standardTime = standardTime
            )
            loadData()
            _selectedElement.value = repository.workElements[index]
        }
    }
}
