package com.example.data.repository

import com.example.core.model.StepExecutionRecord
import com.example.data.local.DiagnosticLogDao
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.StepRecordDao
import com.example.data.local.StepRecordEntity
import com.example.data.local.TaskExecutionDao
import com.example.data.local.TaskExecutionEntity
import com.example.data.local.SynthesizedTaskDao
import com.example.data.local.SynthesizedTaskEntity
import com.example.data.local.SkillDao
import com.example.data.local.SkillEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AgentRepository(
    private val taskExecutionDao: TaskExecutionDao,
    private val stepRecordDao: StepRecordDao,
    private val diagnosticLogDao: DiagnosticLogDao,
    private val synthesizedTaskDao: SynthesizedTaskDao,
    private val skillDao: SkillDao
) {
    val allExecutions: Flow<List<TaskExecutionEntity>> = taskExecutionDao.getAllExecutions()
    val recentLogs: Flow<List<DiagnosticLogEntity>> = diagnosticLogDao.getRecentLogs(300)
    val allSynthesizedTasks: Flow<List<SynthesizedTaskEntity>> = synthesizedTaskDao.getAllTasks()
    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()

    fun getStepsForExecution(executionId: String): Flow<List<StepRecordEntity>> {
        return stepRecordDao.getStepsForExecution(executionId)
    }

    fun getLogsForExecution(executionId: String): Flow<List<DiagnosticLogEntity>> {
        return diagnosticLogDao.getLogsForExecution(executionId)
    }

    suspend fun saveExecution(
        id: String,
        prompt: String,
        status: String,
        totalSteps: Int,
        successfulSteps: Int,
        startTimestamp: Long,
        endTimestamp: Long?,
        durationMs: Long,
        failureReason: String?,
        isLiveExecution: Boolean
    ) {
        taskExecutionDao.insertExecution(
            TaskExecutionEntity(
                id = id,
                prompt = prompt,
                status = status,
                totalSteps = totalSteps,
                successfulSteps = successfulSteps,
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp,
                durationMs = durationMs,
                failureReason = failureReason,
                isLiveExecution = isLiveExecution
            )
        )
    }

    suspend fun saveStepRecord(executionId: String, step: StepExecutionRecord) {
        stepRecordDao.insertStep(
            StepRecordEntity(
                id = UUID.randomUUID().toString(),
                executionId = executionId,
                stepIndex = step.stepIndex,
                commandType = step.commandSummary.takeWhile { it != ' ' && it != '[' },
                commandSummary = step.commandSummary,
                targetSelector = step.targetSelector,
                dispatchedAction = step.dispatchedAction,
                preSnapshotId = step.preSnapshotId,
                postSnapshotId = step.postSnapshotId,
                status = step.status.name,
                verificationDetails = step.verificationDetails,
                durationMs = step.durationMs,
                timestamp = step.timestampMs
            )
        )
    }

    suspend fun log(level: String, tag: String, message: String, executionId: String? = null) {
        diagnosticLogDao.insertLog(
            DiagnosticLogEntity(
                executionId = executionId,
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = tag,
                message = message
            )
        )
    }

    suspend fun saveSynthesizedTask(task: com.example.core.model.SynthesizedTask) {
        val payload = com.example.core.recorder.TaskSynthesizer().encodeCommands(task.commands)
        synthesizedTaskDao.insertTask(
            SynthesizedTaskEntity(
                id = task.id,
                name = task.name,
                description = task.description,
                commandCount = task.commands.size,
                rawInteractionsCount = task.rawInteractionsCount,
                createdAt = task.createdAt,
                commandsPayload = payload
            )
        )
    }

    suspend fun deleteSynthesizedTask(id: String) {
        synthesizedTaskDao.deleteTaskById(id)
    }

    suspend fun saveSkill(skill: com.example.core.model.AgentSkill) {
        val payload = com.example.core.recorder.TaskSynthesizer().encodeCommands(skill.commands)
        skillDao.insertSkill(
            SkillEntity(
                id = skill.id,
                name = skill.name,
                description = skill.description,
                triggerPhrases = skill.triggerPhrases.joinToString(","),
                category = skill.category,
                commandsPayload = payload,
                isBuiltIn = skill.isBuiltIn,
                createdAt = skill.createdAt
            )
        )
    }

    suspend fun deleteSkill(id: String) {
        skillDao.deleteSkillById(id)
    }

    suspend fun getSkillCount(): Int = skillDao.getSkillCount()

    suspend fun clearHistory() {
        taskExecutionDao.clearAll()
        stepRecordDao.clearAll()
        diagnosticLogDao.clearLogs()
        synthesizedTaskDao.clearAll()
    }
}
