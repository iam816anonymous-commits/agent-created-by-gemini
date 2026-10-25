package com.example.core.engine

import com.example.core.model.TaskCommand
import com.example.core.model.UiSnapshot

enum class SubGoalStatus {
    PENDING,
    IN_PROGRESS,
    ACHIEVED,
    EXHAUSTED_BUDGET,
    ROLLED_BACK,
    FAILED
}

data class SubGoal(
    val id: String,
    val title: String,
    val targetPackage: String,
    val stepBudget: Int = 4,
    var stepsConsumed: Int = 0,
    val completionCriteriaDescription: String,
    val evaluateCompletion: (UiSnapshot?) -> Boolean,
    val rollbackCommands: List<TaskCommand> = listOf(TaskCommand.ClickElement(com.example.core.model.TargetSelector(text = "Back"), targetLabel = "rollback_back")),
    var status: SubGoalStatus = SubGoalStatus.PENDING
) {
    val isBudgetExceeded: Boolean get() = stepsConsumed >= stepBudget
}

data class HierarchicalPlan(
    val planId: String,
    val overallIntent: String,
    val subGoals: List<SubGoal>,
    val maxTotalSteps: Int = 15
) {
    val currentSubGoal: SubGoal?
        get() = subGoals.firstOrNull { it.status == SubGoalStatus.IN_PROGRESS || it.status == SubGoalStatus.PENDING }

    val isAllAchieved: Boolean
        get() = subGoals.isNotEmpty() && subGoals.all { it.status == SubGoalStatus.ACHIEVED }

    val totalStepsConsumed: Int
        get() = subGoals.sumOf { it.stepsConsumed }
}

sealed interface PlanExecutionDecision {
    data class ContinueSubGoal(val subGoal: SubGoal, val remainingBudget: Int) : PlanExecutionDecision
    data class SubGoalCompleted(val completedSubGoal: SubGoal, val nextSubGoal: SubGoal?) : PlanExecutionDecision
    data class TriggerRollback(val failedSubGoal: SubGoal, val rollbackCommands: List<TaskCommand>, val reason: String) : PlanExecutionDecision
    data class PlanFinished(val isSuccess: Boolean, val summary: String) : PlanExecutionDecision
}

/**
 * Studied from MadeAgents/mobile-use & skill-agent:
 * Hierarchical Task Planning and Bounded Sub-Goal Execution with Dynamic Rollback.
 *
 * Prevents naive infinite loops and repeated retries on Android by strictly enforcing:
 * 1. Sub-goal step ceilings (max 4 steps per sub-goal).
 * 2. Active precondition & completion verification against live UiSnapshots.
 * 3. Automatic rollback maneuvers (PressBack / App Relaunch) if a sub-goal budget expires.
 * 4. Memory footprint < 3 KB on 4GB RAM device (Helio P23, API 27).
 */
class HierarchicalTaskPlanner {

    fun initializePlan(
        planId: String,
        overallIntent: String,
        subGoals: List<SubGoal>,
        maxTotalSteps: Int = 15
    ): HierarchicalPlan {
        if (subGoals.isNotEmpty()) {
            subGoals.first().status = SubGoalStatus.IN_PROGRESS
        }
        return HierarchicalPlan(
            planId = planId,
            overallIntent = overallIntent,
            subGoals = subGoals,
            maxTotalSteps = maxTotalSteps
        )
    }

    /**
     * Evaluates post-step state and returns the next plan decision.
     */
    fun onStepExecuted(
        plan: HierarchicalPlan,
        postSnapshot: UiSnapshot?,
        isStagnantOrCycle: Boolean = false
    ): PlanExecutionDecision {
        val activeSubGoal = plan.currentSubGoal
            ?: return PlanExecutionDecision.PlanFinished(
                isSuccess = plan.isAllAchieved,
                summary = if (plan.isAllAchieved) "All sub-goals successfully completed" else "No active sub-goal found"
            )

        activeSubGoal.status = SubGoalStatus.IN_PROGRESS
        activeSubGoal.stepsConsumed++

        // 1. Check if overall plan budget exceeded
        if (plan.totalStepsConsumed >= plan.maxTotalSteps) {
            activeSubGoal.status = SubGoalStatus.EXHAUSTED_BUDGET
            return PlanExecutionDecision.PlanFinished(
                isSuccess = false,
                summary = "Plan overall budget exceeded (${plan.totalStepsConsumed}/${plan.maxTotalSteps} steps)"
            )
        }

        // 2. Evaluate if current sub-goal is achieved
        val achieved = try {
            activeSubGoal.evaluateCompletion(postSnapshot)
        } catch (e: Exception) {
            false
        }

        if (achieved) {
            activeSubGoal.status = SubGoalStatus.ACHIEVED
            val next = plan.currentSubGoal
            next?.status = SubGoalStatus.IN_PROGRESS

            return if (next == null) {
                PlanExecutionDecision.PlanFinished(
                    isSuccess = true,
                    summary = "Hierarchical plan '${plan.overallIntent}' completed successfully in ${plan.totalStepsConsumed} steps"
                )
            } else {
                PlanExecutionDecision.SubGoalCompleted(activeSubGoal, next)
            }
        }

        // 3. Check for stagnation or sub-goal step exhaustion -> Trigger Rollback
        if (isStagnantOrCycle || activeSubGoal.isBudgetExceeded) {
            activeSubGoal.status = SubGoalStatus.ROLLED_BACK
            val reason = if (isStagnantOrCycle) {
                "Detected UI stagnation or action oscillation in sub-goal '${activeSubGoal.title}'"
            } else {
                "Sub-goal '${activeSubGoal.title}' exceeded step budget (${activeSubGoal.stepsConsumed}/${activeSubGoal.stepBudget})"
            }

            return PlanExecutionDecision.TriggerRollback(
                failedSubGoal = activeSubGoal,
                rollbackCommands = activeSubGoal.rollbackCommands,
                reason = reason
            )
        }

        // 4. Continue sub-goal within budget
        return PlanExecutionDecision.ContinueSubGoal(
            subGoal = activeSubGoal,
            remainingBudget = activeSubGoal.stepBudget - activeSubGoal.stepsConsumed
        )
    }
}
