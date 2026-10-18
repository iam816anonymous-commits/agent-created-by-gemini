package com.example.core.learning

import com.example.data.local.LearningRecordDao
import com.example.data.local.LearningRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Formal structured learning lifecycle for LocalAgent:
 * OBSERVED -> CANDIDATE -> PENDING_REVIEW -> APPROVED -> VERIFIED -> AVAILABLE_FOR_REUSE
 * Supporting states: REJECTED, NEEDS_MORE_EVIDENCE, FAILED_VALIDATION, SUPERSEDED
 */
enum class LearningState {
    OBSERVED,
    CANDIDATE,
    PENDING_REVIEW,
    APPROVED,
    VERIFIED,
    AVAILABLE_FOR_REUSE,
    REJECTED,
    NEEDS_MORE_EVIDENCE,
    FAILED_VALIDATION,
    SUPERSEDED
}

/**
 * Compact, reusable procedural knowledge record.
 * Avoids indiscriminate raw accessibility trees or screen dumps.
 */
data class CompactSkillRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetAppLabel: String,
    val resolvedPackageName: String,
    val intendedTask: String,
    val requiredPermissions: List<String> = emptyList(),
    val orderedExecutableSteps: List<String>,
    val targetResolutionRules: Map<String, String> = emptyMap(),
    val preconditions: List<String>,
    val postconditions: List<String>,
    val verificationStrategy: String,
    val knownLimitations: List<String>,
    val evidenceSummary: String,
    val successfulExecutions: Int = 1,
    val failedValidations: Int = 0,
    val state: LearningState = LearningState.PENDING_REVIEW,
    val userApprovalNote: String? = null,
    val createdTimestampMs: Long = System.currentTimeMillis(),
    val lastValidatedTimestampMs: Long? = null,
    val version: Int = 1
)

/**
 * User action taken during human-in-the-loop learning review.
 */
enum class ReviewAction {
    APPROVE,
    REJECT,
    TEST_FIRST,
    REMIND_LATER
}

/**
 * Extension mapper: Converts CompactSkillRecord domain model to persistent Room LearningRecordEntity.
 */
fun CompactSkillRecord.toEntity(): LearningRecordEntity {
    val permsJson = JSONArray(requiredPermissions).toString()
    val stepsJson = JSONArray(orderedExecutableSteps).toString()
    val rulesObj = JSONObject()
    targetResolutionRules.forEach { (k, v) -> rulesObj.put(k, v) }
    val precondsJson = JSONArray(preconditions).toString()
    val postcondsJson = JSONArray(postconditions).toString()
    val limitationsJson = JSONArray(knownLimitations).toString()

    return LearningRecordEntity(
        id = id,
        title = title,
        description = description,
        targetAppLabel = targetAppLabel,
        resolvedPackageName = resolvedPackageName,
        intendedTask = intendedTask,
        requiredPermissionsJson = permsJson,
        orderedExecutableStepsJson = stepsJson,
        targetResolutionRulesJson = rulesObj.toString(),
        preconditionsJson = precondsJson,
        postconditionsJson = postcondsJson,
        verificationStrategy = verificationStrategy,
        knownLimitationsJson = limitationsJson,
        evidenceSummary = evidenceSummary,
        successfulExecutions = successfulExecutions,
        failedValidations = failedValidations,
        state = state.name,
        userApprovalNote = userApprovalNote,
        createdTimestampMs = createdTimestampMs,
        lastValidatedTimestampMs = lastValidatedTimestampMs,
        version = version
    )
}

/**
 * Extension mapper: Converts persistent Room LearningRecordEntity to CompactSkillRecord domain model.
 */
