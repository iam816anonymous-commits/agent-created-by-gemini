package com.example.core

import com.example.core.diagnostics.ExecutionTracer
import com.example.core.diagnostics.TraceEvent
import com.example.core.engine.HierarchicalPlan
import com.example.core.engine.HierarchicalTaskPlanner
import com.example.core.engine.PlanExecutionDecision
import com.example.core.engine.SubGoal
import com.example.core.engine.SubGoalStatus
import com.example.core.model.RectData
import com.example.core.model.TaskCommand
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.resolver.SpatialAnchorQuery
import com.example.core.resolver.SpatialAnchorResolver
import com.example.core.resolver.SpatialDirection
import com.example.core.resolver.TargetWidgetRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedRepoMethodsTest {

    private fun createMockSnapshot(nodes: List<UiNode>): UiSnapshot {
        val root = if (nodes.isEmpty()) null else nodes.first().copy(children = nodes.drop(1))
        return UiSnapshot(
            packageName = "com.android.settings",
            rootNode = root,
            totalNodeCount = nodes.size,
            isLive = true
        )
    }

    // =========================================================================
    // Functionality 1: Spatial Anchor Resolution (Tencent AppAgent / OpenGUI)
    // =========================================================================
    @Test
    fun testSpatialAnchorResolver_findsSwitchToTheRightOfText() {
        // Label at (50, 200, 400, 260)
        val labelNode = UiNode(
            id = "node_label",
            packageName = "com.android.settings",
            className = "android.widget.TextView",
            text = "Dark theme",
            contentDescription = null,
            resourceId = "com.android.settings:id/title",
            bounds = RectData(50, 200, 400, 260),
            isClickable = false,
            isEditable = false,
            isEnabled = true,
            isFocused = false,
            isVisibleToUser = true,
            depth = 2
        )

        // Toggle Switch directly to the right at (900, 210, 1020, 250)
        val switchNode = UiNode(
            id = "node_switch",
            packageName = "com.android.settings",
            className = "android.widget.Switch",
            text = null,
            contentDescription = null,
            resourceId = "com.android.settings:id/switch_widget",
            bounds = RectData(900, 210, 1020, 250),
            isClickable = true,
            isEditable = false,
            isEnabled = true,
            isFocused = false,
            isVisibleToUser = true,
            depth = 2
        )

        val snapshot = createMockSnapshot(listOf(labelNode, switchNode))

        val query = SpatialAnchorQuery(
            anchorText = "Dark theme",
            direction = SpatialDirection.RIGHT_OF,
            expectedRole = TargetWidgetRole.SWITCH
        )

        val result = SpatialAnchorResolver.resolve(query, snapshot)
        assertTrue("Resolution should succeed", result is SpatialAnchorResolver.SpatialResolutionResult.Success)

        val success = result as SpatialAnchorResolver.SpatialResolutionResult.Success
        assertEquals("node_switch", success.resolvedTarget.id)
        assertEquals(960, success.clickCoordinates.first) // (900+1020)/2
        assertEquals(230, success.clickCoordinates.second) // (210+250)/2
    }

    @Test
    fun testSpatialAnchorResolver_returnsFailureWhenNoMatchingCandidate() {
        val labelNode = UiNode(
            id = "node_label",
            packageName = "com.android.settings",
            className = "android.widget.TextView",
            text = "Airplane mode",
            contentDescription = null,
            resourceId = null,
            bounds = RectData(50, 200, 400, 260),
            isClickable = false,
            isEditable = false,
            isEnabled = true,
            isFocused = false,
            isVisibleToUser = true,
            depth = 2
        )

        val snapshot = createMockSnapshot(listOf(labelNode))
        val query = SpatialAnchorQuery(
            anchorText = "Airplane mode",
            direction = SpatialDirection.RIGHT_OF,
            expectedRole = TargetWidgetRole.SWITCH
        )

        val result = SpatialAnchorResolver.resolve(query, snapshot)
        assertTrue(result is SpatialAnchorResolver.SpatialResolutionResult.Failure)
    }

    // =========================================================================
    // Functionality 2: Bounded Hierarchical Planning (MobileUse & skill-agent)
    // =========================================================================
    @Test
    fun testHierarchicalTaskPlanner_progressesThroughSubGoals() {
        val planner = HierarchicalTaskPlanner()

        val subGoal1 = SubGoal(
            id = "sg_1",
            title = "Launch Settings",
            targetPackage = "com.android.settings",
            stepBudget = 3,
            completionCriteriaDescription = "Active package is Settings",
            evaluateCompletion = { it?.packageName == "com.android.settings" }
        )

        val subGoal2 = SubGoal(
            id = "sg_2",
            title = "Toggle Switch",
            targetPackage = "com.android.settings",
            stepBudget = 3,
            completionCriteriaDescription = "Switch toggled",
            evaluateCompletion = { true }
        )

        val plan = planner.initializePlan("plan_test", "Enable Settings Toggle", listOf(subGoal1, subGoal2))

        assertEquals("sg_1", plan.currentSubGoal?.id)
        assertEquals(SubGoalStatus.IN_PROGRESS, plan.currentSubGoal?.status)

        // Step 1: Inconclusive state (still on home launcher)
        val homeSnapshot = UiSnapshot(
            packageName = "com.google.android.apps.nexuslauncher",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )
        val decision1 = planner.onStepExecuted(plan, homeSnapshot)
        assertTrue(decision1 is PlanExecutionDecision.ContinueSubGoal)
        assertEquals(1, subGoal1.stepsConsumed)

        // Step 2: Settings launched -> subGoal1 achieved!
        val settingsSnapshot = UiSnapshot(
            packageName = "com.android.settings",
            rootNode = null,
            totalNodeCount = 5,
            isLive = true
        )
        val decision2 = planner.onStepExecuted(plan, settingsSnapshot)
        assertTrue(decision2 is PlanExecutionDecision.SubGoalCompleted)
        assertEquals("sg_2", plan.currentSubGoal?.id)

        // Step 3: subGoal2 executed and achieved -> Plan finishes!
        val decision3 = planner.onStepExecuted(plan, settingsSnapshot)
        assertTrue(decision3 is PlanExecutionDecision.PlanFinished)
        val finish = decision3 as PlanExecutionDecision.PlanFinished
        assertTrue(finish.isSuccess)
    }

    @Test
    fun testHierarchicalTaskPlanner_triggersRollbackOnBudgetExhaustion() {
        val planner = HierarchicalTaskPlanner()

        val subGoal1 = SubGoal(
            id = "sg_stubborn",
            title = "Find Deep Nested Setting",
            targetPackage = "com.android.settings",
            stepBudget = 2,
            completionCriteriaDescription = "Find item",
            evaluateCompletion = { false }, // Never completes
            rollbackCommands = listOf(TaskCommand.PressBack("rollback_key"))
        )

        val plan = planner.initializePlan("plan_fail", "Fail test", listOf(subGoal1))

        // Step 1: Fails
        planner.onStepExecuted(plan, null)
        assertFalse(subGoal1.isBudgetExceeded)

        // Step 2: Fails again, budget 2 reached!
        val decision = planner.onStepExecuted(plan, null)
        assertTrue(decision is PlanExecutionDecision.TriggerRollback)
        val rollback = decision as PlanExecutionDecision.TriggerRollback
        assertEquals(SubGoalStatus.ROLLED_BACK, subGoal1.status)
        assertEquals(1, rollback.rollbackCommands.size)
    }

    // =========================================================================
    // Functionality 3: Structured Execution Tracing (DroidRun & testing)
    // =========================================================================
    @Test
    fun testExecutionTracer_recordsAndExportsMetrics() {
        val tracer = ExecutionTracer(capacity = 10)

        tracer.recordEvent(
            TraceEvent(
                stepIndex = 1,
                actionName = "LaunchApp",
                targetDescription = "com.android.settings",
                preStateHash = 1001L,
                postStateHash = 2002L,
                durationMs = 320L,
                status = "SUCCESS"
            )
        )

        tracer.recordEvent(
            TraceEvent(
                stepIndex = 2,
                actionName = "ClickElement",
                targetDescription = "Display",
                preStateHash = 2002L,
                postStateHash = 3003L,
                durationMs = 180L,
                isAncestorFallback = true,
                status = "SUCCESS"
            )
        )

        val metrics = tracer.computeMetrics()
        assertEquals(2, metrics.totalSteps)
        assertEquals(2, metrics.successfulSteps)
        assertEquals(1, metrics.ancestorFallbacksUsed)
        assertEquals(100f, metrics.successRate, 0.01f)

        val json = tracer.exportJson()
        assertTrue(json.contains("\"actionName\": \"LaunchApp\""))
        assertTrue(json.contains("\"isAncestorFallback\": true"))

        val markdown = tracer.generateMarkdownReport()
        assertTrue(markdown.contains("LocalAgent Execution Trace Report"))
        assertTrue(markdown.contains("| #1 | `LaunchApp` |"))
    }
}
