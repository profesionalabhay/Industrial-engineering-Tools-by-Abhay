package com.example.ai

import com.example.data.*
import java.util.*

class AIEngine(
    private val contextBuilder: AIContextBuilder
) {
    suspend fun analyze(
        prompt: String,
        project: Project,
        models: List<Model>,
        stations: List<Station>,
        elements: List<WorkElement>,
        plan: ProductionPlan?,
        mixItems: List<ModelMixItem>,
        oee: List<OeeMetrics>,
        losses: List<LossEvent>,
        rcas: List<RcaRecord>,
        kaizens: List<KaizenRecord>,
        history: List<AiChatMessage>,
        modelConfig: AIModelConfig,
        providerConfig: AIProviderConfig
    ): Result<AiChatMessage> {
        
        val systemInstruction = contextBuilder.buildSystemInstruction()
        val projectContext = contextBuilder.buildProjectContext(project, models, stations, elements, plan, mixItems)
        val opexContext = contextBuilder.buildOpExContext(oee, losses, rcas, kaizens)
        
        val fullHistory = mutableListOf<AiChatMessage>()
        
        // Add system instruction
        fullHistory.add(AiChatMessage(
            id = "sys-${UUID.randomUUID()}",
            projectId = project.id,
            role = AiRole.SYSTEM,
            content = systemInstruction + "\n\n" + projectContext + "\n\n" + opexContext
        ))
        
        // Add previous history (last 10 messages for context window efficiency)
        fullHistory.addAll(history.takeLast(10))
        
        // Add current prompt
        fullHistory.add(AiChatMessage(
            id = "user-${UUID.randomUUID()}",
            projectId = project.id,
            role = AiRole.USER,
            content = prompt
        ))

        val provider = AiProviderFactory.getProvider(providerConfig.type)
        return provider.generateResponse(fullHistory, modelConfig, providerConfig)
    }

    suspend fun analyzeVideoStudy(
        prompt: String,
        study: VideoStudyMetadata,
        elements: List<AICandidateElement>,
        stats: CycleStatistics,
        modelConfig: AIModelConfig,
        providerConfig: AIProviderConfig
    ): Result<AiChatMessage> {
        val systemInstruction = contextBuilder.buildSystemInstruction()
        val videoContext = contextBuilder.buildVideoEvidenceContext(study, elements, stats)
        
        val messages = listOf(
            AiChatMessage(
                id = "sys-${UUID.randomUUID()}",
                projectId = study.projectId,
                role = AiRole.SYSTEM,
                content = systemInstruction + "\n\n" + videoContext
            ),
            AiChatMessage(
                id = "user-${UUID.randomUUID()}",
                projectId = study.projectId,
                role = AiRole.USER,
                content = prompt
            )
        )

        val provider = AiProviderFactory.getProvider(providerConfig.type)
        return provider.generateResponse(messages, modelConfig, providerConfig)
    }
}
