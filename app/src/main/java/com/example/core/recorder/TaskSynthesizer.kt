package com.example.core.recorder

import com.example.core.model.ActionType
import com.example.core.model.RecordedInteraction
import com.example.core.model.ScrollDirection
import com.example.core.model.SynthesizedTask
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.VerificationRule
import java.util.UUID

class TaskSynthesizer {

    fun synthesize(
        interactions: List<RecordedInteraction>,
        taskName: String = "Recorded Macro Task",
        taskDescription: String? = null
    ): SynthesizedTask {
        if (interactions.isEmpty()) {
            return SynthesizedTask(
                name = taskName,
                description = "Empty task (no interactions recorded)",
                commands = emptyList(),
                rawInteractionsCount = 0
            )
        }

        val commands = mutableListOf<TaskCommand>()
        val primaryPackage = interactions.map { it.packageName }.firstOrNull { it.isNotBlank() && it != "com.example" }

        // Step 1: If recording starts inside an external app, infer launch command
        if (!primaryPackage.isNullOrBlank()) {
            val appLabel = formatPackageToAppName(primaryPackage)
            commands.add(
                TaskCommand.LaunchApp(
                    packageName = primaryPackage,
                    appName = appLabel
                )
            )
            commands.add(
                TaskCommand.VerifyCondition(
                    rule = VerificationRule.PackageMatches(primaryPackage),
                    requirementDescription = "$appLabel active in foreground"
                )
            )
        }

        // Step 2: Convert interactions into commands
        var i = 0
        while (i < interactions.size) {
            val event = interactions[i]

            // If user paused significantly, insert delay command
            if (event.delayFromPreviousMs > 1800L && commands.isNotEmpty()) {
                val boundedDelay = event.delayFromPreviousMs.coerceAtMost(3000L)
                commands.add(TaskCommand.Delay(boundedDelay))
            }

            when (event.actionType) {
                ActionType.TAP -> {
                    val (normX, normY) = if (event.touchCoordinates != null) {
                        val screenW = 1080f
                        val screenH = 2400f
                        Pair(
                            (event.touchCoordinates.first / screenW).coerceIn(0f, 1f),
                            (event.touchCoordinates.second / screenH).coerceIn(0f, 1f)
                        )
                    } else Pair(null, null)

                    val rawSelector = event.targetSelector ?: TargetSelector(text = event.targetLabel)
                    val selector = rawSelector.copy(
                        fallbackNormalizedX = normX,
                        fallbackNormalizedY = normY
                    )

                    commands.add(
                        TaskCommand.ClickElement(
                            selector = selector,
                            targetLabel = event.targetLabel
                        )
                    )

                    // Auto-generate post-click verification if target has specific identifiable text
                    val targetText = selector.text ?: selector.contentDescription
                    if (!targetText.isNullOrBlank() && targetText.length <= 15) {
                        commands.add(
                            TaskCommand.VerifyCondition(
                                rule = VerificationRule.DisplayContainsText(targetText),
                                requirementDescription = "UI interaction with '$targetText' confirmed"
                            )
                        )
                    }
                }

                ActionType.ENTER_TEXT -> {
                    // Studied from Tencent AppAgent: Deduplicate consecutive keystroke events on the same field
                    var textToEnter = event.textPayload ?: ""
                    while (i + 1 < interactions.size && interactions[i + 1].actionType == ActionType.ENTER_TEXT) {
                        textToEnter = interactions[i + 1].textPayload ?: textToEnter
                        i++
                    }

                    val selector = event.targetSelector ?: TargetSelector(className = "EditText")
                    commands.add(
                        TaskCommand.EnterText(
                            selector = selector,
                            textToEnter = textToEnter
                        )
                    )
                    commands.add(
                        TaskCommand.VerifyCondition(
                            rule = VerificationRule.DisplayContainsText(textToEnter),
                            requirementDescription = "Verify input field contains '$textToEnter'"
                        )
                    )
                }

                ActionType.SCROLL -> {
                    val dir = event.scrollDirection ?: ScrollDirection.DOWN
                    commands.add(
                        TaskCommand.Scroll(
                            direction = dir,
                            distanceFraction = 0.5f,
                            containerSelector = event.targetSelector
                        )
                    )
                }

                ActionType.SWIPE -> {
                    commands.add(
                        TaskCommand.Scroll(
                            direction = ScrollDirection.DOWN,
                            distanceFraction = 0.5f
                        )
                    )
                }

                ActionType.OPEN_DRAWER -> {
                    commands.add(TaskCommand.OpenDrawer())
                }

                ActionType.CLOSE_DRAWER -> {
                    commands.add(TaskCommand.CloseDrawer())
                }

                ActionType.ACTUATOR -> {
                    commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.HAPTIC_PULSE))
                }
            }

