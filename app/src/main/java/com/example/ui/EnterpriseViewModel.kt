package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EnterpriseUiState(
    val kpis: List<EnterpriseKpi> = emptyList(),
    val benchmarkMetric: BenchmarkMetric = BenchmarkMetric.OEE,
    val isLoading: Boolean = false,
    val productivityRecords: List<ProductivityRecord> = emptyList()
)

class EnterpriseViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(EnterpriseUiState())
    val uiState: StateFlow<EnterpriseUiState> = _uiState.asStateFlow()

    private var dataJob: kotlinx.coroutines.Job? = null

    fun initialize() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                repository.getEnterpriseKpis(),
                repository.getProductivityRecords()
            ) { kpis, prod ->
                _uiState.update { it.copy(
                    kpis = kpis,
                    productivityRecords = prod,
                    isLoading = false
                ) }
            }.collect()
        }
    }

    fun setBenchmarkMetric(metric: BenchmarkMetric) {
        _uiState.update { it.copy(benchmarkMetric = metric) }
    }
}
