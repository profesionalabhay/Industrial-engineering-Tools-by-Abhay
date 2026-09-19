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
    val ergoAssessments = mutableListOf<ErgoAssessment>()

    // V2.4 Operational Excellence Collections
    val oeeRecords = mutableListOf<OeeRecord>()
    val lossEvents = mutableListOf<LossEvent>()
    val rcaRecords = mutableListOf<RcaRecord>()
    val kaizenRecords = mutableListOf<KaizenRecord>()
    val standardWorkRevisions = mutableListOf<StandardWorkRevision>()
    val vsmMaps = mutableListOf<VsmMap>()
    val materialFlows = mutableListOf<MaterialFlow>()
    val improvementBenefits = mutableListOf<ImprovementBenefit>()
    
    // V2.1 Multi-Model Collections
    val productionPlans = mutableListOf<ProductionPlan>()
    val modelMixItems = mutableListOf<ModelMixItem>()
    val productionSequences = mutableListOf<ProductionSequence>()

    // V2.2 Advanced AI Video Time Study Collections
    val videoStudies = mutableListOf<VideoStudyMetadata>()
    val videoCandidates = mutableListOf<AICandidateElement>()
    val studyCycles = mutableListOf<StudyCycle>()

    // V2.5 — ENTERPRISE IE & DIGITAL SIMULATION COLLECTIONS
    val timeStudyTemplates = mutableListOf<TimeStudyTemplate>()
    val simulationResults = mutableListOf<SimulationResult>()
    val enterpriseKpis = mutableListOf<EnterpriseKpi>()
    val productivityRecords = mutableListOf<ProductivityRecord>()
    val savingsValidations = mutableListOf<SavingsValidation>()

    // V2.3 AI IE COPILOT Collections
    private val aiProviders = mutableListOf<AIProviderConfig>()
    private val aiModels = mutableListOf<AIModelConfig>()
    private val aiChatMessages = mutableListOf<AiChatMessage>()

    init {
        seedData()
        seedAiData()
        seedOpExData()
        seedEnterpriseData()
    }

    // ==========================================
    // V2.5 — ENTERPRISE IE & SIMULATION METHODS
    // ==========================================

    fun getSimulationResults(projectId: String) = simulationResults.filter { it.projectId == projectId }
    fun getProductivityRecords(projectId: String) = productivityRecords.filter { it.projectId == projectId }
    
    fun addManualObservation(observation: Observation) {
        observations.add(observation)
    }

    fun addManualCycle(cycle: StudyCycle) {
        studyCycles.add(cycle)
    }

    fun addSimulationResult(result: SimulationResult) {
        simulationResults.add(result)
    }

    private fun seedEnterpriseData() {
        // 1. Time Study Templates
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-1", "Pick", VideoActivityCategory.PICK, ValueClassification.VA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-2", "Reach", VideoActivityCategory.REACH, ValueClassification.NVA, WasteCategory.MOTION))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-3", "Move", VideoActivityCategory.MOVE, ValueClassification.VA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-4", "Position", VideoActivityCategory.POSITION, ValueClassification.VA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-5", "Assemble", VideoActivityCategory.ASSEMBLE, ValueClassification.VA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-6", "Fasten", VideoActivityCategory.FASTEN, ValueClassification.VA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-7", "Inspect", VideoActivityCategory.INSPECT, ValueClassification.NNVA))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-8", "Walk", VideoActivityCategory.WALK, ValueClassification.NVA, WasteCategory.MOTION))
        timeStudyTemplates.add(TimeStudyTemplate("TEMP-9", "Wait", VideoActivityCategory.WAIT, ValueClassification.NVA, WasteCategory.WAITING))

        // 2. Enterprise KPIs
        enterpriseKpis.add(EnterpriseKpi("EKPI-1", "PLT-1", "PLANT", BenchmarkMetric.OEE, 0.78, 0.85, "2024-Q3", 0.02))
        enterpriseKpis.add(EnterpriseKpi("EKPI-2", "LN-1", "LINE", BenchmarkMetric.BALANCE_EFFICIENCY, 0.84, 0.90, "2024-Q3", -0.01))
        
        // 3. Productivity Records
        productivityRecords.add(ProductivityRecord("PROD-1", "P-001", "2024-05-20", "Day", 450, 32.0, 14.06, 256.0))
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
        
        models.add(Model("MDL-1", "P-001", "Delta Standard", 500, variant = "Base", family = "Delta Series"))
        models.add(Model("MDL-2", "P-001", "Delta Sport", 300, variant = "Performance", family = "Delta Series"))
        models.add(Model("MDL-3", "P-001", "Delta Pro", 200, variant = "Luxury", family = "Delta Series"))
        
        processes.add(Process("PRC-1", "LN-1", "Final Assembly"))
        
        stations.add(Station("ST-04", "PRC-1", "Station 04 - Housing"))
        stations.add(Station("ST-05", "PRC-1", "Station 05 - Motor Mount"))
        
        operators.add(Operator("OP-1", "John D.", "Expert"))
        operators.add(Operator("OP-2", "Jane S.", "Intermediate"))
        
        tools.add(Tool("TL-1", "Torque Wrench"))
        tools.add(Tool("TL-2", "Digital Multimeter"))
        materials.add(Material("MAT-1", "M4 Screws x4", 0.05))
        materials.add(Material("MAT-2", "Reinforcement Bracket", 1.20))
        
        capacityPlans.add(CapacityPlan("CP-1", "P-001", 1000, 28800.0, 27.0))
        
        // Multi-Model Production Plan
        val plan1 = ProductionPlan(
            id = "PLAN-001",
            projectId = "P-001",
            name = "Q3 Mixed Production Plan",
            period = "2026-Q3",
            shiftName = "Day Shift (8h)",
            availableOperatingSeconds = 28800.0,
            plannedOperatingSeconds = 27000.0,
            plannedTotalQuantity = 1000,
            requiredTaktSeconds = 27.0,
            operatorCount = 4,
            workingCalendar = "Standard 5-Day (2 Shifts)"
        )
        productionPlans.add(plan1)

        // Model Mix Items
        modelMixItems.add(
            ModelMixItem(
                id = "MIX-001",
                planId = "PLAN-001",
                modelId = "MDL-1",
                modelName = "Delta Standard",
                variant = "Base",
                plannedQuantity = 500,
                calculatedMixPercentage = 50.0,
                manualMixOverride = null,
                requiredTakt = 54.0,
                standardWorkContent = 29.05,
                isActive = true
            )
        )
        modelMixItems.add(
            ModelMixItem(
                id = "MIX-002",
                planId = "PLAN-001",
                modelId = "MDL-2",
                modelName = "Delta Sport",
                variant = "Performance",
                plannedQuantity = 300,
                calculatedMixPercentage = 30.0,
                manualMixOverride = null,
                requiredTakt = 90.0,
                standardWorkContent = 41.75,
                isActive = true
            )
        )
        modelMixItems.add(
            ModelMixItem(
                id = "MIX-003",
                planId = "PLAN-001",
                modelId = "MDL-3",
                modelName = "Delta Pro",
                variant = "Luxury",
                plannedQuantity = 200,
                calculatedMixPercentage = 20.0,
                manualMixOverride = null,
                requiredTakt = 135.0,
                standardWorkContent = 51.45,
                isActive = true
            )
        )

        // Production Sequence
        productionSequences.add(
            ProductionSequence(
                id = "SEQ-001",
                planId = "PLAN-001",
                name = "Heijunka Leveled Sequence",
                modelPattern = listOf("MDL-1", "MDL-2", "MDL-1", "MDL-3", "MDL-1"),
                batchSizes = mapOf("MDL-1" to 1, "MDL-2" to 1, "MDL-3" to 1),
                isRepeating = true,
                pitchMinutes = 2.25
            )
        )
        
        val we1 = WorkElement(
            id = "WE-001", projectId = "P-001", modelId = "MDL-1", processId = "PRC-1", stationId = "ST-04", operatorId = "OP-1",
            sequence = 10, name = "Retrieve housing", description = "Get main housing from bin",
            startTime = 0.0, endTime = 2.5, observedTime = 2.5, normalTime = 2.5, standardTime = 2.75,
            performanceRating = 1.0, allowance = 0.1, valueClassification = ValueClassification.NNVA,
            wasteCategory = WasteCategory.MOTION, toolIds = emptyList(), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = true, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.95, evidenceReference = "Vid_01_00:00", notes = "",
            applicability = ElementApplicability.COMMON
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
            automationOpportunity = true, confidence = 0.9, evidenceReference = "Vid_01_00:02", notes = "Potential for auto-feeder",
            applicability = ElementApplicability.COMMON,
            modelStandardTimes = mapOf("MDL-1" to 7.8, "MDL-2" to 9.5, "MDL-3" to 11.2)
        )
        addWorkElement(we2)

        val we3 = WorkElement(
            id = "WE-003", projectId = "P-001", modelId = "MDL-2", processId = "PRC-1", stationId = "ST-04", operatorId = "OP-1",
            sequence = 30, name = "Mount sport bracket", description = "Install reinforcement bracket for high-spec variants",
            startTime = 10.0, endTime = 16.0, observedTime = 6.0, normalTime = 5.9, standardTime = 6.5,
            performanceRating = 0.98, allowance = 0.1, valueClassification = ValueClassification.VA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-1"), materialIds = listOf("MAT-2"),
            predecessorIds = listOf("WE-002"), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.92, evidenceReference = "Vid_01_00:10", notes = "Sport & Pro exclusive",
            applicability = ElementApplicability.MODEL_SPECIFIC,
            applicableModelIds = listOf("MDL-2", "MDL-3")
        )
        addWorkElement(we3)

        val we4 = WorkElement(
            id = "WE-004", projectId = "P-001", modelId = "MDL-1", processId = "PRC-1", stationId = "ST-05", operatorId = "OP-2",
            sequence = 10, name = "Install base motor", description = "Mount standard single-rotor motor",
            startTime = 0.0, endTime = 13.0, observedTime = 13.0, normalTime = 12.7, standardTime = 14.0,
            performanceRating = 0.98, allowance = 0.1, valueClassification = ValueClassification.VA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-1"), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.94, evidenceReference = "Vid_01_00:14", notes = "Base model only",
            applicability = ElementApplicability.MODEL_SPECIFIC,
            applicableModelIds = listOf("MDL-1")
        )
        addWorkElement(we4)

        val we5 = WorkElement(
            id = "WE-005", projectId = "P-001", modelId = "MDL-2", processId = "PRC-1", stationId = "ST-05", operatorId = "OP-2",
            sequence = 20, name = "Install brushless motor", description = "Mount performance brushless dual-rotor motor",
            startTime = 0.0, endTime = 17.0, observedTime = 17.0, normalTime = 16.8, standardTime = 18.5,
            performanceRating = 0.99, allowance = 0.1, valueClassification = ValueClassification.VA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-1"), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.91, evidenceReference = "Vid_01_00:20", notes = "Sport & Pro",
            applicability = ElementApplicability.MODEL_SPECIFIC,
            applicableModelIds = listOf("MDL-2", "MDL-3")
        )
        addWorkElement(we5)

        val we6 = WorkElement(
            id = "WE-006", projectId = "P-001", modelId = "MDL-3", processId = "PRC-1", stationId = "ST-05", operatorId = "OP-2",
            sequence = 30, name = "Connect Pro telemetry", description = "Route digital sensor harness and connect telemetry bus",
            startTime = 17.0, endTime = 25.0, observedTime = 8.0, normalTime = 7.7, standardTime = 8.5,
            performanceRating = 0.96, allowance = 0.1, valueClassification = ValueClassification.VA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-2"), materialIds = emptyList(),
            predecessorIds = listOf("WE-005"), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.95, evidenceReference = "Vid_01_00:28", notes = "Pro exclusive",
            applicability = ElementApplicability.MODEL_SPECIFIC,
            applicableModelIds = listOf("MDL-3")
        )
        addWorkElement(we6)

        val we7 = WorkElement(
            id = "WE-007", projectId = "P-001", modelId = "MDL-1", processId = "PRC-1", stationId = "ST-05", operatorId = "OP-2",
            sequence = 40, name = "Final torque & inspection", description = "Confirm torque spec and verify visual continuity",
            startTime = 25.0, endTime = 29.0, observedTime = 4.0, normalTime = 4.0, standardTime = 4.5,
            performanceRating = 1.0, allowance = 0.125, valueClassification = ValueClassification.NNVA,
            wasteCategory = WasteCategory.NONE, toolIds = listOf("TL-1"), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = false, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.97, evidenceReference = "Vid_01_00:34", notes = "Universal inspection",
            applicability = ElementApplicability.COMMON
        )
        addWorkElement(we7)
        
        scenarios.add(Scenario("SCN-1", "P-001", "Auto-feeder Implementation", "Replace manual fastening", System.currentTimeMillis()))
        scenarioElements.add(ScenarioElement("SE-1", "SCN-1", "WE-002", deltaObservedTime = 3.0, deltaStationId = null, deltaOperatorId = null, deltaSequence = null, deltaValueClassification = null))
        
        // Generate some observations
        observations.add(Observation("OBS-1", "WE-002", "CYC-1", 7.6, "Normal pace"))
        observations.add(Observation("OBS-2", "WE-002", "CYC-2", 7.4, "Normal pace"))
        observations.add(Observation("OBS-3", "WE-002", "CYC-3", 8.1, "Fumbled screw"))

        // V2.2 Seed Video Study
        val vs1 = VideoStudyMetadata(
            id = "VS-001",
            projectId = "P-001",
            name = "Housing Assembly & Fastening Study",
            modelId = "MDL-1",
            variant = "Base",
            lineId = "L-01",
            processId = "PRC-1",
            stationId = "ST-04",
            operatorId = "OP-1",
            videoFileName = "workstation_st04_cycle_study.mp4",
            videoDurationSeconds = 64.5,
            shift = "Day Shift (08:00 - 16:30)",
            date = "2026-09-18",
            productionCondition = "Nominal Run Rate (3 Operators active)",
            expectedTaktSeconds = 27.0,
            availableManpower = 1,
            targetOutputUnits = 450,
            notes = "Workstation 4 multi-cycle study to evaluate fastening cycle variation and walking waste.",
            status = VideoStudyStatus.AWAITING_VALIDATION
        )
        videoStudies.add(vs1)

        // Cycles for VS-001
        studyCycles.add(StudyCycle("CYC-01", "VS-001", 1, 0.0, 21.0, 21.0, isNormal = true, isExcluded = false))
        studyCycles.add(StudyCycle("CYC-02", "VS-001", 2, 21.0, 42.5, 21.5, isNormal = true, isExcluded = false))
        studyCycles.add(StudyCycle("CYC-03", "VS-001", 3, 42.5, 64.5, 22.0, isNormal = true, isExcluded = false))

        // Candidates for Cycle 1
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-101", studyId = "VS-001", cycleNumber = 1, sequence = 10,
                name = "Retrieve housing from bin", description = "Operator reaches for and grasps main housing.",
                startTime = 0.0, endTime = 2.5, activityCategory = VideoActivityCategory.PICK,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.95, validationStatus = ValidationStatus.USER_VALIDATED,
                materialNames = listOf("Housing"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Retrieve housing from bin", 0.0, 2.5, 2.5, VideoActivityCategory.PICK, ValueClassification.NNVA, 0.95)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-102", studyId = "VS-001", cycleNumber = 1, sequence = 20,
                name = "Inspect seal groove", description = "Visual verification of rubber seal placement.",
                startTime = 2.5, endTime = 4.5, activityCategory = VideoActivityCategory.INSPECT,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.88, validationStatus = ValidationStatus.USER_VALIDATED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Inspect seal groove", 2.5, 4.5, 2.0, VideoActivityCategory.INSPECT, ValueClassification.NNVA, 0.88)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-103", studyId = "VS-001", cycleNumber = 1, sequence = 30,
                name = "Position housing in fixture", description = "Places housing onto fixture locators.",
                startTime = 4.5, endTime = 6.2, activityCategory = VideoActivityCategory.POSITION,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.92, validationStatus = ValidationStatus.USER_VALIDATED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Position housing in fixture", 4.5, 6.2, 1.7, VideoActivityCategory.POSITION, ValueClassification.NNVA, 0.92)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-104", studyId = "VS-001", cycleNumber = 1, sequence = 40,
                name = "Align PCB board", description = "Aligns internal controller board to pins.",
                startTime = 6.2, endTime = 8.8, activityCategory = VideoActivityCategory.ALIGN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.91, validationStatus = ValidationStatus.USER_VALIDATED,
                materialNames = listOf("PCB Board"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Align PCB board", 6.2, 8.8, 2.6, VideoActivityCategory.ALIGN, ValueClassification.VA, 0.91)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-105", studyId = "VS-001", cycleNumber = 1, sequence = 50,
                name = "Fasten 4 torque screws", description = "Torque-tightens 4 M4 fasteners with automatic shutoff.",
                startTime = 8.8, endTime = 16.2, activityCategory = VideoActivityCategory.FASTEN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.96, validationStatus = ValidationStatus.USER_VALIDATED,
                toolNames = listOf("Torque Gun"), materialNames = listOf("M4 Screws x4"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Fasten 4 torque screws", 8.8, 16.2, 7.4, VideoActivityCategory.FASTEN, ValueClassification.VA, 0.96)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-106", studyId = "VS-001", cycleNumber = 1, sequence = 60,
                name = "Gage seat height", description = "Apply depth gage to verify board seating.",
                startTime = 16.2, endTime = 18.0, activityCategory = VideoActivityCategory.MEASURE,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.84, validationStatus = ValidationStatus.AI_SUGGESTED,
                toolNames = listOf("Depth Gage"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Gage seat height", 16.2, 18.0, 1.8, VideoActivityCategory.MEASURE, ValueClassification.NNVA, 0.84)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-107", studyId = "VS-001", cycleNumber = 1, sequence = 70,
                name = "Walk to staging rack", description = "Operator travels 3 steps to transfer assembled housing.",
                startTime = 18.0, endTime = 21.0, activityCategory = VideoActivityCategory.WALK,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.94, validationStatus = ValidationStatus.AI_SUGGESTED,
                notes = "Unnecessary walking detected outside golden zone.",
                originalAiSuggestion = ImmutableCandidateSnapshot("Walk to staging rack", 18.0, 21.0, 3.0, VideoActivityCategory.WALK, ValueClassification.NVA, 0.94)
            )
        )

        // Candidates for Cycle 2
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-201", studyId = "VS-001", cycleNumber = 2, sequence = 10,
                name = "Retrieve housing from bin", description = "Operator reaches for and grasps main housing.",
                startTime = 21.0, endTime = 23.4, activityCategory = VideoActivityCategory.PICK,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.96, validationStatus = ValidationStatus.AI_SUGGESTED,
                materialNames = listOf("Housing"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Retrieve housing from bin", 21.0, 23.4, 2.4, VideoActivityCategory.PICK, ValueClassification.NNVA, 0.96)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-202", studyId = "VS-001", cycleNumber = 2, sequence = 20,
                name = "Inspect seal groove", description = "Visual verification of rubber seal placement.",
                startTime = 23.4, endTime = 25.5, activityCategory = VideoActivityCategory.INSPECT,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.89, validationStatus = ValidationStatus.AI_SUGGESTED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Inspect seal groove", 23.4, 25.5, 2.1, VideoActivityCategory.INSPECT, ValueClassification.NNVA, 0.89)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-203", studyId = "VS-001", cycleNumber = 2, sequence = 30,
                name = "Position housing in fixture", description = "Places housing onto fixture locators.",
                startTime = 25.5, endTime = 27.3, activityCategory = VideoActivityCategory.POSITION,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.91, validationStatus = ValidationStatus.AI_SUGGESTED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Position housing in fixture", 25.5, 27.3, 1.8, VideoActivityCategory.POSITION, ValueClassification.NNVA, 0.91)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-204", studyId = "VS-001", cycleNumber = 2, sequence = 40,
                name = "Align PCB board", description = "Aligns internal controller board to pins.",
                startTime = 27.3, endTime = 29.8, activityCategory = VideoActivityCategory.ALIGN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.90, validationStatus = ValidationStatus.AI_SUGGESTED,
                materialNames = listOf("PCB Board"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Align PCB board", 27.3, 29.8, 2.5, VideoActivityCategory.ALIGN, ValueClassification.VA, 0.90)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-205", studyId = "VS-001", cycleNumber = 2, sequence = 50,
                name = "Fasten 4 torque screws", description = "Torque-tightens 4 M4 fasteners with automatic shutoff.",
                startTime = 29.8, endTime = 37.1, activityCategory = VideoActivityCategory.FASTEN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.94, validationStatus = ValidationStatus.AI_SUGGESTED,
                toolNames = listOf("Torque Gun"), materialNames = listOf("M4 Screws x4"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Fasten 4 torque screws", 29.8, 37.1, 7.3, VideoActivityCategory.FASTEN, ValueClassification.VA, 0.94)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-206", studyId = "VS-001", cycleNumber = 2, sequence = 60,
                name = "Gage seat height", description = "Apply depth gage to verify board seating.",
                startTime = 37.1, endTime = 39.0, activityCategory = VideoActivityCategory.MEASURE,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.82, validationStatus = ValidationStatus.AI_SUGGESTED,
                toolNames = listOf("Depth Gage"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Gage seat height", 37.1, 39.0, 1.9, VideoActivityCategory.MEASURE, ValueClassification.NNVA, 0.82)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-207", studyId = "VS-001", cycleNumber = 2, sequence = 70,
                name = "Walk to staging rack", description = "Operator travels 3.5 steps to transfer assembled housing.",
                startTime = 39.0, endTime = 42.5, activityCategory = VideoActivityCategory.WALK,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.93, validationStatus = ValidationStatus.AI_SUGGESTED,
                notes = "Walking waste",
                originalAiSuggestion = ImmutableCandidateSnapshot("Walk to staging rack", 39.0, 42.5, 3.5, VideoActivityCategory.WALK, ValueClassification.NVA, 0.93)
            )
        )

        // Candidates for Cycle 3
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-301", studyId = "VS-001", cycleNumber = 3, sequence = 10,
                name = "Retrieve housing from bin", description = "Operator reaches for and grasps main housing.",
                startTime = 42.5, endTime = 45.1, activityCategory = VideoActivityCategory.PICK,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.95, validationStatus = ValidationStatus.AI_SUGGESTED,
                materialNames = listOf("Housing"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Retrieve housing from bin", 42.5, 45.1, 2.6, VideoActivityCategory.PICK, ValueClassification.NNVA, 0.95)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-302", studyId = "VS-001", cycleNumber = 3, sequence = 20,
                name = "Inspect seal groove", description = "Visual verification of rubber seal placement.",
                startTime = 45.1, endTime = 47.0, activityCategory = VideoActivityCategory.INSPECT,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.86, validationStatus = ValidationStatus.AI_SUGGESTED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Inspect seal groove", 45.1, 47.0, 1.9, VideoActivityCategory.INSPECT, ValueClassification.NNVA, 0.86)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-303", studyId = "VS-001", cycleNumber = 3, sequence = 30,
                name = "Position housing in fixture", description = "Places housing onto fixture locators.",
                startTime = 47.0, endTime = 48.9, activityCategory = VideoActivityCategory.POSITION,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.90, validationStatus = ValidationStatus.AI_SUGGESTED,
                originalAiSuggestion = ImmutableCandidateSnapshot("Position housing in fixture", 47.0, 48.9, 1.9, VideoActivityCategory.POSITION, ValueClassification.NNVA, 0.90)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-304", studyId = "VS-001", cycleNumber = 3, sequence = 40,
                name = "Align PCB board", description = "Aligns internal controller board to pins.",
                startTime = 48.9, endTime = 51.3, activityCategory = VideoActivityCategory.ALIGN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.92, validationStatus = ValidationStatus.AI_SUGGESTED,
                materialNames = listOf("PCB Board"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Align PCB board", 48.9, 51.3, 2.4, VideoActivityCategory.ALIGN, ValueClassification.VA, 0.92)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-305", studyId = "VS-001", cycleNumber = 3, sequence = 50,
                name = "Fasten 4 torque screws", description = "Torque-tightens 4 M4 fasteners with automatic shutoff.",
                startTime = 51.3, endTime = 58.9, activityCategory = VideoActivityCategory.FASTEN,
                suggestedClassification = ValueClassification.VA, finalClassification = ValueClassification.VA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.95, validationStatus = ValidationStatus.AI_SUGGESTED,
                toolNames = listOf("Torque Gun"), materialNames = listOf("M4 Screws x4"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Fasten 4 torque screws", 51.3, 58.9, 7.6, VideoActivityCategory.FASTEN, ValueClassification.VA, 0.95)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-306", studyId = "VS-001", cycleNumber = 3, sequence = 60,
                name = "Gage seat height", description = "Apply depth gage to verify board seating.",
                startTime = 58.9, endTime = 60.8, activityCategory = VideoActivityCategory.MEASURE,
                suggestedClassification = ValueClassification.NNVA, finalClassification = ValueClassification.NNVA,
                wasteCategory = WasteCategory.NONE, confidenceScore = 0.83, validationStatus = ValidationStatus.AI_SUGGESTED,
                toolNames = listOf("Depth Gage"),
                originalAiSuggestion = ImmutableCandidateSnapshot("Gage seat height", 58.9, 60.8, 1.9, VideoActivityCategory.MEASURE, ValueClassification.NNVA, 0.83)
            )
        )
        videoCandidates.add(
            AICandidateElement(
                id = "CAND-307", studyId = "VS-001", cycleNumber = 3, sequence = 70,
                name = "Walk to staging rack", description = "Operator travels 3.7 steps to transfer assembled housing.",
                startTime = 60.8, endTime = 64.5, activityCategory = VideoActivityCategory.WALK,
                suggestedClassification = ValueClassification.NVA, finalClassification = ValueClassification.NVA,
                wasteCategory = WasteCategory.MOTION, confidenceScore = 0.91, validationStatus = ValidationStatus.AI_SUGGESTED,
                notes = "Walking waste",
                originalAiSuggestion = ImmutableCandidateSnapshot("Walk to staging rack", 60.8, 64.5, 3.7, VideoActivityCategory.WALK, ValueClassification.NVA, 0.91)
            )
        )
    }

    // ==========================================
    // MULTI-MODEL REPOSITORY METHODS & FALLBACKS
    // ==========================================

    fun getProductionPlanForProject(projectId: String): ProductionPlan {
        val existing = productionPlans.find { it.projectId == projectId }
        if (existing != null) return existing

        // Automatic fallback migration for legacy V1 single-model projects:
        val projectModels = models.filter { it.projectId == projectId && it.isActive }
        val totalDemand = projectModels.sumOf { it.demand }.coerceAtLeast(1)
        val defaultTakt = 60.0
        val plannedOperating = defaultTakt * totalDemand

        val newPlan = ProductionPlan(
            id = "PLAN-$projectId",
            projectId = projectId,
            name = "Production Plan (Standard)",
            period = "Current Period",
            shiftName = "Day Shift (8h)",
            availableOperatingSeconds = 28800.0,
            plannedOperatingSeconds = plannedOperating,
            plannedTotalQuantity = totalDemand,
            requiredTaktSeconds = defaultTakt,
            operatorCount = 2,
            workingCalendar = "Standard 5-Day"
        )
        productionPlans.add(newPlan)

        // Seed initial 100% or multi-mix items
        projectModels.forEach { model ->
            val mixPercent = if (totalDemand > 0) (model.demand.toDouble() / totalDemand.toDouble()) * 100.0 else 100.0
            val takt = if (model.demand > 0) plannedOperating / model.demand else defaultTakt
            modelMixItems.add(
                ModelMixItem(
                    id = "MIX-${model.id}",
                    planId = newPlan.id,
                    modelId = model.id,
                    modelName = model.name,
                    variant = model.variant,
                    plannedQuantity = model.demand,
                    calculatedMixPercentage = mixPercent,
                    manualMixOverride = null,
                    requiredTakt = takt,
                    standardWorkContent = 0.0,
                    isActive = true
                )
            )
        }

        return newPlan
    }

    fun getModelMixForPlan(planId: String): List<ModelMixItem> {
        return modelMixItems.filter { it.planId == planId }
    }

    fun getProductionSequencesForPlan(planId: String): List<ProductionSequence> {
        return productionSequences.filter { it.planId == planId }
    }

    fun saveProductionPlan(plan: ProductionPlan) {
        val idx = productionPlans.indexOfFirst { it.id == plan.id }
        if (idx >= 0) {
            productionPlans[idx] = plan
        } else {
            productionPlans.add(plan)
        }
    }

    fun saveModelMixItem(item: ModelMixItem) {
        val idx = modelMixItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            modelMixItems[idx] = item
        } else {
            modelMixItems.add(item)
        }
    }

    fun saveProductionSequence(seq: ProductionSequence) {
        val idx = productionSequences.indexOfFirst { it.id == seq.id }
        if (idx >= 0) {
            productionSequences[idx] = seq
        } else {
            productionSequences.add(seq)
        }
    }

    fun updateModelDemand(planId: String, modelId: String, newDemand: Int) {
        val modelIdx = models.indexOfFirst { it.id == modelId }
        if (modelIdx >= 0) {
            models[modelIdx] = models[modelIdx].copy(demand = newDemand)
        }

        // Recalculate plan total demand and mix percentages
        val plan = productionPlans.find { it.id == planId } ?: return
        val projectModels = models.filter { it.projectId == plan.projectId && it.isActive }
        val totalQuantity = projectModels.sumOf { it.demand }

        val updatedPlan = plan.copy(
            plannedTotalQuantity = totalQuantity,
            requiredTaktSeconds = if (totalQuantity > 0) plan.plannedOperatingSeconds / totalQuantity.toDouble() else plan.requiredTaktSeconds
        )
        saveProductionPlan(updatedPlan)

        projectModels.forEach { m ->
            val mixPercent = if (totalQuantity > 0) (m.demand.toDouble() / totalQuantity.toDouble()) * 100.0 else 0.0
            val takt = if (m.demand > 0) plan.plannedOperatingSeconds / m.demand.toDouble() else 0.0
            val existingMix = modelMixItems.find { it.planId == planId && it.modelId == m.id }
            if (existingMix != null) {
                saveModelMixItem(
                    existingMix.copy(
                        plannedQuantity = m.demand,
                        calculatedMixPercentage = mixPercent,
                        requiredTakt = takt
                    )
                )
            }
        }
    }

    // ==========================================
    // V2.2 VIDEO STUDY REPOSITORY METHODS
    // ==========================================

    fun getVideoStudies(projectId: String): List<VideoStudyMetadata> {
        return videoStudies.filter { it.projectId == projectId }
    }

    fun getVideoCandidates(studyId: String): List<AICandidateElement> {
        return videoCandidates.filter { it.studyId == studyId }
    }

    fun getStudyCycles(studyId: String): List<StudyCycle> {
        return studyCycles.filter { it.studyId == studyId }
    }

    fun saveVideoStudy(study: VideoStudyMetadata) {
        val index = videoStudies.indexOfFirst { it.id == study.id }
        if (index >= 0) {
            videoStudies[index] = study
        } else {
            videoStudies.add(study)
        }
    }

    fun saveCandidate(candidate: AICandidateElement) {
        val index = videoCandidates.indexOfFirst { it.id == candidate.id }
        if (index >= 0) {
            videoCandidates[index] = candidate
        } else {
            videoCandidates.add(candidate)
        }
    }

    fun removeCandidate(candidateId: String) {
        videoCandidates.removeAll { it.id == candidateId }
    }

    fun saveCycle(cycle: StudyCycle) {
        val index = studyCycles.indexOfFirst { it.id == cycle.id }
        if (index >= 0) {
            studyCycles[index] = cycle
        } else {
            studyCycles.add(cycle)
        }
    }

    fun commitCandidateToWorkElement(candidate: AICandidateElement, targetStationId: String? = null): WorkElement {
        val study = videoStudies.find { it.id == candidate.studyId }
        val project = projects.find { it.id == (study?.projectId ?: "P-001") } ?: projects.first()
        val station = stations.find { it.id == (targetStationId ?: study?.stationId ?: "ST-04") } ?: stations.first()
        val model = models.find { it.id == (study?.modelId ?: "MDL-1") } ?: models.first()
        val operator = operators.find { it.id == (study?.operatorId ?: "OP-1") } ?: operators.first()

        val nextSeq = (workElements.filter { it.stationId == station.id }.maxOfOrNull { it.sequence } ?: 0) + 10
        val workElementId = "WE-VID-${System.currentTimeMillis()}-${(100..999).random()}"

        val we = WorkElement(
            id = workElementId,
            projectId = project.id,
            modelId = model.id,
            processId = station.processId,
            stationId = station.id,
            operatorId = operator.id,
            sequence = nextSeq,
            name = candidate.name,
            description = candidate.description.ifBlank { "From Video Study: ${study?.name ?: "Observed"}" },
            startTime = candidate.startTime,
            endTime = candidate.endTime,
            observedTime = candidate.duration,
            normalTime = candidate.duration,
            standardTime = candidate.duration * 1.10, // 10% allowance assumption
            performanceRating = 1.0,
            allowance = 0.10,
            valueClassification = candidate.finalClassification,
            wasteCategory = candidate.wasteCategory,
            toolIds = candidate.toolNames,
            materialIds = candidate.materialNames,
            predecessorIds = emptyList(),
            successorIds = emptyList(),
            transferable = true,
            combinable = true,
            parallelizable = false,
            eliminable = candidate.finalClassification == ValueClassification.NVA,
            automationOpportunity = candidate.activityCategory == VideoActivityCategory.FASTEN || candidate.activityCategory == VideoActivityCategory.INSPECT,
            confidence = candidate.confidenceScore,
            evidenceReference = "Video_${study?.videoFileName ?: "study"}_${String.format(java.util.Locale.US, "%.2f", candidate.startTime)}s",
            notes = "Validated from Video Study '${study?.name ?: ""}' Cycle ${candidate.cycleNumber}",
            timeSource = TimeSource.OBSERVED,
            applicability = ElementApplicability.COMMON
        )

        addWorkElement(we)

        // Add corresponding Observation
        val obs = Observation(
            id = "OBS-VID-${System.currentTimeMillis()}-${(100..999).random()}",
            workElementId = we.id,
            cycleId = "CYC-${candidate.cycleNumber}",
            observedTime = candidate.duration,
            notes = "Recorded at video timestamp ${String.format(java.util.Locale.US, "%.2f", candidate.startTime)}s - ${String.format(java.util.Locale.US, "%.2f", candidate.endTime)}s"
        )
        observations.add(obs)

        // Link candidate to created element and update validation status
        saveCandidate(candidate.copy(elementId = we.id, validationStatus = ValidationStatus.USER_VALIDATED))

        return we
    }

    fun sendCandidateToWhatIfScenario(
        candidate: AICandidateElement,
        scenarioTitle: String,
        targetStationId: String
    ): Scenario {
        val study = videoStudies.find { it.id == candidate.studyId }
        val projectId = study?.projectId ?: "P-001"
        val scnId = "SCN-VID-${System.currentTimeMillis()}"

        val newScenario = Scenario(
            id = scnId,
            baseProjectId = projectId,
            name = scenarioTitle.ifBlank { "What-If: Relocate ${candidate.name}" },
            description = "Simulate moving element '${candidate.name}' (${String.format(java.util.Locale.US, "%.2f", candidate.duration)}s) to Station $targetStationId",
            createdAt = System.currentTimeMillis()
        )
        scenarios.add(newScenario)

        val tempElementId = candidate.elementId ?: "WE-002"
        val scenarioElement = ScenarioElement(
            id = "SE-${System.currentTimeMillis()}",
            scenarioId = scnId,
            baseWorkElementId = tempElementId,
            deltaObservedTime = candidate.duration,
            deltaStationId = targetStationId,
            deltaOperatorId = null,
            deltaSequence = null,
            deltaValueClassification = candidate.finalClassification
        )
        scenarioElements.add(scenarioElement)

        return newScenario
    }

    // --- AI IE COPILOT Methods ---

    private fun seedAiData() {
        val geminiProviderId = "P-GEMINI"
        aiProviders.add(AIProviderConfig(
            id = geminiProviderId,
            name = "Google Gemini",
            type = AIProviderType.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com",
            capabilities = listOf("text", "vision", "tool-calling", "streaming")
        ))

        aiModels.add(AIModelConfig(
            id = "M-GEMINI-FLASH",
            providerId = geminiProviderId,
            modelId = "gemini-3.5-flash",
            name = "Gemini Flash (General Analysis)",
            isDefault = true,
            taskType = "GENERAL"
        ))

        val nvidiaProviderId = "P-NVIDIA"
        aiProviders.add(AIProviderConfig(
            id = nvidiaProviderId,
            name = "NVIDIA NIM",
            type = AIProviderType.NVIDIA_NIM,
            baseUrl = "https://integrate.api.nvidia.com/v1",
            isActive = false,
            capabilities = listOf("text", "reasoning")
        ))

        aiModels.add(AIModelConfig(
            id = "M-NV-LLAMA-3-70B",
            providerId = nvidiaProviderId,
            modelId = "meta/llama-3.1-70b-instruct",
            name = "Llama-3.1 70B (Deep Reasoning)",
            taskType = "REASONING"
        ))
    }

    fun getAiProviders(): List<AIProviderConfig> = aiProviders
    fun getAiModels(): List<AIModelConfig> = aiModels
    
    fun saveAiProvider(provider: AIProviderConfig) {
        aiProviders.removeAll { it.id == provider.id }
        aiProviders.add(provider)
    }

    fun saveAiModel(model: AIModelConfig) {
        aiModels.removeAll { it.id == model.id }
        aiModels.add(model)
    }

    fun getAiChatMessages(projectId: String): List<AiChatMessage> {
        return aiChatMessages.filter { it.projectId == projectId }.sortedBy { it.timestamp }
    }

    fun saveAiChatMessage(message: AiChatMessage) {
        aiChatMessages.add(message)
    }

    fun deleteAiHistory(projectId: String) {
        aiChatMessages.removeAll { it.projectId == projectId }
    }

    // =============================================================================
    // V2.4 — OPERATIONAL EXCELLENCE REPOSITORY METHODS
    // =============================================================================

    fun getOeeRecords(projectId: String): List<OeeRecord> {
        return oeeRecords.filter { it.projectId == projectId }
    }

    fun getLossEvents(oeeRecordId: String): List<LossEvent> {
        return lossEvents.filter { it.oeeRecordId == oeeRecordId }
    }

    fun getRcaRecords(projectId: String): List<RcaRecord> {
        return rcaRecords.filter { it.problemStatement.isNotBlank() } // Generic filter or by lossId
    }

    fun getKaizenRecords(projectId: String): List<KaizenRecord> {
        return kaizenRecords.filter { it.title.isNotBlank() }
    }

    fun getStandardWorkRevisions(stationId: String, modelId: String): List<StandardWorkRevision> {
        return standardWorkRevisions.filter { it.stationId == stationId && it.modelId == modelId }
            .sortedByDescending { it.version }
    }

    fun getVsmMaps(projectId: String): List<VsmMap> {
        return vsmMaps.filter { it.projectId == projectId }
    }

    fun getErgoAssessments(projectId: String): List<ErgoAssessment> {
        return ergoAssessments.filter { it.projectId == projectId }
    }

    fun calculateOeeMetrics(record: OeeRecord): OeeMetrics {
        val losses = lossEvents.filter { it.oeeRecordId == record.id }
        
        val plannedDowntime = losses.filter { it.category == OeeLossCategory.PLANNED_DOWNTIME }.sumOf { it.durationMinutes }
        val availabilityLoss = losses.filter { it.category == OeeLossCategory.AVAILABILITY_LOSS }.sumOf { it.durationMinutes }
        
        val availableTime = (record.plannedProductionTimeMinutes - plannedDowntime).coerceAtLeast(0.0)
        val runTime = (availableTime - availabilityLoss).coerceAtLeast(0.0)
        
        val availability = if (availableTime > 0) runTime / availableTime else 0.0
        
        val performance = if (runTime > 0) {
            (record.idealCycleTimeSeconds * record.totalCount) / (runTime * 60.0)
        } else 0.0
        
        val quality = if (record.totalCount > 0) {
            record.goodCount.toDouble() / record.totalCount.toDouble()
        } else 0.0
        
        val oee = availability * performance * quality
        
        // Performance loss in minutes (theoretical vs actual run time)
        val perfLossMin = if (runTime > 0) {
            runTime - ((record.idealCycleTimeSeconds * record.totalCount) / 60.0)
        } else 0.0

        return OeeMetrics(
            availability = availability.coerceIn(0.0, 1.0),
            performance = performance.coerceIn(0.0, 1.0),
            quality = quality.coerceIn(0.0, 1.0),
            oee = oee.coerceIn(0.0, 1.0),
            plannedProductionTimeMinutes = record.plannedProductionTimeMinutes,
            availableTimeMinutes = availableTime,
            runTimeMinutes = runTime,
            availabilityLossMinutes = availabilityLoss.coerceAtLeast(0.0),
            performanceLossMinutes = perfLossMin.coerceAtLeast(0.0),
            qualityLossCount = record.rejectCount
        )
    }

    private fun seedOpExData() {
        val projectId = "P-001"
        val lineId = "LN-1"
        val date = "2026-09-18"
        
        // 0. Ergo
        ergoAssessments.add(ErgoAssessment(
            id = "ERGO-001",
            projectId = projectId,
            stationId = "ST-04",
            method = "RULA",
            assessmentDate = "2026-09-15",
            assessor = "IE Copilot AI",
            totalScore = 8,
            riskLevel = "High",
            findings = listOf("High trunk flexion (>60°)", "Excessive shoulder abduction", "Repetitive reaching >4x/min"),
            recommendation = "Install height-adjustable workstation and relocate fastener bin 20cm closer to operator center."
        ))

        // 1. OEE Record
        val oee1 = OeeRecord(
            id = "OEE-001",
            projectId = projectId,
            lineId = lineId,
            date = date,
            shift = "Day Shift",
            plannedProductionTimeMinutes = 480.0, // 8 hours
            idealCycleTimeSeconds = 27.0, // Takt
            totalCount = 850,
            goodCount = 820,
            rejectCount = 30
        )
        oeeRecords.add(oee1)
        
        // 2. Loss Events
        lossEvents.add(LossEvent("L-001", oee1.id, OeeLossCategory.PLANNED_DOWNTIME, "Scheduled Lunch & Meeting", 0L, 45.0))
        lossEvents.add(LossEvent("L-002", oee1.id, OeeLossCategory.AVAILABILITY_LOSS, "ST-04 Fastening Tool Failure", 0L, 35.0, stationId = "ST-04", m4Category = "Machine"))
        lossEvents.add(LossEvent("L-003", oee1.id, OeeLossCategory.AVAILABILITY_LOSS, "Material Delay - Housing Bin Empty", 0L, 15.0, stationId = "ST-04", m4Category = "Material"))
        
        // 3. RCA for Tool Failure
        val rca1 = RcaRecord(
            id = "RCA-001",
            problemStatement = "ST-04 Fastening Tool Repeated Failure",
            evidenceReference = "Maintenance Log #882",
            lossEventId = "L-002",
            status = RcaStatus.CAUSE_FOUND,
            fiveWhys = listOf(
                FiveWhyStep(1, "Tool shut off mid-cycle", "Operator observation"),
                FiveWhyStep(2, "Battery connection intermittent", "Internal inspection"),
                FiveWhyStep(3, "Vibration loosened the internal contacts", "Engineering teardown"),
                FiveWhyStep(4, "Mounting bracket design doesn't dampen 50Hz frequency", "CAD analysis"),
                FiveWhyStep(5, "Original design spec missed vibration harmonics for high-torque apps", "Design Review")
            ),
            rootCause = "Inadequate vibration dampening in tool mounting bracket for high-torque fastening.",
            countermeasure = "Replace standard bracket with dampened shock-mount variant.",
            owner = "Sarah Engineering",
            dueDate = "2026-09-25",
            validationStatus = ValidationStatus.USER_VALIDATED
        )
        rcaRecords.add(rca1)
        
        // 4. Kaizen for the RCA
        val kaizen1 = KaizenRecord(
            id = "KZN-001",
            title = "ST-04 Tool Vibration Dampening",
            rcaId = rca1.id,
            status = KaizenStatus.IMPLEMENTED,
            problem = "Excessive downtime due to tool battery disconnects caused by vibration.",
            countermeasure = "Installed vibration dampening housing for all ST-04 torque tools.",
            beforeMetricValue = 35.0, // 35 min downtime
            afterMetricValue = 2.0,   // 2 min downtime
            metricUnit = "Downtime Minutes/Shift",
            owner = "Maintenance Team",
            targetDate = "2026-09-20",
            actualDate = "2026-09-19",
            stationId = "ST-04",
            evidenceBefore = "L-002",
            validationStatus = ValidationStatus.USER_VALIDATED
        )
        kaizenRecords.add(kaizen1)
        
        // 5. Standard Work Updated
        val sw1 = StandardWorkRevision(
            id = "SW-001-V2",
            projectId = projectId,
            stationId = "ST-04",
            modelId = "MDL-1",
            version = 2,
            status = SwStatus.RELEASED,
            elements = listOf(
                StandardWorkElement("SWE-01", "WE-001", 1, "Retrieve housing", 2.75, keyPoints = "Use gravity bin"),
                StandardWorkElement("SWE-02", "WE-002", 2, "Fasten screws", 7.8, isQualityCheck = true, tools = listOf("Torque Gun (Dampened)"), keyPoints = "Verify shut-off light")
            ),
            taktTime = 27.0,
            author = "IE Team",
            kaizenId = kaizen1.id
        )
        standardWorkRevisions.add(sw1)
        
        // 6. VSM Map
        val vsm1 = VsmMap(
            id = "VSM-001",
            projectId = projectId,
            name = "Main Assembly Current State",
            steps = listOf(
                VsmProcessStep("VSM-S1", "Housing Sub-Assy", 15.0, 12.0, 95.0, 1, 15, stationId = "ST-04"),
                VsmProcessStep("VSM-S2", "Final Drive Assy", 22.0, 18.0, 88.0, 1, 10, stationId = "ST-05")
            ),
            demandPerDay = 500,
            availableTimeSeconds = 27000.0
        )
        vsmMaps.add(vsm1)
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
