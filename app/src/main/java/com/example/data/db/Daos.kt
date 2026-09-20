package com.example.data.db

import androidx.room.*
import com.example.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant)

    @Query("SELECT * FROM lines")
    fun getAllLines(): Flow<List<Line>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line)

    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM models WHERE projectId = :projectId")
    fun getModelsForProject(projectId: String): Flow<List<Model>>

    @Query("SELECT * FROM models")
    fun getAllModels(): Flow<List<Model>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: Model)
}

@Dao
interface ProcessDao {
    @Query("SELECT * FROM processes WHERE lineId = :lineId")
    fun getProcessesForLine(lineId: String): Flow<List<Process>>

    @Query("SELECT * FROM processes")
    fun getAllProcesses(): Flow<List<Process>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcess(process: Process)

    @Query("SELECT * FROM stations WHERE processId = :processId")
    fun getStationsForProcess(processId: String): Flow<List<Station>>

    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<Station>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: Station)

    @Query("SELECT * FROM operators")
    fun getAllOperators(): Flow<List<Operator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: Operator)
}

@Dao
interface WorkDao {
    @Query("SELECT * FROM work_elements WHERE projectId = :projectId")
    fun getWorkElementsForProject(projectId: String): Flow<List<WorkElement>>

    @Query("SELECT * FROM work_elements")
    fun getAllWorkElements(): Flow<List<WorkElement>>

    @Query("SELECT * FROM work_elements WHERE id = :id")
    suspend fun getWorkElementById(id: String): WorkElement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkElement(element: WorkElement)

    @Delete
    suspend fun deleteWorkElement(element: WorkElement)

    @Query("SELECT * FROM observations WHERE workElementId = :elementId")
    fun getObservationsForElement(elementId: String): Flow<List<Observation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: Observation)

    @Query("SELECT * FROM cycles WHERE workElementId = :elementId")
    fun getCyclesForElement(elementId: String): Flow<List<Cycle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: Cycle)
}

@Dao
interface OperationalDao {
    @Query("SELECT * FROM oee_records WHERE projectId = :projectId")
    fun getOeeRecords(projectId: String): Flow<List<OeeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOeeRecord(record: OeeRecord)

    @Query("SELECT * FROM kaizen_records")
    fun getAllKaizenRecords(): Flow<List<KaizenRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKaizenRecord(record: KaizenRecord)

    @Query("SELECT * FROM improvement_benefits WHERE kaizenId = :kaizenId")
    fun getImprovementBenefits(kaizenId: String): Flow<List<ImprovementBenefit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImprovementBenefit(benefit: ImprovementBenefit)

    @Query("SELECT * FROM loss_events WHERE oeeRecordId = :oeeRecordId")
    fun getLossEvents(oeeRecordId: String): Flow<List<LossEvent>>

    @Query("SELECT * FROM rca_records WHERE projectId = :projectId")
    fun getRcaRecords(projectId: String): Flow<List<RcaRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRcaRecord(record: RcaRecord)

    @Query("SELECT * FROM ergonomic_assessments WHERE projectId = :projectId")
    fun getErgoAssessments(projectId: String): Flow<List<ErgoAssessment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErgoAssessment(assessment: ErgoAssessment)

    @Query("SELECT * FROM model_mix_items WHERE planId = :planId")
    fun getModelMixForPlan(planId: String): Flow<List<ModelMixItem>>

    @Query("SELECT * FROM production_plans WHERE projectId = :projectId")
    fun getProductionPlans(projectId: String): Flow<List<ProductionPlan>>

    @Query("SELECT * FROM enterprise_kpis")
    fun getEnterpriseKpis(): Flow<List<EnterpriseKpi>>

    @Query("SELECT * FROM productivity_records")
    fun getProductivityRecords(): Flow<List<ProductivityRecord>>

    @Query("SELECT * FROM capacity_scenarios WHERE projectId = :projectId")
    fun getCapacityScenarios(projectId: String): Flow<List<CapacityScenario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapacityScenario(scenario: CapacityScenario)

    @Query("SELECT * FROM vsm_scenarios WHERE projectId = :projectId")
    fun getVsmScenarios(projectId: String): Flow<List<VsmState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVsmScenario(scenario: VsmState)

    @Query("SELECT * FROM standard_work_revisions WHERE projectId = :projectId")
    fun getStandardWorkRevisions(projectId: String): Flow<List<StandardWorkRevision>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandardWorkRevision(revision: StandardWorkRevision)

    @Query("SELECT * FROM simulation_results WHERE projectId = :projectId")
    fun getSimulationResults(projectId: String): Flow<List<SimulationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulationResult(result: SimulationResult)

    @Query("SELECT * FROM spaghetti_diagrams WHERE projectId = :projectId")
    fun getSpaghettiDiagrams(projectId: String): Flow<List<SpaghettiScenario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaghettiDiagram(diagram: SpaghettiScenario)

    @Query("SELECT * FROM motion_studies WHERE projectId = :projectId")
    fun getMotionStudies(projectId: String): Flow<List<MotionStudy>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionStudy(study: MotionStudy)

    @Query("SELECT * FROM savings_calculations WHERE projectId = :projectId")
    fun getSavingsCalculations(projectId: String): Flow<List<SavingsCalculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsCalculation(calculation: SavingsCalculation)

    @Query("SELECT * FROM savings_records WHERE kaizenId = :kaizenId")
    fun getSavingsRecords(kaizenId: String): Flow<List<SavingsRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsRecord(record: SavingsRecord)

    @Query("SELECT * FROM savings_validations WHERE benefitId = :benefitId")
    fun getSavingsValidations(benefitId: String): Flow<List<SavingsValidation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsValidation(validation: SavingsValidation)
}

@Dao
interface AiDao {
    @Query("SELECT * FROM ai_provider_configs")
    fun getAiProviders(): Flow<List<AIProviderConfig>>

    @Query("SELECT * FROM ai_model_configs")
    fun getAiModels(): Flow<List<AIModelConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiProvider(provider: AIProviderConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiModel(model: AIModelConfig)

    @Query("SELECT * FROM ai_chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getChatMessages(projectId: String): Flow<List<AiChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAiChatMessage(message: AiChatMessage)

    @Query("DELETE FROM ai_chat_messages WHERE projectId = :projectId")
    suspend fun deleteAiHistory(projectId: String)
}

@Dao
interface TimeStudyDao {
    @Query("SELECT * FROM time_study_templates")
    fun getAllTemplates(): Flow<List<TimeStudyTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TimeStudyTemplate)
}

@Dao
interface VideoStudyDao {
    @Query("SELECT * FROM video_studies WHERE projectId = :projectId")
    fun getVideoStudies(projectId: String): Flow<List<VideoStudyMetadata>>

    @Query("SELECT * FROM video_studies WHERE id = :id")
    suspend fun getVideoStudyById(id: String): VideoStudyMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoStudy(study: VideoStudyMetadata)

    @Query("SELECT * FROM video_candidates WHERE studyId = :studyId")
    fun getVideoCandidates(studyId: String): Flow<List<AICandidateElement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoCandidate(candidate: AICandidateElement)

    @Query("DELETE FROM video_candidates WHERE id = :id")
    suspend fun deleteVideoCandidate(id: String)

    @Query("SELECT * FROM study_cycles WHERE studyId = :studyId")
    fun getStudyCycles(studyId: String): Flow<List<StudyCycle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyCycle(cycle: StudyCycle)
}
