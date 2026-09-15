package com.example.data

class ManufacturingRepository private constructor() {
    val plants = mutableListOf<Plant>()
    val lines = mutableListOf<Line>()
    val projects = mutableListOf<Project>()
    val models = mutableListOf<Model>()
    val processes = mutableListOf<Process>()
    val stations = mutableListOf<Station>()
    val operators = mutableListOf<Operator>()
    val workElements = mutableListOf<WorkElement>()
    val observations = mutableListOf<Observation>()
    val cycles = mutableListOf<Cycle>()
    val tools = mutableListOf<Tool>()
    val materials = mutableListOf<Material>()
    val dependencies = mutableListOf<Dependency>()
    val classifications = mutableListOf<Classification>()
    val scenarios = mutableListOf<Scenario>()
    val scenarioElements = mutableListOf<ScenarioElement>()
    val vsmProcesses = mutableListOf<VSMProcess>()
    val layoutObjects = mutableListOf<LayoutObject>()
    val motionEvents = mutableListOf<MotionEvent>()
    val capacityPlans = mutableListOf<CapacityPlan>()
    val manpowerPlans = mutableListOf<ManpowerPlan>()
    val kaizenActions = mutableListOf<KaizenAction>()
    val savingsRecords = mutableListOf<SavingsRecord>()
    val standardWorks = mutableListOf<StandardWork>()
    val ergonomicAssessments = mutableListOf<ErgonomicAssessment>()

    init {
        seedData()
    }

    fun addWorkElement(element: WorkElement) {
        val station = stations.find { it.id == element.stationId }
            ?: throw IllegalArgumentException("Station not found: ${element.stationId}")
        require(station.processId == element.processId) { "Station must belong to current process." }

        val model = models.find { it.id == element.modelId }
            ?: throw IllegalArgumentException("Model not found: ${element.modelId}")
        require(model.projectId == element.projectId) { "Model must belong to project." }

        element.predecessorIds.forEach { predId ->
            require(workElements.any { it.id == predId }) { "Dependency reference must exist: $predId" }
        }
        element.successorIds.forEach { succId ->
            require(workElements.any { it.id == succId }) { "Dependency reference must exist: $succId" }
        }

        workElements.add(element)
    }

    private fun seedData() {
        plants.add(Plant("PLT-1", "Main Assembly Plant", "Detroit, MI"))
        lines.add(Line("LN-1", "PLT-1", "Assembly Line 1"))
        
        projects.add(Project("P-001", "LN-1", "Line 1 Assembly Optimization", "Improving cycle time for main assembly line.", "Active"))
        projects.add(Project("P-002", "LN-1", "Warehouse Layout Redesign", "Redesigning warehouse for better flow.", "Planning"))
        projects.add(Project("P-003", "LN-2", "Ergonomics Study - St 04", "Evaluating operator ergonomics at Station 04.", "Active"))
        projects.add(Project("P-004", "LN-3", "New Product Introduction", "Setup and balancing for the new Delta model.", "On Hold"))
        
        models.add(Model("MDL-1", "P-001", "Delta Widget V1", 500))
        
        processes.add(Process("PRC-1", "LN-1", "Final Assembly"))
        
        stations.add(Station("ST-04", "PRC-1", "Station 04 - Housing"))
        stations.add(Station("ST-05", "PRC-1", "Station 05 - Motor Mount"))
        
        operators.add(Operator("OP-1", "John D.", "Expert"))
        operators.add(Operator("OP-2", "Jane S.", "Intermediate"))
        
        tools.add(Tool("TL-1", "Torque Wrench"))
        materials.add(Material("MAT-1", "M4 Screws x4", 0.05))
        
        capacityPlans.add(CapacityPlan("CP-1", "P-001", 500, 28800.0, 57.6))
        
        val we1 = WorkElement(
            id = "WE-001", projectId = "P-001", modelId = "MDL-1", processId = "PRC-1", stationId = "ST-04", operatorId = "OP-1",
            sequence = 10, name = "Retrieve housing", description = "Get main housing from bin",
            startTime = 0.0, endTime = 2.5, observedTime = 2.5, normalTime = 2.5, standardTime = 2.75,
            performanceRating = 1.0, allowance = 0.1, valueClassification = ValueClassification.NNVA,
            wasteCategory = WasteCategory.MOTION, toolIds = emptyList(), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = true, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.95, evidenceReference = "Vid_01_00:00", notes = ""
        )
        addWorkElement(we1)
        
        val we2 = WorkElement(
            id = "WE-002", projectId = "P-001", modelId = "MDL-1", processId = "PRC-1", stationId = "ST-04", operatorId = "OP-1",
            sequence = 20, name = "Fasten screws", description = "Secure housing with 4 screws",
            startTime = 2.5, endTime = 10.0, observedTime = 7.5, normalTime = 7.1, standardTime = 7.8,
            performanceRating = 0.95, allowance = 0.1, valueClassification = ValueClassification.VA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-1"), materialIds = listOf("MAT-1"),
            predecessorIds = listOf("WE-001"), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = true, confidence = 0.9, evidenceReference = "Vid_01_00:02", notes = "Potential for auto-feeder"
        )
        addWorkElement(we2)
        
        scenarios.add(Scenario("SCN-1", "P-001", "Auto-feeder Implementation", "Replace manual fastening", System.currentTimeMillis()))
        scenarioElements.add(ScenarioElement("SE-1", "SCN-1", "WE-002", deltaObservedTime = 3.0, deltaStationId = null, deltaOperatorId = null, deltaSequence = null, deltaValueClassification = null))
        
        // Generate some observations
        observations.add(Observation("OBS-1", "WE-002", "CYC-1", 7.6, "Normal pace"))
        observations.add(Observation("OBS-2", "WE-002", "CYC-2", 7.4, "Normal pace"))
        observations.add(Observation("OBS-3", "WE-002", "CYC-3", 8.1, "Fumbled screw"))
    }

    companion object {
        @Volatile
        private var instance: ManufacturingRepository? = null

        fun getInstance(): ManufacturingRepository {
            return instance ?: synchronized(this) {
                instance ?: ManufacturingRepository().also { instance = it }
            }
        }
    }
}
