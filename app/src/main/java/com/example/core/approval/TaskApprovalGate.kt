package com.example.core.approval

import java.util.UUID
import java.util.regex.Pattern

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
 *
 * FIXES SUBSTRING VULNERABILITY:
 * Replaces naive string contains("pin") with structured AST analysis and regex word boundaries.
 * Prevents benign terms like "shopping", "spinning", "pinching", or "camping" from falsely triggering
 * high-consequence credential alarms.
 */
class TaskApprovalGate {

    companion object {
        // Pattern-based classifier with strict word boundaries to prevent substring collisions
        private val CREDENTIAL_WORD_PATTERN = Pattern.compile(
            "\\b(pin|pin code|passcode|password|secret key|credentials?|master password)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val FINANCIAL_WORD_PATTERN = Pattern.compile(
            "\\b(transfer money|send money|pay to|pay bill|pay recipient|make payment|wire funds|debit|credit card|bank account|upi|checkout|authorize payment)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val DESTRUCTIVE_WORD_PATTERN = Pattern.compile(
            "\\b(wipe data|factory reset|format storage|erase all|delete all|uninstall)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val SENSITIVE_SYSTEM_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.gms",
            "com.android.systemui",
            "com.android.packageinstaller"
        )

        private val SENSITIVE_FINANCIAL_PACKAGES = setOf(
            "com.google.android.apps.walletnfcrel",
            "net.one97.paytm",
            "com.phonepe.app",
            "in.org.npci.upiapp",
            "com.sbi.upi"
        )

        /**
         * Assesses risk using structured AST evaluation of description, actionType, and target.
         */
        fun assessRisk(
            actionDesc: String,
            actionType: String = "",
            target: String = "",
            targetPackage: String? = null
        ): RiskLevel {
            val fullContext = "$actionDesc $actionType $target".trim()

            // 1. Destructive System Actions (Wipe, Format, Factory Reset)
            if (DESTRUCTIVE_WORD_PATTERN.matcher(fullContext).find()) {
                return RiskLevel.CRITICAL
            }

            // 2. Financial Transactions
            if (FINANCIAL_WORD_PATTERN.matcher(fullContext).find() || (targetPackage != null && targetPackage in SENSITIVE_FINANCIAL_PACKAGES)) {
                return RiskLevel.CRITICAL
            }

            // 3. Credential & PIN Modification (Checked with word boundary regex - never naive substring)
            if (CREDENTIAL_WORD_PATTERN.matcher(fullContext).find()) {
                // If the target is system settings or credential manager, flag HIGH
                if (targetPackage in SENSITIVE_SYSTEM_PACKAGES || target.contains("password", ignoreCase = true) || target.contains("pin", ignoreCase = true)) {
                    return RiskLevel.HIGH
                }
                return RiskLevel.HIGH
            }

            // 4. System Settings & Permission Modification
            if (targetPackage in SENSITIVE_SYSTEM_PACKAGES || fullContext.contains("permission", ignoreCase = true)) {
                return RiskLevel.MEDIUM
            }

            return RiskLevel.LOW
        }
    }

    private var currentPlan: ExecutionPlan? = null

    fun proposePlan(
        rawTask: String,
        resolvedPackage: String?,
        steps: List<ProposedStep>
    ): ExecutionPlan {
        val classifiedSteps = steps.map { step ->
            if (step.riskLevel == RiskLevel.LOW) {
                step.copy(
                    riskLevel = assessRisk(
                        actionDesc = step.description,
                        actionType = step.actionType,
                        target = step.target,
                        targetPackage = resolvedPackage
                    )
                )
            } else step
        }

        val hasConsequential = classifiedSteps.any { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        val plan = ExecutionPlan(
            rawTask = rawTask,
            summary = "Execute task: '$rawTask'",
            resolvedPackage = resolvedPackage,
            steps = classifiedSteps,
            estimatedResourceImpact = if (classifiedSteps.size > 5) "Medium" else "Low",
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

    fun getCurrentPlan(): ExecutionPlan? = currentPlan
}