            i++
        }

        val generatedDescription = taskDescription
            ?: "Synthesized from ${interactions.size} interaction(s) across ${commands.size} autonomous command(s)."

        return SynthesizedTask(
            id = UUID.randomUUID().toString(),
            name = taskName,
            description = generatedDescription,
            commands = commands,
            rawInteractionsCount = interactions.size
        )
    }

    fun encodeCommands(commands: List<TaskCommand>): String {
        return commands.joinToString("\n") { cmd ->
            when (cmd) {
                is TaskCommand.LaunchApp -> "LAUNCH|${cmd.packageName}|${cmd.appName}"
                is TaskCommand.ClickElement -> "CLICK|${cmd.selector.text ?: ""}|${cmd.selector.contentDescription ?: ""}|${cmd.selector.resourceId ?: ""}|${cmd.targetLabel}|${cmd.selector.fallbackNormalizedX ?: ""}|${cmd.selector.fallbackNormalizedY ?: ""}"
                is TaskCommand.EnterText -> "ENTER_TEXT|${cmd.textToEnter}|${cmd.selector.text ?: ""}|${cmd.selector.resourceId ?: ""}"
                is TaskCommand.Scroll -> "SCROLL|${cmd.direction.name}|${cmd.distanceFraction}"
                is TaskCommand.OpenDrawer -> "OPEN_DRAWER"
                is TaskCommand.CloseDrawer -> "CLOSE_DRAWER"
                is TaskCommand.SwipeGesture -> "SWIPE|${cmd.startXRatio}|${cmd.startYRatio}|${cmd.endXRatio}|${cmd.endYRatio}|${cmd.durationMs}"
                is TaskCommand.Delay -> "DELAY|${cmd.durationMs}"
                is TaskCommand.WaitForElement -> "WAIT|${cmd.selector.text ?: ""}|${cmd.timeoutMs}"
                is TaskCommand.ControlActuator -> "ACTUATOR|${cmd.actuatorType.name}|${cmd.param}|${cmd.durationMs}"
                is TaskCommand.StartTimer -> "START_TIMER|${cmd.durationSeconds}|${cmd.timerLabel}"
                is TaskCommand.WebResearch -> "WEB_RESEARCH|${cmd.query}|${cmd.extractType}"
                is TaskCommand.AddNote -> "ADD_NOTE|${cmd.noteContent}|${cmd.title}"
                is TaskCommand.ExportDurableVault -> "EXPORT_VAULT|${cmd.destinationPath}"
                is TaskCommand.MorningBriefing -> "BRIEFING|${cmd.dateString}"
                is TaskCommand.VerifyCondition -> when (val rule = cmd.rule) {
                    is VerificationRule.DisplayContainsText -> "VERIFY_TEXT|${rule.expectedText}|${cmd.requirementDescription}"
                    is VerificationRule.PackageMatches -> "VERIFY_PKG|${rule.expectedPackage}|${cmd.requirementDescription}"
                    else -> "VERIFY_OTHER|${cmd.requirementDescription}"
                }
            }
        }
    }

    fun decodeCommands(payload: String): List<TaskCommand> {
        if (payload.isBlank()) return emptyList()
        val result = mutableListOf<TaskCommand>()
        payload.lines().forEach { line ->
            val parts = line.split("|")
            if (parts.isEmpty()) return@forEach
            when (parts[0]) {
                "LAUNCH" -> {
                    val pkg = parts.getOrNull(1) ?: return@forEach
                    val app = parts.getOrNull(2) ?: pkg
                    result.add(TaskCommand.LaunchApp(packageName = pkg, appName = app))
                }
                "CLICK" -> {
                    val text = parts.getOrNull(1)?.ifBlank { null }
                    val desc = parts.getOrNull(2)?.ifBlank { null }
                    val id = parts.getOrNull(3)?.ifBlank { null }
                    val label = parts.getOrNull(4) ?: text ?: desc ?: id ?: "Element"
                    val normX = parts.getOrNull(5)?.toFloatOrNull()
                    val normY = parts.getOrNull(6)?.toFloatOrNull()
                    result.add(
                        TaskCommand.ClickElement(
                            selector = TargetSelector(
                                text = text,
                                contentDescription = desc,
                                resourceId = id,
                                fallbackNormalizedX = normX,
                                fallbackNormalizedY = normY
                            ),
                            targetLabel = label
                        )
                    )
                }
                "ENTER_TEXT" -> {
                    val textToEnter = parts.getOrNull(1) ?: ""
                    val field = parts.getOrNull(2)?.ifBlank { null }
                    val id = parts.getOrNull(3)?.ifBlank { null }
                    result.add(TaskCommand.EnterText(selector = TargetSelector(text = field, resourceId = id, className = "EditText"), textToEnter = textToEnter))
                }
                "SCROLL" -> {
                    val dir = try { ScrollDirection.valueOf(parts.getOrNull(1) ?: "DOWN") } catch (e: Exception) { ScrollDirection.DOWN }
                    val dist = parts.getOrNull(2)?.toFloatOrNull() ?: 0.5f
                    result.add(TaskCommand.Scroll(direction = dir, distanceFraction = dist))
                }
                "OPEN_DRAWER" -> result.add(TaskCommand.OpenDrawer())
                "CLOSE_DRAWER" -> result.add(TaskCommand.CloseDrawer())
                "SWIPE" -> {
                    val sx = parts.getOrNull(1)?.toFloatOrNull() ?: 0.5f
                    val sy = parts.getOrNull(2)?.toFloatOrNull() ?: 0.8f
                    val ex = parts.getOrNull(3)?.toFloatOrNull() ?: 0.5f
                    val ey = parts.getOrNull(4)?.toFloatOrNull() ?: 0.2f
                    val dur = parts.getOrNull(5)?.toLongOrNull() ?: 300L
                    result.add(TaskCommand.SwipeGesture(startXRatio = sx, startYRatio = sy, endXRatio = ex, endYRatio = ey, durationMs = dur))
                }
                "DELAY" -> {
                    val dur = parts.getOrNull(1)?.toLongOrNull() ?: 500L
                    result.add(TaskCommand.Delay(durationMs = dur))
                }
                "WAIT" -> {
                    val text = parts.getOrNull(1) ?: ""
                    val timeout = parts.getOrNull(2)?.toLongOrNull() ?: 5000L
                    result.add(TaskCommand.WaitForElement(selector = TargetSelector(text = text), timeoutMs = timeout))
                }
                "ACTUATOR" -> {
                    val typeName = parts.getOrNull(1) ?: "HAPTIC_PULSE"
                    val type = try { com.example.core.model.ActuatorType.valueOf(typeName) } catch (_: Exception) { com.example.core.model.ActuatorType.HAPTIC_PULSE }
                    val param = parts.getOrNull(2) ?: ""
                    val dur = parts.getOrNull(3)?.toLongOrNull() ?: 200L
                    result.add(TaskCommand.ControlActuator(actuatorType = type, param = param, durationMs = dur))
                }
                "START_TIMER" -> {
                    val sec = parts.getOrNull(1)?.toIntOrNull() ?: 60
                    val label = parts.getOrNull(2) ?: "Agent Timer"
                    result.add(TaskCommand.StartTimer(durationSeconds = sec, timerLabel = label))
                }
                "WEB_RESEARCH" -> {
                    val q = parts.getOrNull(1) ?: ""
                    val extractType = parts.getOrNull(2) ?: "movie_list"
                    result.add(TaskCommand.WebResearch(query = q, extractType = extractType))
                }
                "VERIFY_TEXT" -> {
                    val expected = parts.getOrNull(1) ?: ""
                    val req = parts.getOrNull(2) ?: "Text present"
                    result.add(TaskCommand.VerifyCondition(rule = VerificationRule.DisplayContainsText(expected), requirementDescription = req))
                }
                "VERIFY_PKG" -> {
                    val pkg = parts.getOrNull(1) ?: ""
                    val req = parts.getOrNull(2) ?: "Package active"
                    result.add(TaskCommand.VerifyCondition(rule = VerificationRule.PackageMatches(pkg), requirementDescription = req))
                }
            }
        }
        return result
    }

    private fun formatPackageToAppName(packageName: String): String {
        return when {
            packageName.contains("calculator") -> "Calculator"
            packageName.contains("settings") -> "Settings"
            packageName.contains("camera") -> "Camera"
            packageName.contains("clock") -> "Clock"
            packageName.contains("contacts") -> "Contacts"
            else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}
