package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StationYamazumiMetrics(
    val station: Station,
    val elements: List<WorkElement>,
    val totalTime: Double,
    val vaTime: Double,
    val nnvaTime: Double,
    val nvaTime: Double,
    val vaPercent: Double,
    val nvaPercent: Double,
    val utilization: Double,
    val idleTime: Double
)

class YamazumiViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()

    private val _metrics = MutableStateFlow<List<StationYamazumiMetrics>>(emptyList())
    val metrics: StateFlow<List<StationYamazumiMetrics>> = _metrics.asStateFlow()

    private val _taktTime = MutableStateFlow<Double?>(null)
    val taktTime: StateFlow<Double?> = _taktTime.asStateFlow()

    private val _selectedStation = MutableStateFlow<StationYamazumiMetrics?>(null)
    val selectedStation: StateFlow<StationYamazumiMetrics?> = _selectedStation.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val line = repository.lines.firstOrNull()
        val takt = line?.taktTime
        _taktTime.value = takt

        val elementsByStation = repository.workElements.groupBy { it.stationId }
        val allStations = repository.stations

        val newMetrics = allStations.map { station ->
            val elements = elementsByStation[station.id] ?: emptyList()
            val vaTime = elements.filter { it.valueClassification == ValueClassification.VA }.sumOf { it.standardTime }
            val nnvaTime = elements.filter { it.valueClassification == ValueClassification.NNVA }.sumOf { it.standardTime }
            val nvaTime = elements.filter { it.valueClassification == ValueClassification.NVA }.sumOf { it.standardTime }
            val totalTime = vaTime + nnvaTime + nvaTime
            
            val vaPercent = if (totalTime > 0) (vaTime / totalTime) * 100 else 0.0
            val nvaPercent = if (totalTime > 0) ((nnvaTime + nvaTime) / totalTime) * 100 else 0.0
            
            val idleTime = if (takt != null) maxOf(0.0, takt - totalTime) else 0.0
            val utilization = if (takt != null && takt > 0) (totalTime / takt) * 100 else 0.0

            StationYamazumiMetrics(
                station = station,
                elements = elements.sortedBy { it.sequence },
                totalTime = totalTime,
                vaTime = vaTime,
                nnvaTime = nnvaTime,
                nvaTime = nvaTime,
                vaPercent = vaPercent,
                nvaPercent = nvaPercent,
                utilization = utilization,
                idleTime = idleTime
            )
        }
        _metrics.value = newMetrics

        // Keep selection updated if it exists
        _selectedStation.value?.let { currentSelection ->
            _selectedStation.value = newMetrics.find { it.station.id == currentSelection.station.id }
        }
    }

    fun selectStation(stationId: String?) {
        if (stationId == null) {
            _selectedStation.value = null
        } else {
            _selectedStation.value = _metrics.value.find { it.station.id == stationId }
        }
    }

    fun updateElementClassification(elementId: String, classification: ValueClassification, wasteCategory: WasteCategory, reason: String) {
        val index = repository.workElements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = repository.workElements[index]
            repository.workElements[index] = el.copy(
                valueClassification = classification,
                wasteCategory = wasteCategory,
                classificationReason = reason,
                ieOverridden = true
            )
            loadData() // Recalculate metrics
        }
    }
}
