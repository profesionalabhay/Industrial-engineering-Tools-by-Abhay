package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.*

@Database(
    entities = [
        Plant::class, Line::class, Project::class, Model::class,
        Process::class, Station::class, Operator::class,
        WorkElement::class, Observation::class, Cycle::class,
        Tool::class, Material::class, Dependency::class, Classification::class,
        Scenario::class, ScenarioElement::class, VSMProcess::class, LayoutObject::class,
        MotionEvent::class, CapacityPlan::class, ManpowerPlan::class,
        KaizenAction::class, SavingsRecord::class, StandardWork::class, ErgoAssessment::class,
        ProductionPlan::class, ModelMixItem::class, ProductionSequence::class,
        VideoStudyMetadata::class, AICandidateElement::class, StudyCycle::class,
        OeeRecord::class, LossEvent::class, RcaRecord::class, KaizenRecord::class,
        StandardWorkRevision::class, VsmMap::class, MaterialFlow::class,
        ImprovementBenefit::class, SimulationResult::class, EnterpriseKpi::class,
        ProductivityRecord::class, SavingsValidation::class, TimeStudyTemplate::class,
        AIProviderConfig::class, AIModelConfig::class, AiChatMessage::class, AiEvidence::class,
        CapacityScenario::class, VsmState::class, SpaghettiScenario::class, MotionStudy::class, SavingsCalculation::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun processDao(): ProcessDao
    abstract fun workDao(): WorkDao
    abstract fun operationalDao(): OperationalDao
    abstract fun aiDao(): AiDao
    abstract fun videoStudyDao(): VideoStudyDao
    abstract fun timeStudyDao(): TimeStudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manufacturing_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
