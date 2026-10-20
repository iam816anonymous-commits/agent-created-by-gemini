package com.example.core.engine

import com.example.core.model.StepStatus
import com.example.core.model.TaskCommand
import com.example.core.model.UiSnapshot
import java.util.UUID

data class SubGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetPackage: String? = null,
    val stepBudget: Int = 4,
    val plannedCommands: List<TaskCommand>,
    val rollbackCommand: TaskCommand? = null,
    val completionCheck: ((UiSnapshot?) -> Boolean)? = null
)

enum class SubGoalExecutionState {
    PENDING,
    IN_PROGRESS,
    ACCOMPLISHED,
    BUDGET_EXHAUSTED,
    ROLLBACK_TRIGGERED,
    FAILED
}

data class SubGoalProgress(
    val subGoal: SubGoal,
    var state: SubGoalExecutionState = SubGoalExecutionState.PENDING,
    var stepsConsumed: Int = 0,
    var failureReason: String? = null
)

data class HierarchicalPlan(
    val id: String = UUID.randomUUID().toString(),
    val overallGoal: String,
    val subGoals: List<SubGoal>,
    val maxTotalSteps: Int = 16,
    val createdAtMs: Long = System.currentTimeMillis()
)

sealed interface PlannerDecision {
    data class NextCommand(
        val subGoalId: String,
        val command: TaskCommand,
        val remainingBudgetInSubGoal: Int
    ) : PlannerDecision

    data class TriggerRollback(
        val subGoalId: String,
        val rollbackCommand: TaskCommand,
        val reason: String
    ) : PlannerDecision

    data class PlanCompleted(val summary: String) : PlannerDecision
    data class PlanAborted(val reason: String) : PlannerDecision
}

/**
 * Studied from MadeAgents MobileUse & skill-agent:
 * Hierarchical Task Planner with bounded sub-goal step budgets and dynamic rollback recovery.
 *
 * Prevents long-horizon task collapse, infinite retry loops, and blind repetition on low-memory devices.
 * Memory footprint: < 2 KB on heap.
 */
class HierarchicalTaskPlanner(
    val plan: HierarchicalPlan
) {
    private val subGoalProgressList = plan.subGoals.map { SubGoalProgress(it) }
    private var currentSubGoalIdx = 0
    private var currentCommandIdx = 0
    private var totalStepsConsumed = 0

    val currentSubGoal: SubGoal?
        get() = if (currentSubGoalIdx in subGoalProgressList.indices) {
            subGoalProgressList[currentSubGoalIdx].subGoal
        } else null

    val currentProgress: SubGoalProgress?
        get() = if (currentSubGoalIdx in subGoalProgressList.indices) {
            subGoalProgressList[currentSubGoalIdx]
        } else null

    val isFinished: Boolean
        get() = currentSubGoalIdx >= subGoalProgressList.size || totalStepsConsumed >= plan.maxTotalSteps

    /**
     * Determines the next action based on current sub-goal status, step budget, and active UI snapshot.
     */
    fun nextDecision(latestSnapshot: UiSnapshot?): PlannerDecision {
        if (totalStepsConsumed >= plan.maxTotalSteps) {
            return PlannerDecision.PlanAborted("Exceeded overall maximum step budget of ${plan.maxTotalSteps} steps")
        }

        if (currentSubGoalIdx >= subGoalProgressList.size) {
            return PlannerDecision.PlanCompleted("All ${subGoalProgressList.size} sub-goals successfully accomplished")
        }

        val progress = subGoalProgressList[currentSubGoalIdx]
        val subGoal = progress.subGoal

        // Check if current sub-goal is already satisfied by current UI observation
        if (subGoal.completionCheck?.invoke(latestSnapshot) == true) {
            progress.state = SubGoalExecutionState.ACCOMPLISHED
            currentSubGoalIdx++
            currentCommandIdx = 0
            return nextDecision(latestSnapshot)
        }

        // Check step budget exhaustion for current sub-goal
        if (progress.stepsConsumed >= subGoal.stepBudget) {
            progress.state = SubGoalExecutionState.BUDGET_EXHAUSTED
            if (subGoal.rollbackCommand != null && progress.state != SubGoalExecutionState.ROLLBACK_TRIGGERED) {
                progress.state = SubGoalExecutionState.ROLLBACK_TRIGGERED
                totalStepsConsumed++
                return PlannerDecision.TriggerRollback(
                    subGoalId = subGoal.id,
                    rollbackCommand = subGoal.rollbackCommand,
                    reason = "Sub-goal '${subGoal.title}' exhausted step budget (${subGoal.stepBudget} steps); triggering baseline rollback"
                )
            }
            return PlannerDecision.PlanAborted(
                "Sub-goal '${subGoal.title}' exhausted budget of ${subGoal.stepBudget} steps without achieving post-condition"
            )
        }

        // Dispatch next command within active sub-goal
        if (currentCommandIdx < subGoal.plannedCommands.size) {
            val cmd = subGoal.plannedCommands[currentCommandIdx]
            currentCommandIdx++
            progress.stepsConsumed++
            totalStepsConsumed++
            progress.state = SubGoalExecutionState.IN_PROGRESS

            val remainingBudget = subGoal.stepBudget - progress.stepsConsumed
            return PlannerDecision.NextCommand(
                subGoalId = subGoal.id,
                command = cmd,
                remainingBudgetInSubGoal = remainingBudget
            )
        }

        // If commands ran out but completionCheck didn't pass yet, evaluate snapshot
        if (subGoal.completionCheck?.invoke(latestSnapshot) == true) {
            progress.state = SubGoalExecutionState.ACCOMPLISHED
            currentSubGoalIdx++
            currentCommandIdx = 0
            return nextDecision(latestSnapshot)
        }

        // If no completionCheck was set and all commands dispatched, mark accomplished
        if (subGoal.completionCheck == null) {
            progress.state = SubGoalExecutionState.ACCOMPLISHED
            currentSubGoalIdx++
            currentCommandIdx = 0
            return nextDecision(latestSnapshot)
        }

        return PlannerDecision.PlanAborted("Commands completed for '${subGoal.title}', but postcondition verification failed")
    }

    /**
     * Reports execution feedback from actuator/verifier to advance or adjust planner state.
     */
    fun reportStepResult(stepStatus: StepStatus, details: String? = null) {
        val progress = currentProgress ?: return
        if (stepStatus == StepStatus.FAILURE || stepStatus == StepStatus.TIMEOUT) {
            progress.failureReason = details ?: "Step ended with status $stepStatus"
        }
    }

    fun getSummary(): String {
        val completedCount = subGoalProgressList.count { it.state == SubGoalExecutionState.ACCOMPLISHED }
        return "Hierarchical Plan [${plan.overallGoal}]: $completedCount/${subGoalProgressList.size} sub-goals completed, $totalStepsConsumed/${plan.maxTotalSteps} steps used"
    }
}
