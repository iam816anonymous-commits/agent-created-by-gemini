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
            val (action, target, extraParams) = when (cmd) {
                is TaskCommand.LaunchApp -> Triple("LAUNCH_APP", cmd.packageName, mapOf("appName" to cmd.appName))
                is TaskCommand.ClickElement -> Triple(
                    "CLICK_ELEMENT",
                    cmd.targetLabel,
                    mapOf(
                        "text" to (cmd.selector.text ?: cmd.targetLabel),
                        "targetLabel" to cmd.targetLabel
                    ) + (cmd.selector.resourceId?.let { mapOf("resourceId" to it) } ?: emptyMap())
                )
                is TaskCommand.EnterText -> Triple("ENTER_TEXT", cmd.textToEnter, mapOf("text" to cmd.textToEnter))
                is TaskCommand.ControlActuator -> Triple("CONTROL_ACTUATOR", cmd.actuatorType.name, mapOf("type" to cmd.actuatorType.name, "param" to cmd.param))
                is TaskCommand.StartTimer -> Triple("START_TIMER", "${cmd.durationSeconds}", mapOf("durationSeconds" to "${cmd.durationSeconds}", "label" to cmd.timerLabel))
                is TaskCommand.WebResearch -> Triple("WEB_RESEARCH", cmd.query, mapOf("query" to cmd.query, "extractType" to cmd.extractType))
                is TaskCommand.WaitForElement -> Triple(
                    "WAIT_FOR_ELEMENT",
                    cmd.selector.text ?: cmd.selector.toReadableDescription(),
                    mapOf("timeoutMs" to "${cmd.timeoutMs}", "text" to (cmd.selector.text ?: ""))
                )
                is TaskCommand.VerifyCondition -> {
                    val expected = when (val r = cmd.rule) {
                        is com.example.core.model.VerificationRule.DisplayContainsText -> r.expectedText
                        is com.example.core.model.VerificationRule.PackageMatches -> r.expectedPackage
                        else -> cmd.requirementDescription
                    }
                    Triple(
                        "VERIFY_CONDITION",
                        expected,
                        mapOf(
                            "requirementDescription" to cmd.requirementDescription,
                            "expectedText" to expected
                        )
                    )
                }
                is TaskCommand.Delay -> Triple("DELAY", "${cmd.durationMs}", mapOf("durationMs" to "${cmd.durationMs}"))
                is TaskCommand.Scroll -> Triple("SCROLL", cmd.direction.name, mapOf("direction" to cmd.direction.name, "distanceFraction" to "${cmd.distanceFraction}"))
                is TaskCommand.SwipeGesture -> Triple("SWIPE", "gesture", mapOf("startX" to "${cmd.startXRatio}", "startY" to "${cmd.startYRatio}", "endX" to "${cmd.endXRatio}", "endY" to "${cmd.endYRatio}", "durationMs" to "${cmd.durationMs}"))
                is TaskCommand.OpenDrawer -> Triple("OPEN_DRAWER", "drawer", emptyMap())
                is TaskCommand.CloseDrawer -> Triple("CLOSE_DRAWER", "drawer", emptyMap())
                is TaskCommand.MorningBriefing -> Triple("LAUNCH_APP", "com.google.android.deskclock", mapOf("appName" to "DeskClock"))
                is TaskCommand.AddNote -> Triple("ENTER_TEXT", cmd.noteContent, mapOf("text" to cmd.noteContent, "targetApp" to cmd.appPackage))
                is TaskCommand.ExportDurableVault -> Triple("CONTROL_ACTUATOR", "DURABLE_VAULT_SYNC", mapOf("type" to "DURABLE_VAULT_SYNC"))
            }
            IntentActionStep(
                id = "step_$idx",
                action = action,
                target = target,
                parameters = extraParams + mapOf("command" to cmd.displayName)
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
