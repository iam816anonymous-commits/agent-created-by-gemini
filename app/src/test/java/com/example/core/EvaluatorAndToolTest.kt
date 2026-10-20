package com.example.core

import com.example.core.actuator.AgentTool
import com.example.core.actuator.ToolExecutionResult
import com.example.core.actuator.ToolRegistry
import com.example.core.eval.TaskGoalEvaluator
import com.example.core.model.RectData
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatorAndToolTest {

    private fun createNode(
        id: String,
        className: String,
        text: String? = null,
        bounds: RectData = RectData(0, 0, 100, 100)
    ): UiNode = UiNode(
        id = id,
        packageName = "com.google.android.calculator",
        className = className,
        text = text,
        contentDescription = null,
        resourceId = "com.google.android.calculator:id/result",
        bounds = bounds,
        isClickable = false,
        isEditable = false,
        isEnabled = true,
        isFocused = false,
        isScrollable = false,
        isVisibleToUser = true,
        depth = 0,
        children = emptyList()
    )

    @Test
    fun testTaskGoalEvaluator_verifiesPackageGoal() {
        val goal = TaskGoalEvaluator.createPackageGoal("com.google.android.calculator", "Calculator")

        val preSnapshot = UiSnapshot(
            packageName = "com.home.launcher",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 5,
            isLive = true
        )

        val successPostSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 12,
            isLive = true
        )

        val outcome = goal.evaluate(preSnapshot, successPostSnapshot)
        assertTrue("Goal should be achieved when window stabilizes in target package", outcome.isAchieved)
        assertEquals(1.0f, outcome.metricScore, 0.001f)

        // Blank window failure case
        val blankPostSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )
        val blankOutcome = goal.evaluate(preSnapshot, blankPostSnapshot)
        assertFalse("Goal should fail if window rendered 0 nodes", blankOutcome.isAchieved)
    }

    @Test
    fun testTaskGoalEvaluator_verifiesTextMutationGoal() {
        val goal = TaskGoalEvaluator.createTextMutationGoal("56", "Calculator Result")

        val resultNode = createNode("display", "android.widget.TextView", text = "56")
        val postSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = resultNode,
            totalNodeCount = 1,
            isLive = true
        )

        val outcome = goal.evaluate(null, postSnapshot)
        assertTrue("Goal should be achieved when formula contains '56'", outcome.isAchieved)

        val failureNode = createNode("display", "android.widget.TextView", text = "0")
        val failSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = failureNode,
            totalNodeCount = 1,
            isLive = true
        )
        val failOutcome = goal.evaluate(null, failSnapshot)
        assertFalse("Goal should fail if formula does not contain '56'", failOutcome.isAchieved)
    }

    @Test
    fun testToolRegistry_registersAndExecutesTool() = runBlocking {
        val registry = ToolRegistry()

        val mockTorchTool = object : AgentTool {
            override val name: String = "torch_control"
            override val description: String = "Turn device flashlight on or off"
            override val parameterSchemaJson: String = """{"enabled": "boolean"}"""

            override suspend fun execute(parameters: Map<String, Any?>): ToolExecutionResult {
                val enabled = parameters["enabled"] as? Boolean ?: false
                return ToolExecutionResult(
                    isSuccess = true,
                    message = "Torch set to $enabled",
                    outputData = mapOf("state" to if (enabled) "ON" else "OFF"),
                    executionTrace = listOf("Resolved Camera2 Manager", "Dispatched setTorchMode($enabled)")
                )
            }
        }

        registry.register(mockTorchTool)
        val retrieved = registry.get("torch_control")
        assertNotNull(retrieved)

        val result = retrieved?.execute(mapOf("enabled" to true))
        assertTrue(result?.isSuccess == true)
        assertEquals("ON", result?.outputData?.get("state"))
        assertEquals(2, result?.executionTrace?.size)
    }
}
