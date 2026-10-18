package com.example.core.learning

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
 * Manages the learning lifecycle, ensuring that knowledge is compact, verified,
 * and guarded by explicit human approval.
 */
class LearningManager {

    private val learningRepository = mutableListOf<CompactSkillRecord>()

    fun getPendingProposals(): List<CompactSkillRecord> {
        return learningRepository.filter { it.state == LearningState.PENDING_REVIEW }
    }

    fun getVerifiedSkills(): List<CompactSkillRecord> {
        return learningRepository.filter {
            it.state == LearningState.VERIFIED || it.state == LearningState.AVAILABLE_FOR_REUSE
        }
    }

    fun submitCandidate(skill: CompactSkillRecord) {
        // Check for duplicates
        val existingIndex = learningRepository.indexOfFirst {
            it.resolvedPackageName == skill.resolvedPackageName && it.intendedTask == skill.intendedTask
        }
        if (existingIndex >= 0) {
            val existing = learningRepository[existingIndex]
            // Update evidence rather than duplicate
            learningRepository[existingIndex] = existing.copy(
                successfulExecutions = existing.successfulExecutions + 1,
                lastValidatedTimestampMs = System.currentTimeMillis()
            )
        } else {
            learningRepository.add(skill.copy(state = LearningState.PENDING_REVIEW))
        }
    }

    fun processReview(skillId: String, action: ReviewAction, userNote: String? = null): CompactSkillRecord? {
        val index = learningRepository.indexOfFirst { it.id == skillId }
        if (index < 0) return null
        val current = learningRepository[index]

        val newState = when (action) {
            ReviewAction.APPROVE -> {
                // If already technically validated, can become AVAILABLE_FOR_REUSE; otherwise APPROVED pending verification
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
        return updated
    }
}
