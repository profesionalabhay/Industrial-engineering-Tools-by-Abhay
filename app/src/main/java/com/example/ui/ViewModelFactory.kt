package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ManufacturingRepository

class ViewModelFactory(
    private val repository: ManufacturingRepository
) : ViewModelProvider.Factory {
    private val simulationEngine = com.example.logic.SimulationEngine(repository)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(ProjectViewModel::class.java) -> ProjectViewModel(repository) as T
            modelClass.isAssignableFrom(TimeStudyViewModel::class.java) -> TimeStudyViewModel(repository) as T
            modelClass.isAssignableFrom(ManualTimeStudyViewModel::class.java) -> ManualTimeStudyViewModel(repository) as T
            modelClass.isAssignableFrom(AiCopilotViewModel::class.java) -> AiCopilotViewModel(repository) as T
            modelClass.isAssignableFrom(OpExDashboardViewModel::class.java) -> OpExDashboardViewModel(repository) as T
            modelClass.isAssignableFrom(YamazumiViewModel::class.java) -> YamazumiViewModel(repository) as T
            modelClass.isAssignableFrom(WorkBalanceViewModel::class.java) -> WorkBalanceViewModel(repository) as T
            modelClass.isAssignableFrom(OeeViewModel::class.java) -> OeeViewModel(repository) as T
            modelClass.isAssignableFrom(KaizenViewModel::class.java) -> KaizenViewModel(repository) as T
            modelClass.isAssignableFrom(RcaViewModel::class.java) -> RcaViewModel(repository) as T
            modelClass.isAssignableFrom(VideoStudyViewModel::class.java) -> VideoStudyViewModel(repository) as T
            modelClass.isAssignableFrom(StandardWorkViewModel::class.java) -> StandardWorkViewModel(repository) as T
            modelClass.isAssignableFrom(EnterpriseViewModel::class.java) -> EnterpriseViewModel(repository) as T
            modelClass.isAssignableFrom(CapacityViewModel::class.java) -> CapacityViewModel(repository) as T
            modelClass.isAssignableFrom(ErgoViewModel::class.java) -> ErgoViewModel(repository) as T
            modelClass.isAssignableFrom(VsmViewModel::class.java) -> VsmViewModel(repository) as T
            modelClass.isAssignableFrom(SimulationViewModel::class.java) -> SimulationViewModel(repository, simulationEngine) as T
            modelClass.isAssignableFrom(SpaghettiViewModel::class.java) -> SpaghettiViewModel(repository) as T
            modelClass.isAssignableFrom(MotionViewModel::class.java) -> MotionViewModel(repository) as T
            modelClass.isAssignableFrom(MultiModelViewModel::class.java) -> MultiModelViewModel(repository) as T
            modelClass.isAssignableFrom(WhatIfViewModel::class.java) -> WhatIfViewModel(repository) as T
            modelClass.isAssignableFrom(SavingsViewModel::class.java) -> SavingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
