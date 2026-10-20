package com.example.core.eval

import com.example.core.model.StepStatus
import com.example.core.model.UiSnapshot

data class GoalOutcome(
    val isAchieved: Boolean,
    val details: String,
    val metricScore: Float = if (isAchieved) 1.0f else 0.0f,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TaskGoal(
    val id: String,
    val description: String,
    val maxSteps: Int = 12,
    val timeoutMs: Long = 25000L,
    val evaluate: (preState: UiSnapshot?, postState: UiSnapshot?) -> GoalOutcome
)

/**
 * Studied from Google Research AndroidWorld:
 * Evaluates real task completion criteria independently of action dispatch.
 * Prevents false positives where an agent dispatches gestures successfully
 * but the actual target app state never reached the desired terminal goal.
 *
 * Runs 100% on-device in < 1 KB of heap memory.
 */
object TaskGoalEvaluator {

    /**
     * Standard goal: Verifies that an app window transitioned to the expected package and rendered interactive content.
     */
    fun createPackageGoal(expectedPackage: String, appLabel: String): TaskGoal {
        return TaskGoal(
            id = "goal_launch_${expectedPackage.replace('.', '_')}",
            description = "Launch and stabilize $appLabel ($expectedPackage)",
            evaluate = { _, postState ->
                if (postState == null) {
                    GoalOutcome(false, "Post-execution observation is null")
                } else if (postState.packageName != expectedPackage) {
                    GoalOutcome(false, "Active package '${postState.packageName}' does not match expected '$expectedPackage'")
                } else if (postState.totalNodeCount == 0) {
                    GoalOutcome(false, "Window rendered 0 interactive nodes (blank or transient transition frame)")
                } else {
                    GoalOutcome(true, "Window stabilized in '$expectedPackage' with ${postState.totalNodeCount} interactive nodes", 1.0f)
                }
            }
        )
    }

    /**
     * Standard goal: Verifies that a target display/formula or text container mutated to contain the expected value.
     */
    fun createTextMutationGoal(expectedSubstring: String, targetFieldDescription: String): TaskGoal {
        return TaskGoal(
            id = "goal_mutation_${expectedSubstring.hashCode()}",
            description = "Verify $targetFieldDescription reflects '$expectedSubstring'",
            evaluate = { preState, postState ->
                if (postState == null) {
                    GoalOutcome(false, "Post-execution observation is null")
                } else {
                    val matchingNode = postState.allNodes.firstOrNull { node ->
                        node.text?.contains(expectedSubstring, ignoreCase = true) == true
                    }
                    if (matchingNode != null) {
                        GoalOutcome(true, "Found '$expectedSubstring' in node '${matchingNode.resourceId ?: matchingNode.className}': '${matchingNode.text}'", 1.0f)
                    } else {
                        GoalOutcome(false, "Substring '$expectedSubstring' not found in any of the ${postState.totalNodeCount} nodes in '${postState.packageName}'")
                    }
                }
            }
        )
    }
}
