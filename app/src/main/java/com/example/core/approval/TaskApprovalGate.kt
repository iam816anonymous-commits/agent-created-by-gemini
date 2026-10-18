package com.example.core.approval

import java.util.UUID

/**
 * Consequential risk rating for an execution step.
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class ProposedStep(
    val stepIndex: Int,
    val description: String,
    val actionType: String,
    val target: String,
    val requiredPermission: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

data class ExecutionPlan(
    val planId: String = UUID.randomUUID().toString(),
    val rawTask: String,
    val summary: String,
    val resolvedPackage: String?,
    val steps: List<ProposedStep>,
    val estimatedResourceImpact: String = "Low",
    val requiresElevatedConfirmation: Boolean = false,
    val isApproved: Boolean = false,
    val planVersion: Int = 1
)

/**
 * Human-in-the-loop task approval gate.
 * Enforces the non-negotiable rule: THE AGENT MUST ASK BEFORE STARTING A TASK.
 */
class TaskApprovalGate {

    private var currentPlan: ExecutionPlan? = null

    fun proposePlan(
        rawTask: String,
        resolvedPackage: String?,
        steps: List<ProposedStep>
    ): ExecutionPlan {
        val hasConsequential = steps.any { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        val plan = ExecutionPlan(
            rawTask = rawTask,
            summary = "Execute task: '$rawTask'",
            resolvedPackage = resolvedPackage,
            steps = steps,
            estimatedResourceImpact = if (steps.size > 5) "Medium" else "Low",
            requiresElevatedConfirmation = hasConsequential,
            isApproved = false,
            planVersion = 1
        )
        currentPlan = plan
        return plan
    }

    fun approvePlan(planId: String): Boolean {
        val plan = currentPlan
        if (plan != null && plan.planId == planId) {
            currentPlan = plan.copy(isApproved = true)
            return true
        }
        return false
    }

    fun rejectOrCancel(): ExecutionPlan? {
        val plan = currentPlan
        currentPlan = null
        return plan
    }

    fun canExecute(planId: String): Boolean {
        val plan = currentPlan ?: return false
        return plan.planId == planId && plan.isApproved
    }
}