fun LearningRecordEntity.toRecord(): CompactSkillRecord {
    val perms = mutableListOf<String>()
    try {
        val arr = JSONArray(requiredPermissionsJson)
        for (i in 0 until arr.length()) perms.add(arr.getString(i))
    } catch (_: Exception) {}

    val steps = mutableListOf<String>()
    try {
        val arr = JSONArray(orderedExecutableStepsJson)
        for (i in 0 until arr.length()) steps.add(arr.getString(i))
    } catch (_: Exception) {}

    val rules = mutableMapOf<String, String>()
    try {
        val obj = JSONObject(targetResolutionRulesJson)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            rules[key] = obj.optString(key, "")
        }
    } catch (_: Exception) {}

    val preconds = mutableListOf<String>()
    try {
        val arr = JSONArray(preconditionsJson)
        for (i in 0 until arr.length()) preconds.add(arr.getString(i))
    } catch (_: Exception) {}

    val postconds = mutableListOf<String>()
    try {
        val arr = JSONArray(postconditionsJson)
        for (i in 0 until arr.length()) postconds.add(arr.getString(i))
    } catch (_: Exception) {}

    val limits = mutableListOf<String>()
    try {
        val arr = JSONArray(knownLimitationsJson)
        for (i in 0 until arr.length()) limits.add(arr.getString(i))
    } catch (_: Exception) {}

    val parsedState = try {
        LearningState.valueOf(state)
    } catch (_: Exception) {
        LearningState.PENDING_REVIEW
    }

    return CompactSkillRecord(
        id = id,
        title = title,
        description = description,
        targetAppLabel = targetAppLabel,
        resolvedPackageName = resolvedPackageName,
        intendedTask = intendedTask,
        requiredPermissions = perms,
        orderedExecutableSteps = steps,
        targetResolutionRules = rules,
        preconditions = preconds,
        postconditions = postconds,
        verificationStrategy = verificationStrategy,
        knownLimitations = limits,
        evidenceSummary = evidenceSummary,
        successfulExecutions = successfulExecutions,
        failedValidations = failedValidations,
        state = parsedState,
        userApprovalNote = userApprovalNote,
        createdTimestampMs = createdTimestampMs,
        lastValidatedTimestampMs = lastValidatedTimestampMs,
        version = version
    )
}

/**
 * Manages the learning lifecycle with Room SQLite persistence, ensuring that knowledge
 * is compact, technically verified, guarded by explicit human approval, and survives app restart.
 */
