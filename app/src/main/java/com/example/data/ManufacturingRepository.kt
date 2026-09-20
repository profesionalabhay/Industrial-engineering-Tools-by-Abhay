package com.example.data

import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ManufacturingRepository(private val database: AppDatabase) {

    private val projectDao = database.projectDao()
    private val processDao = database.processDao()
    private val workDao = database.workDao()
    private val operationalDao = database.operationalDao()
    private val aiDao = database.aiDao()
    private val videoStudyDao = database.videoStudyDao()

    // --- Projects ---
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)
    suspend fun insertProject(project: Project) = projectDao.insertProject(project)
    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)

    fun getAllPlants(): Flow<List<Plant>> = projectDao.getAllPlants()
    suspend fun insertPlant(plant: Plant) = projectDao.insertPlant(plant)

    fun getAllLines(): Flow<List<Line>> = projectDao.getAllLines()
    suspend fun insertLine(line: Line) = projectDao.insertLine(line)

    fun getModelsForProject(projectId: String): Flow<List<Model>> = projectDao.getModelsForProject(projectId)
    suspend fun insertModel(model: Model) = projectDao.insertModel(model)

    // --- Processes & Stations ---
    fun getProcessesForLine(lineId: String): Flow<List<Process>> = processDao.getProcessesForLine(lineId)
    suspend fun insertProcess(process: Process) = processDao.insertProcess(process)

    fun getStationsForProcess(processId: String): Flow<List<Station>> = processDao.getStationsForProcess(processId)
    suspend fun insertStation(station: Station) = processDao.insertStation(station)

    fun getAllOperators(): Flow<List<Operator>> = processDao.getAllOperators()
    suspend fun insertOperator(operator: Operator) = processDao.insertOperator(operator)

    // --- Work Elements & Observations ---
    fun getWorkElementsForProject(projectId: String): Flow<List<WorkElement>> = workDao.getWorkElementsForProject(projectId)
    suspend fun getWorkElementById(id: String): WorkElement? = workDao.getWorkElementById(id)
    suspend fun insertWorkElement(element: WorkElement) = workDao.insertWorkElement(element)
    suspend fun deleteWorkElement(element: WorkElement) = workDao.deleteWorkElement(element)

    fun getObservationsForElement(elementId: String): Flow<List<Observation>> = workDao.getObservationsForElement(elementId)
    suspend fun insertObservation(observation: Observation) = workDao.insertObservation(observation)

    fun getCyclesForElement(elementId: String): Flow<List<Cycle>> = workDao.getCyclesForElement(elementId)
    suspend fun insertCycle(cycle: Cycle) = workDao.insertCycle(cycle)

    // --- Operational Excellence ---
    fun getOeeRecords(projectId: String): Flow<List<OeeRecord>> = operationalDao.getOeeRecords(projectId)
    suspend fun insertOeeRecord(record: OeeRecord) = operationalDao.insertOeeRecord(record)
    fun getLossEvents(oeeRecordId: String): Flow<List<LossEvent>> = operationalDao.getLossEvents(oeeRecordId)
    fun getRcaRecords(projectId: String): Flow<List<RcaRecord>> = operationalDao.getRcaRecords(projectId)
    suspend fun insertRcaRecord(record: RcaRecord) = operationalDao.insertRcaRecord(record)

    fun getAllKaizenRecords(): Flow<List<KaizenRecord>> = operationalDao.getAllKaizenRecords()
    suspend fun insertKaizenRecord(record: KaizenRecord) = operationalDao.insertKaizenRecord(record)

    fun getImprovementBenefits(kaizenId: String): Flow<List<ImprovementBenefit>> = operationalDao.getImprovementBenefits(kaizenId)
    suspend fun insertImprovementBenefit(benefit: ImprovementBenefit) = operationalDao.insertImprovementBenefit(benefit)

    fun getStandardWorkRevisions(projectId: String): Flow<List<StandardWorkRevision>> = operationalDao.getStandardWorkRevisions(projectId)
    suspend fun insertStandardWorkRevision(revision: StandardWorkRevision) = operationalDao.insertStandardWorkRevision(revision)

    fun getProductionPlans(projectId: String): Flow<List<ProductionPlan>> = operationalDao.getProductionPlans(projectId)
    fun getModelMixForPlan(planId: String): Flow<List<ModelMixItem>> = operationalDao.getModelMixForPlan(planId)

    // --- Enterprise ---
    fun getEnterpriseKpis(): Flow<List<EnterpriseKpi>> = operationalDao.getEnterpriseKpis()
    fun getProductivityRecords(): Flow<List<ProductivityRecord>> = operationalDao.getProductivityRecords()

    // --- Ergonomics ---
    fun getErgoAssessments(projectId: String): Flow<List<ErgoAssessment>> = operationalDao.getErgoAssessments(projectId)
    suspend fun insertErgoAssessment(assessment: ErgoAssessment) = operationalDao.insertErgoAssessment(assessment)

    // --- Capacity ---
    fun getCapacityScenarios(projectId: String): Flow<List<CapacityScenario>> = operationalDao.getCapacityScenarios(projectId)
    suspend fun insertCapacityScenario(scenario: CapacityScenario) = operationalDao.insertCapacityScenario(scenario)

    // --- VSM ---
    fun getVsmScenarios(projectId: String): Flow<List<VsmState>> = operationalDao.getVsmScenarios(projectId)
    suspend fun insertVsmScenario(scenario: VsmState) = operationalDao.insertVsmScenario(scenario)

    // --- Helpers for AI Context ---
    fun getAllModels(): Flow<List<Model>> = projectDao.getAllModels()
    fun getAllStations(): Flow<List<Station>> = processDao.getAllStations()
    fun getAllProcesses(): Flow<List<Process>> = processDao.getAllProcesses()
    fun getAllWorkElements(): Flow<List<WorkElement>> = workDao.getAllWorkElements()

    // --- AI & Copilot ---
    fun getAiProviders(): Flow<List<AIProviderConfig>> = aiDao.getAiProviders()
    fun getAiModels(): Flow<List<AIModelConfig>> = aiDao.getAiModels()
    suspend fun saveAiProvider(provider: AIProviderConfig) = aiDao.insertAiProvider(provider)
    suspend fun saveAiModel(model: AIModelConfig) = aiDao.insertAiModel(model)
    fun getAiChatMessages(projectId: String): Flow<List<AiChatMessage>> = aiDao.getChatMessages(projectId)
    suspend fun saveAiChatMessage(message: AiChatMessage) = aiDao.saveAiChatMessage(message)
    suspend fun deleteAiHistory(projectId: String) = aiDao.deleteAiHistory(projectId)

    fun calculateOeeMetrics(record: OeeRecord): OeeMetrics {
        // Deterministic IE Logic for OEE
        val totalTime = record.durationMinutes
        val availableTime = totalTime - record.plannedDowntimeMinutes
        val availability = if (availableTime > 0) (availableTime - record.unplannedDowntimeMinutes) / availableTime else 0.0
        
        val runTime = availableTime - record.unplannedDowntimeMinutes
        val idealCycleTime = record.idealCycleTimeSeconds / 60.0 // in minutes
        val performance = if (runTime > 0) (record.totalCount * idealCycleTime) / runTime else 0.0
        
        val quality = if (record.totalCount > 0) (record.totalCount - record.rejectCount).toDouble() / record.totalCount else 0.0
        
        return OeeMetrics(
            availability = availability.coerceIn(0.0, 1.0),
            performance = performance.coerceIn(0.0, 1.0),
            quality = quality.coerceIn(0.0, 1.0),
            oee = (availability * performance * quality).coerceIn(0.0, 1.0),
            plannedProductionTimeMinutes = totalTime,
            availableTimeMinutes = availableTime,
            runTimeMinutes = runTime,
            availabilityLossMinutes = record.unplannedDowntimeMinutes,
            performanceLossMinutes = runTime - (record.totalCount * idealCycleTime),
            qualityLossCount = record.rejectCount
        )
    }

    // --- Video Study ---
    fun getVideoStudies(projectId: String): Flow<List<VideoStudyMetadata>> = videoStudyDao.getVideoStudies(projectId)
    suspend fun getVideoStudyById(studyId: String): VideoStudyMetadata? = videoStudyDao.getVideoStudyById(studyId)
    suspend fun insertVideoStudy(study: VideoStudyMetadata) = videoStudyDao.insertVideoStudy(study)

    fun getVideoCandidates(studyId: String): Flow<List<AICandidateElement>> = videoStudyDao.getVideoCandidates(studyId)
    suspend fun insertVideoCandidate(candidate: AICandidateElement) = videoStudyDao.insertVideoCandidate(candidate)
    suspend fun deleteVideoCandidate(candidateId: String) = videoStudyDao.deleteVideoCandidate(candidateId)

    fun getStudyCycles(studyId: String): Flow<List<StudyCycle>> = videoStudyDao.getStudyCycles(studyId)
    suspend fun insertStudyCycle(cycle: StudyCycle) = videoStudyDao.insertStudyCycle(cycle)

    // --- Simulation ---
    fun getSimulationResults(projectId: String): Flow<List<SimulationResult>> = operationalDao.getSimulationResults(projectId)
    suspend fun insertSimulationResult(result: SimulationResult) = operationalDao.insertSimulationResult(result)

    // --- Spaghetti Diagram ---
    fun getSpaghettiDiagrams(projectId: String): Flow<List<SpaghettiScenario>> = operationalDao.getSpaghettiDiagrams(projectId)
    suspend fun insertSpaghettiDiagram(diagram: SpaghettiScenario) = operationalDao.insertSpaghettiDiagram(diagram)

    // --- Motion Study ---
    fun getMotionStudies(projectId: String): Flow<List<MotionStudy>> = operationalDao.getMotionStudies(projectId)
    suspend fun insertMotionStudy(study: MotionStudy) = operationalDao.insertMotionStudy(study)

    // --- Savings ---
    fun getSavingsCalculations(projectId: String): Flow<List<SavingsCalculation>> = operationalDao.getSavingsCalculations(projectId)
    suspend fun insertSavingsCalculation(calculation: SavingsCalculation) = operationalDao.insertSavingsCalculation(calculation)

    fun getSavingsRecords(kaizenId: String): Flow<List<SavingsRecord>> = operationalDao.getSavingsRecords(kaizenId)
    suspend fun insertSavingsRecord(record: SavingsRecord) = operationalDao.insertSavingsRecord(record)

    fun getSavingsValidations(benefitId: String): Flow<List<SavingsValidation>> = operationalDao.getSavingsValidations(benefitId)
    suspend fun insertSavingsValidation(validation: SavingsValidation) = operationalDao.insertSavingsValidation(validation)

    // --- Templates & Simulation ---
    fun getAllTimeStudyTemplates(): Flow<List<TimeStudyTemplate>> = database.timeStudyDao().getAllTemplates()
    suspend fun insertTimeStudyTemplate(template: TimeStudyTemplate) = database.timeStudyDao().insertTemplate(template)

    // Sample data seeding (can be called manually)
    suspend fun seedSampleData() {
        val plantId = "PLT-1"
        insertPlant(Plant(plantId, "Main Assembly Plant", "Detroit, MI"))
        
        val lineId = "LN-1"
        insertLine(Line(lineId, plantId, "Assembly Line 1"))
        
        val projectId = "P-001"
        insertProject(Project(projectId, lineId, "Line 1 Assembly Optimization", "Improving cycle time for main assembly line.", "Active"))
        
        val modelId = "MDL-1"
        insertModel(Model(modelId, projectId, "Delta Standard", 500, variant = "Base", family = "Delta Series"))
        
        val processId = "PRC-1"
        insertProcess(Process(processId, lineId, "Final Assembly"))
        
        val stationId = "ST-04"
        insertStation(Station(stationId, processId, "Station 04 - Housing"))
        
        insertOperator(Operator("OP-1", "John D.", "Expert"))
        
        val weId = "WE-001"
        insertWorkElement(WorkElement(
            id = weId, projectId = projectId, modelId = modelId, processId = processId, stationId = stationId, operatorId = "OP-1",
            sequence = 10, name = "Retrieve housing", description = "Get main housing from bin",
            startTime = 0.0, endTime = 2.5, observedTime = 2.5, normalTime = 2.5, standardTime = 2.75,
            performanceRating = 1.0, allowance = 0.1, valueClassification = ValueClassification.NNVA,
            wasteCategory = WasteCategory.MOTION, toolIds = emptyList(), materialIds = emptyList(),
            predecessorIds = emptyList(), successorIds = emptyList(),
            transferable = true, combinable = true, parallelizable = false, eliminable = false,
            automationOpportunity = false, confidence = 0.95, evidenceReference = "Vid_01_00:00", notes = "",
            applicability = ElementApplicability.COMMON
        ))
    }
}
