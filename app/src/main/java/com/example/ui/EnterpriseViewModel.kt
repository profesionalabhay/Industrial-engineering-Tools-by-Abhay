package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun loadEnterpriseData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val kpis = repository.enterpriseKpis
            val prod = repository.productivityRecords
            _uiState.value = _uiState.value.copy(
                kpis = kpis,
                productivityRecords = prod,
                isLoading = false
            )
        }
    }

    fun setBenchmarkMetric(metric: BenchmarkMetric) {
        _uiState.value = _uiState.value.copy(benchmarkMetric = metric)
    }
}