class LearningManager(
    private val dao: LearningRecordDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val learningRepository = mutableListOf<CompactSkillRecord>()

    init {
        // Automatically hydrate state from Room SQLite if DAO provided
        dao?.let {
            try {
                runBlocking(Dispatchers.IO) {
                    reloadFromPersistentStore()
                }
            } catch (_: Exception) {
                // If running in asynchronous or testing context without blocking
                scope.launch {
                    reloadFromPersistentStore()
                }
            }
        }
    }

    /**
     * Reloads all learning records directly from Room SQLite database.
     * Guarantees recovery of pending and approved skills across process restarts.
     */
    suspend fun reloadFromPersistentStore(): List<CompactSkillRecord> {
        val daoInstance = dao ?: return synchronized(learningRepository) { learningRepository.toList() }
        val entities = daoInstance.getAllRecordsSnapshot()
        val records = entities.map { it.toRecord() }
        synchronized(learningRepository) {
            learningRepository.clear()
            learningRepository.addAll(records)
        }
        return records
    }

    @Synchronized
    fun getPendingProposals(): List<CompactSkillRecord> {
        return learningRepository.filter { it.state == LearningState.PENDING_REVIEW }
    }

    @Synchronized
    fun getApprovedSkills(): List<CompactSkillRecord> {
        return learningRepository.filter { it.state == LearningState.APPROVED }
    }

    @Synchronized
    fun getVerifiedSkills(): List<CompactSkillRecord> {
        return learningRepository.filter {
            it.state == LearningState.VERIFIED || it.state == LearningState.AVAILABLE_FOR_REUSE
        }
    }

    @Synchronized
    fun getAllSkills(): List<CompactSkillRecord> {
        return learningRepository.toList()
    }

    /**
     * Submits a newly observed workflow as a candidate for supervisor review.
     * Persists to Room SQLite immediately.
     */
    @Synchronized
    fun submitCandidate(skill: CompactSkillRecord) {
        // Check for duplicate workflows targeting same package and task
        val existingIndex = learningRepository.indexOfFirst {
            it.resolvedPackageName == skill.resolvedPackageName && it.intendedTask == skill.intendedTask
        }
        val recordToPersist: CompactSkillRecord

        if (existingIndex >= 0) {
            val existing = learningRepository[existingIndex]
            // Update evidence and execution counters rather than duplicate
            recordToPersist = existing.copy(
                successfulExecutions = existing.successfulExecutions + 1,
                lastValidatedTimestampMs = System.currentTimeMillis()
            )
            learningRepository[existingIndex] = recordToPersist
        } else {
            recordToPersist = skill.copy(state = LearningState.PENDING_REVIEW)
            learningRepository.add(recordToPersist)
        }

        dao?.let { targetDao ->
            scope.launch {
                targetDao.insertRecord(recordToPersist.toEntity())
            }
        }
    }

    /**
     * Synchronous candidate submission for sequential coroutine workflows.
     */
    suspend fun submitCandidateSync(skill: CompactSkillRecord) {
        val existing = dao?.findByPackageAndTask(skill.resolvedPackageName, skill.intendedTask)
        val recordToPersist: CompactSkillRecord

        if (existing != null) {
            val existingRecord = existing.toRecord()
            recordToPersist = existingRecord.copy(
                successfulExecutions = existingRecord.successfulExecutions + 1,
                lastValidatedTimestampMs = System.currentTimeMillis()
            )
        } else {
            recordToPersist = skill.copy(state = LearningState.PENDING_REVIEW)
        }

        dao?.insertRecord(recordToPersist.toEntity())
        synchronized(learningRepository) {
            val idx = learningRepository.indexOfFirst { it.id == recordToPersist.id }
            if (idx >= 0) {
                learningRepository[idx] = recordToPersist
            } else {
                learningRepository.add(recordToPersist)
            }
        }
    }

    /**
     * Processes human-in-the-loop supervisor review.
     *
     * Core Invariant: User approval does NOT automatically confer technical verification.
     * Technical verification requires at least 2 successful executions and 0 failed validations.
     * If unverified, it enters APPROVED state awaiting verification testing.
     */
    @Synchronized
    fun processReview(skillId: String, action: ReviewAction, userNote: String? = null): CompactSkillRecord? {
        val index = learningRepository.indexOfFirst { it.id == skillId }
        if (index < 0) return null
        val current = learningRepository[index]

        val newState = when (action) {
            ReviewAction.APPROVE -> {
                // User approval != Technical verification.
                // Transition to VERIFIED only if technical criteria are independently satisfied.
                if (current.successfulExecutions >= 2 && current.failedValidations == 0) {
                    LearningState.VERIFIED
                } else {
                    LearningState.APPROVED
                }
            }
            ReviewAction.REJECT -> LearningState.REJECTED
            ReviewAction.TEST_FIRST -> LearningState.NEEDS_MORE_EVIDENCE
            ReviewAction.REMIND_LATER -> LearningState.PENDING_REVIEW
        }

        val updated = current.copy(
            state = newState,
            userApprovalNote = userNote ?: current.userApprovalNote,
            lastValidatedTimestampMs = System.currentTimeMillis()
        )
        learningRepository[index] = updated

        dao?.let { targetDao ->
            scope.launch {
                targetDao.updateRecord(updated.toEntity())
            }
        }

        return updated
    }

    /**
     * Synchronous review processor for sequential coroutine workflows and testing.
     */
    suspend fun processReviewSync(skillId: String, action: ReviewAction, userNote: String? = null): CompactSkillRecord? {
        val existingEntity = dao?.getRecordById(skillId)
        val current = existingEntity?.toRecord() ?: synchronized(learningRepository) {
            learningRepository.firstOrNull { it.id == skillId }
        } ?: return null

        val newState = when (action) {
            ReviewAction.APPROVE -> {
                if (current.successfulExecutions >= 2 && current.failedValidations == 0) {
                    LearningState.VERIFIED
                } else {
                    LearningState.APPROVED
                }
            }
            ReviewAction.REJECT -> LearningState.REJECTED
            ReviewAction.TEST_FIRST -> LearningState.NEEDS_MORE_EVIDENCE
            ReviewAction.REMIND_LATER -> LearningState.PENDING_REVIEW
        }

        val updated = current.copy(
            state = newState,
            userApprovalNote = userNote ?: current.userApprovalNote,
            lastValidatedTimestampMs = System.currentTimeMillis()
        )

        dao?.updateRecord(updated.toEntity())
        synchronized(learningRepository) {
            val idx = learningRepository.indexOfFirst { it.id == skillId }
            if (idx >= 0) {
                learningRepository[idx] = updated
            } else {
                learningRepository.add(updated)
            }
        }

        return updated
    }
}
