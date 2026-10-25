package com.example.core.intent

import com.example.core.model.ActuatorType
import com.example.core.model.ScrollDirection
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.VerificationRule
import java.util.Locale

/**
 * Strict Schema and Action Validator for Autonomous Intent Planning (Issue A08).
 * Validates model/backend generated plans before converting to executable TaskCommands.
 * Rejects malformed action types, out-of-bounds parameters, and invalid schemas.
 */
object PlanSchemaValidator {

    private const val MAX_PLAN_STEPS = 50
    private const val MAX_STRING_LENGTH = 4096
    private const val MAX_TARGET_LENGTH = 256

    sealed interface ValidationResult {
        data class Valid(val commands: List<TaskCommand>) : ValidationResult
        data class Invalid(val errors: List<String>) : ValidationResult
    }

    fun validateAndMap(steps: List<IntentActionStep>): ValidationResult {
        if (steps.isEmpty()) {
            return ValidationResult.Invalid(listOf("Plan contains zero action steps."))
        }

        if (steps.size > MAX_PLAN_STEPS) {
            return ValidationResult.Invalid(
                listOf("Plan step count (${steps.size}) exceeds safe limit of $MAX_PLAN_STEPS steps.")
            )
        }

        val errors = mutableListOf<String>()
        val commands = mutableListOf<TaskCommand>()

        for ((index, step) in steps.withIndex()) {
            val stepId = step.id.ifBlank { "step_$index" }
            val rawAction = step.action.trim().uppercase(Locale.ROOT)
            val target = step.target.trim()

            if (rawAction.isBlank()) {
                errors.add("Step $stepId: Missing action type.")
                continue
            }
            if (rawAction.length > 64) {
                errors.add("Step $stepId: Action name '$rawAction' exceeds maximum length.")
                continue
            }
            if (target.length > MAX_TARGET_LENGTH) {
                errors.add("Step $stepId: Target length (${target.length}) exceeds maximum limit ($MAX_TARGET_LENGTH).")
                continue
            }

            when (rawAction) {
                "LAUNCH_APP", "LAUNCH", "OPEN_APP" -> {
                    if (target.isBlank()) {
                        errors.add("Step $stepId: LAUNCH_APP requires non-blank target package or app name.")
                    } else {
                        val packageName = if (target.contains(".")) target else mapAppNameToPackage(target)
                        val appName = step.parameters["appName"] ?: target
                        commands.add(
                            TaskCommand.LaunchApp(
                                packageName = packageName,
                                appName = appName
                            )
                        )
                    }
                }

                "CLICK_ELEMENT", "CLICK", "TAP" -> {
                    if (target.isBlank()) {
                        errors.add("Step $stepId: CLICK requires a non-blank target label or selector.")
                    } else {
                        val fallbackX = step.parameters["fallbackX"]?.toFloatOrNull()
                        val fallbackY = step.parameters["fallbackY"]?.toFloatOrNull()

                        if (fallbackX != null && (fallbackX < 0.0f || fallbackX > 1.0f)) {
                            errors.add("Step $stepId: Coordinate fallbackX ($fallbackX) must be within [0.0, 1.0].")
                        } else if (fallbackY != null && (fallbackY < 0.0f || fallbackY > 1.0f)) {
                            errors.add("Step $stepId: Coordinate fallbackY ($fallbackY) must be within [0.0, 1.0].")
                        } else {
                            val resolvedText = step.parameters["text"]
                                ?: step.parameters["param"]
                                ?: run {
                                    val digitMatch = Regex("""(?:button|key|tap|press|click)?\s*([0-9\+\-\*\/\=])""", RegexOption.IGNORE_CASE).find(target)
                                    digitMatch?.groupValues?.get(1) ?: target
                                }
                            val selector = TargetSelector(
                                text = resolvedText,
                                contentDescription = step.parameters["contentDescription"] ?: if (target != resolvedText) target else null,
                                resourceId = step.parameters["resourceId"],
                                className = step.parameters["className"],
                                fallbackNormalizedX = fallbackX,
                                fallbackNormalizedY = fallbackY
                            )
                            commands.add(
                                TaskCommand.ClickElement(
                                    selector = selector,
                                    targetLabel = target
                                )
                            )
                        }
                    }
                }

                "ENTER_TEXT", "TYPE", "INPUT_TEXT" -> {
                    val textPayload = step.parameters["text"]
                        ?: step.parameters["textToEnter"]
                        ?: step.parameters["param"]
                        ?: target

                    if (textPayload.length > MAX_STRING_LENGTH) {
                        errors.add("Step $stepId: Text payload length (${textPayload.length}) exceeds safe limit ($MAX_STRING_LENGTH).")
                    } else {
                        val selector = TargetSelector(
                            text = if (target != textPayload && target.isNotBlank()) target else null,
                            className = "EditText"
                        )
                        commands.add(
                            TaskCommand.EnterText(
                                selector = selector,
                                textToEnter = textPayload
                            )
                        )
                    }
                }

                "WAIT_FOR_ELEMENT", "WAIT" -> {
                    val timeout = step.parameters["timeoutMs"]?.toLongOrNull() ?: 5000L
                    if (timeout < 50L || timeout > 60000L) {
                        errors.add("Step $stepId: WAIT timeout ($timeout ms) must be between 50ms and 60000ms.")
                    } else {
                        val resolvedText = step.parameters["text"]?.takeIf { it.isNotBlank() }
                            ?: run {
                                val digitMatch = Regex("""(?:button|key|tap|press|click)?\s*([0-9\+\-\*\/\=])""", RegexOption.IGNORE_CASE).find(target)
                                digitMatch?.groupValues?.get(1) ?: target.ifBlank { null }
                            }
                        val selector = TargetSelector(text = resolvedText)
                        commands.add(
                            TaskCommand.WaitForElement(
                                selector = selector,
                                timeoutMs = timeout
                            )
                        )
                    }
                }

                "VERIFY_CONDITION", "VERIFY" -> {
                    if (target.isBlank()) {
                        errors.add("Step $stepId: VERIFY requires a non-blank expected condition description.")
                    } else {
                        val expectedText = step.parameters["expectedText"]
                            ?: Regex("""['"]([^'"]+)['"]""").find(target)?.groupValues?.get(1)
                            ?: run {
                                val digitMatch = Regex("""(?:reflects|contains|value|is)?\s*([0-9\+\-\*\/\=])""", RegexOption.IGNORE_CASE).find(target)
                                digitMatch?.groupValues?.get(1) ?: target
                            }
                        val description = step.parameters["requirementDescription"] ?: target
                        val rule = if (target.startsWith("com.") || target.contains(".android.")) {
                            VerificationRule.PackageMatches(target)
                        } else {
                            VerificationRule.DisplayContainsText(expectedText)
                        }
                        commands.add(
                            TaskCommand.VerifyCondition(
                                rule = rule,
                                requirementDescription = description
                            )
                        )
                    }
                }

                "SCROLL" -> {
                    val dirName = (step.parameters["direction"] ?: target).uppercase(Locale.ROOT)
                    val direction = try {
                        ScrollDirection.valueOf(dirName)
                    } catch (_: Exception) {
                        ScrollDirection.DOWN
                    }
                    val fraction = step.parameters["distanceFraction"]?.toFloatOrNull() ?: 0.5f
                    if (fraction < 0.05f || fraction > 1.0f) {
                        errors.add("Step $stepId: Scroll distance fraction ($fraction) must be in [0.05, 1.0].")
                    } else {
                        commands.add(
                            TaskCommand.Scroll(
                                direction = direction,
                                distanceFraction = fraction
                            )
                        )
                    }
                }

                "SWIPE" -> {
                    val startX = step.parameters["startX"]?.toFloatOrNull() ?: 0.5f
                    val startY = step.parameters["startY"]?.toFloatOrNull() ?: 0.8f
                    val endX = step.parameters["endX"]?.toFloatOrNull() ?: 0.5f
                    val endY = step.parameters["endY"]?.toFloatOrNull() ?: 0.2f
                    val duration = step.parameters["durationMs"]?.toLongOrNull() ?: 300L

                    if (startX !in 0.0f..1.0f || startY !in 0.0f..1.0f || endX !in 0.0f..1.0f || endY !in 0.0f..1.0f) {
                        errors.add("Step $stepId: Swipe coordinates must all be in [0.0, 1.0].")
                    } else if (duration !in 50L..5000L) {
                        errors.add("Step $stepId: Swipe duration ($duration ms) must be in [50, 5000]ms.")
                    } else {
                        commands.add(
                            TaskCommand.SwipeGesture(
                                startXRatio = startX,
                                startYRatio = startY,
                                endXRatio = endX,
                                endYRatio = endY,
                                durationMs = duration
                            )
                        )
                    }
                }

                "DELAY", "SLEEP" -> {
                    val rawDuration = step.parameters["durationMs"] ?: target.filter { it.isDigit() }
                    val duration = rawDuration.toLongOrNull() ?: 1000L
                    if (duration !in 50L..60000L) {
                        errors.add("Step $stepId: Delay duration ($duration ms) must be within [50, 60000]ms.")
                    } else {
                        commands.add(TaskCommand.Delay(durationMs = duration))
                    }
                }

                "CONTROL_ACTUATOR", "ACTUATOR" -> {
                    val actuatorKey = (step.parameters["type"] ?: target).uppercase(Locale.ROOT)
                    val actuatorType = findActuatorType(actuatorKey)
                    if (actuatorType == null) {
                        errors.add("Step $stepId: Unknown or unsupported actuator type '$actuatorKey'.")
                    } else {
                        val param = step.parameters["param"] ?: ""
                        val duration = step.parameters["durationMs"]?.toLongOrNull() ?: 200L
                        commands.add(
                            TaskCommand.ControlActuator(
                                actuatorType = actuatorType,
                                param = param,
                                durationMs = duration
                            )
                        )
                    }
                }

                "START_TIMER", "TIMER" -> {
                    val rawSec = step.parameters["durationSeconds"] ?: step.parameters["seconds"] ?: target.filter { it.isDigit() }
                    val sec = rawSec.toIntOrNull() ?: 60
                    if (sec !in 1..86400) {
                        errors.add("Step $stepId: Timer duration ($sec seconds) must be between 1 and 86400.")
                    } else {
                        val label = step.parameters["label"] ?: "Agent Timer"
                        commands.add(TaskCommand.StartTimer(durationSeconds = sec, timerLabel = label))
                    }
                }

                "WEB_RESEARCH", "SEARCH" -> {
                    val query = target.ifBlank { step.parameters["query"] ?: "" }
                    if (query.isBlank()) {
                        errors.add("Step $stepId: Web research requires a non-blank search query.")
                    } else if (query.length > 500) {
                        errors.add("Step $stepId: Search query length (${query.length}) exceeds 500 characters.")
                    } else {
                        val extractType = step.parameters["extractType"] ?: "general"
                        commands.add(TaskCommand.WebResearch(query = query, extractType = extractType))
                    }
                }

                "OPEN_DRAWER" -> commands.add(TaskCommand.OpenDrawer())
                "CLOSE_DRAWER" -> commands.add(TaskCommand.CloseDrawer())

                else -> {
                    errors.add("Step $stepId: Unsupported or forbidden action type '$rawAction'.")
                }
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(commands)
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    private fun mapAppNameToPackage(appName: String): String {
        return when (appName.lowercase(Locale.ROOT)) {
            "calculator", "calc" -> "com.google.android.calculator"
            "clock", "alarm", "timer" -> "com.google.android.deskclock"
            "settings" -> "com.android.settings"
            "chrome", "browser" -> "com.android.chrome"
            "keep", "notes" -> "com.google.android.keep"
            "maps" -> "com.google.android.apps.maps"
            else -> appName
        }
    }

    private fun findActuatorType(key: String): ActuatorType? {
        // Direct enum match
        try {
            return ActuatorType.valueOf(key)
        } catch (_: Exception) {}

        // Friendly name mapping
        return when (key) {
            "TORCH_ON", "FLASHLIGHT_ON", "LIGHT_ON" -> ActuatorType.TORCH_ON
            "TORCH_OFF", "FLASHLIGHT_OFF", "LIGHT_OFF" -> ActuatorType.TORCH_OFF
            "TORCH_TOGGLE", "TOGGLE_FLASHLIGHT" -> ActuatorType.TORCH_TOGGLE
            "VIBRATE", "HAPTIC", "VIBRATION" -> ActuatorType.HAPTIC_PULSE
            "AUDIO_TONE", "BEEP", "CHIME" -> ActuatorType.AUDIO_TONE
            "WIFI_ON" -> ActuatorType.WIFI_ON
            "WIFI_OFF" -> ActuatorType.WIFI_OFF
            "BLUETOOTH_ON" -> ActuatorType.BLUETOOTH_ON
            "BLUETOOTH_OFF" -> ActuatorType.BLUETOOTH_OFF
            "SPEAKER_ALARM", "SIREN", "ALARM" -> ActuatorType.SPEAKER_ALARM
            "SPEAKER_CONFIRM" -> ActuatorType.SPEAKER_CONFIRM
            else -> null
        }
    }
}
