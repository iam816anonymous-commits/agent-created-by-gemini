package com.example.core.intent

import android.content.Context
import com.example.core.model.TaskCommand
import com.example.core.parser.NaturalLanguageCommandParser

/**
 * Tier 1 Deterministic Fast-Path Backend (<5ms latency).
 * Converts user instructions directly into structured action sequences via rules and regex patterns.
 */
class DeterministicRulesBackend(
    private val parser: NaturalLanguageCommandParser = NaturalLanguageCommandParser()
) : IntentBackend {

    override val name: String = "DeterministicRulesFastPath"
    override val tier: Int = 1

    override suspend fun isAvailable(context: Context): Boolean = true

    override suspend fun plan(context: Context, query: String, activePackage: String?): IntentPlanResult? {
        val start = System.currentTimeMillis()
        val commands = parser.parse(query)
        if (commands.isEmpty()) return null

        val steps = commands.mapIndexed { idx, cmd ->
            val (action, target) = when (cmd) {
                is TaskCommand.LaunchApp -> "LAUNCH_APP" to cmd.packageName
                is TaskCommand.ClickElement -> "CLICK_ELEMENT" to (cmd.targetLabel ?: cmd.selector.toReadableDescription())
                is TaskCommand.EnterText -> "ENTER_TEXT" to cmd.textToEnter
                is TaskCommand.ControlActuator -> "CONTROL_ACTUATOR" to cmd.actuatorType.name
                is TaskCommand.StartTimer -> "START_TIMER" to "${cmd.durationSeconds}s"
                is TaskCommand.WebResearch -> "WEB_RESEARCH" to cmd.query
                is TaskCommand.WaitForElement -> "WAIT_FOR_ELEMENT" to cmd.selector.toReadableDescription()
                is TaskCommand.VerifyCondition -> "VERIFY_CONDITION" to cmd.requirementDescription
                is TaskCommand.Delay -> "DELAY" to "${cmd.durationMs}ms"
                is TaskCommand.Scroll -> "SCROLL" to "${cmd.direction} (${cmd.distanceFraction})"
                is TaskCommand.TakeScreenshot -> "SCREENSHOT" to (cmd.tag ?: "capture")
            }
            IntentActionStep(
                id = "step_$idx",
                action = action,
                target = target,
                parameters = mapOf("command" to cmd.displayName)
            )
        }

        val latency = System.currentTimeMillis() - start
        return IntentPlanResult(
            sourceTier = 1,
            backendName = name,
            confidence = 0.95f,
            latencyMs = latency,
            steps = steps,
            isOfflineDegraded = false,
            provenance = "REGEX_DETERMINISTIC_FAST_PATH"
        )
    }
}
