package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectViewModel(private val repository: ManufacturingRepository) : ViewModel() {
    
    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    init {
        viewModelScope.launch {
            // Automatically select first project if available and none selected
            projects.collectLatest { list ->
                if (_currentProject.value == null && list.isNotEmpty()) {
                    _currentProject.value = list.first()
                }
            }
        }
    }

    fun selectProject(project: Project) {
        _currentProject.value = project
    }

    fun selectProjectById(id: String) {
        viewModelScope.launch {
            projects.value.find { it.id == id }?.let {
                _currentProject.value = it
            }
        }
    }

    fun createProject(project: Project) {
        viewModelScope.launch {
            repository.insertProject(project)
            if (_currentProject.value == null) {
                _currentProject.value = project
            }
        }
    }

    fun createPlant(plant: Plant) {
        viewModelScope.launch {
            repository.insertPlant(plant)
        }
    }

    fun createLine(line: Line) {
        viewModelScope.launch {
            repository.insertLine(line)
        }
    }

    fun createStation(station: Station) {
        viewModelScope.launch {
            repository.insertStation(station)
        }
    }

    fun createProcess(process: Process) {
        viewModelScope.launch {
            repository.insertProcess(process)
        }
    }

    val plants: StateFlow<List<Plant>> = repository.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lines: StateFlow<List<Line>> = repository.getAllLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
