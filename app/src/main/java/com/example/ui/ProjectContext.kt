package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.ManufacturingRepository
import com.example.data.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProjectViewModel : ViewModel() {
    private val repository = ManufacturingRepository.getInstance()
    
    private val _projects = MutableStateFlow(repository.projects)
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow(repository.projects.first())
    val currentProject: StateFlow<Project> = _currentProject.asStateFlow()

    fun selectProject(project: Project) {
        _currentProject.value = project
    }
}
