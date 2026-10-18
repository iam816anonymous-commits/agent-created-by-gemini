package com.example.core.parser

import com.example.core.model.ScrollDirection
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.VerificationRule
import java.util.Locale

class NaturalLanguageCommandParser(
    private val defaultCalculatorPackage: String = "com.google.android.calculator",
    private val fallbackCalculatorPackages: List<String> = listOf(
        "com.android.calculator2",
        "com.transsion.calculator",
        "com.google.android.calculator",
        "com.sec.android.app.popupcalculator",
        "com.miui.calculator"
    )
) {
    fun parse(prompt: String): List<TaskCommand> {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return emptyList()

        val normalized = trimmed.lowercase(Locale.ROOT)
        val commands = mutableListOf<TaskCommand>()

        // Case 1: Calculator end-to-end MVP demonstration
        if (normalized.contains("calculator") || normalized.contains("calc")) {
            commands.add(
                TaskCommand.LaunchApp(
                    packageName = defaultCalculatorPackage,
                    appName = "Calculator",
                    fallbackPackages = fallbackCalculatorPackages
                )
            )

            // Look for specific digit or key tap (e.g. tap 7, click 8, press 5, tap +, etc.)
            val digitMatch = Regex("""(?:tap|press|click|button)\s*([0-9\+\-\*\/\=])""").find(normalized)
            if (digitMatch != null) {
                val key = digitMatch.groupValues[1]
                commands.add(
                    TaskCommand.WaitForElement(
                        selector = TargetSelector(text = key, clickableOnly = false),
                        timeoutMs = 4000L
                    )
                )
                commands.add(
                    TaskCommand.ClickElement(
                        selector = TargetSelector(text = key, clickableOnly = false),
                        targetLabel = "Button $key"
                    )
                )
                // Independent post-action verification
                commands.add(
                    TaskCommand.VerifyCondition(
                        rule = VerificationRule.DisplayContainsText(key),
                        requirementDescription = "Calculator display/formula reflects '$key'"
                    )
                )
                return commands
            }

            // Calculation sequence like "calculate 7 + 5" or "7 + 8 ="
            val tokens = extractCalculationTokens(trimmed)
            if (tokens.isNotEmpty()) {
                for (token in tokens) {
                    commands.add(
                        TaskCommand.ClickElement(
                            selector = TargetSelector(text = token, clickableOnly = false),
                            targetLabel = "Calculator Key '$token'"
                        )
                    )
                }
                // Verify last token or result display
                val lastToken = tokens.lastOrNull()
                if (lastToken != null && lastToken != "=") {
                    commands.add(
                        TaskCommand.VerifyCondition(
                            rule = VerificationRule.DisplayContainsText(lastToken),
                            requirementDescription = "Calculator display reflects '$lastToken'"
                        )
                    )
                }
                return commands
            }

            // Default launch calculator verification if no specific button was requested
            commands.add(
                TaskCommand.VerifyCondition(
                    rule = VerificationRule.PackageMatches(defaultCalculatorPackage),
                    requirementDescription = "Calculator window is active in foreground"
                )
            )
            return commands
        }

        // Compound commands: e.g. "open settings and scroll down", "open contacts then tap add"
        if (normalized.contains(" and ") || normalized.contains(" then ") || normalized.contains(";")) {
            val delimiterRegex = Regex("""\s+(?:and|then)\s+|;\s*""")
            val subPhrases = trimmed.split(delimiterRegex).map { it.trim() }.filter { it.isNotEmpty() }
            if (subPhrases.size > 1) {
                val compoundCommands = mutableListOf<TaskCommand>()
                for (sub in subPhrases) {
                    val subCmds = parseSingleCommand(sub)
                    compoundCommands.addAll(subCmds)
                }
                if (compoundCommands.isNotEmpty()) {
                    return compoundCommands
                }
            }
        }

        return parseSingleCommand(trimmed)
    }

    private fun parseSingleCommand(trimmed: String): List<TaskCommand> {
        val normalized = trimmed.lowercase(Locale.ROOT)
        val commands = mutableListOf<TaskCommand>()

        // Actuator Controls (Hardware, Haptics, Flashlight, Audio, System Settings)
        if (normalized.contains("vibrate") || normalized.contains("buzz") || normalized.contains("haptic")) {
            val isBurst = normalized.contains("burst") || normalized.contains("double")
            commands.add(
                TaskCommand.ControlActuator(
                    actuatorType = if (isBurst) com.example.core.model.ActuatorType.HAPTIC_BURST else com.example.core.model.ActuatorType.HAPTIC_PULSE,
                    durationMs = if (isBurst) 400L else 200L
                )
            )
            return commands
        }

        if (normalized.contains("flashlight on") || normalized.contains("torch on") || normalized.contains("turn on flashlight") || normalized.contains("turn on torch")) {
            commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.TORCH_ON))
            return commands
        }
        if (normalized.contains("flashlight off") || normalized.contains("torch off") || normalized.contains("turn off flashlight") || normalized.contains("turn off torch")) {
            commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.TORCH_OFF))
            return commands
        }
        if (normalized.contains("toggle flashlight") || normalized.contains("toggle torch")) {
            commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.TORCH_TOGGLE))
            return commands
        }

        if (normalized.contains("beep") || normalized.contains("chime") || normalized.contains("sound alert") || normalized.contains("play tone")) {
            commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.AUDIO_TONE))
            return commands
        }

        if (normalized.contains("volume") || normalized.contains("mute")) {
            val percent = Regex("""(\d{1,3})%?""").find(normalized)?.groupValues?.get(1) ?: if (normalized.contains("mute")) "0" else "75"
            commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.AUDIO_VOLUME, param = percent))
            return commands
        }

        // Wi-Fi Controls (Turn ON, Turn OFF, Open Settings)
        if (normalized.contains("wifi") || normalized.contains("wi-fi")) {
            val isOn = normalized.contains("on") || normalized.contains("enable") || normalized.contains("start") || normalized.contains("switch on") || normalized.startsWith("on ")
            val isOff = normalized.contains("off") || normalized.contains("disable") || normalized.contains("stop") || normalized.contains("switch off") || normalized.startsWith("off ")
            if (isOn && !isOff) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.WIFI_ON))
                return commands
            } else if (isOff) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.WIFI_OFF))
                return commands
            } else if (normalized.contains("open") || normalized.contains("settings") || normalized.contains("panel")) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.SETTINGS_WIFI))
                return commands
            }
        }

        // Bluetooth Controls (Turn ON, Turn OFF, Open Settings)
        if (normalized.contains("bluetooth") || normalized.contains("bt")) {
            val isOn = normalized.contains("on") || normalized.contains("enable") || normalized.contains("start") || normalized.contains("switch on") || normalized.startsWith("on ")
            val isOff = normalized.contains("off") || normalized.contains("disable") || normalized.contains("stop") || normalized.contains("switch off") || normalized.startsWith("off ")
            if (isOn && !isOff) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.BLUETOOTH_ON))
                return commands
            } else if (isOff) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.BLUETOOTH_OFF))
                return commands
            } else if (normalized.contains("open") || normalized.contains("settings") || normalized.contains("panel")) {
                commands.add(TaskCommand.ControlActuator(com.example.core.model.ActuatorType.SETTINGS_BLUETOOTH))
                return commands
            }
        }

        // Timer Controls (start timer, set timer, timer 5 min, start timeer, etc.)
        if (normalized.contains("timer") || normalized.contains("timeer")) {
            val durationSeconds = parseTimerDurationSeconds(normalized)
            val labelMatch = Regex("""(?:for|called|named)\s+([a-zA-Z0-9\s]+)""").find(normalized)?.groupValues?.get(1)?.trim()
            val label = if (!labelMatch.isNullOrBlank() && !labelMatch.contains("min") && !labelMatch.contains("sec")) {
                labelMatch
            } else {
                "Agent Timer"
            }
            commands.add(TaskCommand.StartTimer(durationSeconds = durationSeconds, timerLabel = label))
            return commands
        }

        val isTextInput = normalized.startsWith("enter ") || normalized.startsWith("type ") || normalized.startsWith("write ")
        val isClickInput = normalized.startsWith("click ") || normalized.startsWith("tap ") || normalized.startsWith("press ")
        val isDrawerInput = normalized.contains("drawer")
        val isScrollInput = normalized.startsWith("scroll ") || normalized.startsWith("swipe ")

        // Web Research / Movie Scraping Commands
        // e.g. "search for new telugu movies", "research new telugu movies", "find telugu movies", "open google and search for X"
        if (!isTextInput && !isClickInput && !isDrawerInput && !isScrollInput &&
            (normalized.startsWith("search") || normalized.startsWith("research") || normalized.startsWith("scrape") ||
             normalized.contains("movie") || normalized.contains("telugu") || normalized.contains("cinema") ||
             normalized.contains("google and search"))) {
            var searchQuery = trimmed
            when {
                normalized.contains("google and search for ") -> searchQuery = trimmed.substring(normalized.indexOf("google and search for ") + 22).trim()
                normalized.contains("google and search ") -> searchQuery = trimmed.substring(normalized.indexOf("google and search ") + 18).trim()
                normalized.startsWith("search for ") -> searchQuery = trimmed.substring(11).trim()
                normalized.startsWith("search ") -> searchQuery = trimmed.substring(7).trim()
                normalized.startsWith("research ") -> searchQuery = trimmed.substring(9).trim()
                normalized.startsWith("scrape ") -> searchQuery = trimmed.substring(7).trim()
                normalized.startsWith("find ") -> searchQuery = trimmed.substring(5).trim()
                normalized.contains("search for ") -> searchQuery = trimmed.substring(normalized.indexOf("search for ") + 11).trim()
                normalized.contains("search ") -> searchQuery = trimmed.substring(normalized.indexOf("search ") + 7).trim()
            }
            val isMovie = normalized.contains("movie") || normalized.contains("telugu") || normalized.contains("film") || normalized.contains("cinema")
            commands.add(TaskCommand.WebResearch(query = searchQuery, extractType = if (isMovie) "movie_list" else "general"))
            return commands
        }

        // Case 2: Drawer Interactions
        if (normalized.contains("open drawer") || normalized.contains("open navigation") || normalized.contains("open menu") || normalized == "drawer") {
            commands.add(TaskCommand.OpenDrawer())
            return commands
        }
        if (normalized.contains("close drawer") || normalized.contains("close menu")) {
            commands.add(TaskCommand.CloseDrawer())
            return commands
        }

        // Case 3: Scrolling Interactions
        if (normalized.startsWith("scroll ") || normalized.startsWith("swipe ")) {
            val dir = when {
                normalized.contains("down") -> ScrollDirection.DOWN
                normalized.contains("up") -> ScrollDirection.UP
                normalized.contains("left") -> ScrollDirection.LEFT
                normalized.contains("right") -> ScrollDirection.RIGHT
                else -> ScrollDirection.DOWN
            }
            commands.add(TaskCommand.Scroll(direction = dir))
            return commands
        }

        // Case 4: Open/Launch specific apps
        if (normalized.startsWith("open ") || normalized.startsWith("launch ")) {
            val appTarget = trimmed.substringAfter(" ").trim()
            val targetPkg = when (appTarget.lowercase(Locale.ROOT)) {
                "settings" -> "com.android.settings"
                "camera" -> "com.android.camera"
                "contacts" -> "com.android.contacts"
                "clock" -> "com.google.android.deskclock"
                "files" -> "com.android.documentsui"
                else -> appTarget
            }
            commands.add(
                TaskCommand.LaunchApp(
                    packageName = targetPkg,
                    appName = appTarget
                )
            )
            commands.add(
                TaskCommand.VerifyCondition(
                    rule = VerificationRule.PackageMatches(targetPkg),
                    requirementDescription = "$appTarget foreground window active"
                )
            )
            return commands
        }

        // Case 5: Click / Tap element
        if (normalized.startsWith("click ") || normalized.startsWith("tap ") || normalized.startsWith("press ")) {
            val targetName = trimmed.substringAfter(" ").trim()
            commands.add(
                TaskCommand.ClickElement(
                    selector = TargetSelector(text = targetName),
                    targetLabel = targetName
                )
            )
            commands.add(
                TaskCommand.VerifyCondition(
                    rule = VerificationRule.DisplayContainsText(targetName),
                    requirementDescription = "Target UI interaction confirmed"
                )
            )
            return commands
        }

        // Case 6: Enter text
        if (normalized.startsWith("type ") || normalized.startsWith("enter ") || normalized.startsWith("write ")) {
            val remainder = trimmed.substringAfter(" ").trim()
            val textToEnter = if (remainder.contains(" into ")) {
                remainder.substringBefore(" into ").trim()
            } else if (remainder.contains(" in ")) {
                remainder.substringBefore(" in ").trim()
            } else {
                remainder
            }
            val fieldName = if (remainder.contains(" into ")) {
                remainder.substringAfter(" into ").trim()
            } else if (remainder.contains(" in ")) {
                remainder.substringAfter(" in ").trim()
            } else {
                null
            }
            commands.add(
                TaskCommand.EnterText(
                    selector = TargetSelector(text = fieldName, className = "EditText"),
                    textToEnter = textToEnter
                )
            )
            commands.add(
                TaskCommand.VerifyCondition(
                    rule = VerificationRule.DisplayContainsText(textToEnter),
                    requirementDescription = "Verify text '$textToEnter' present in UI"
                )
            )
            return commands
        }

        // Fallback: parse as single element click
        commands.add(
            TaskCommand.ClickElement(
                selector = TargetSelector(text = trimmed),
                targetLabel = trimmed
            )
        )
        return commands
    }

    private fun extractCalculationTokens(prompt: String): List<String> {
        val tokens = mutableListOf<String>()
        val regex = Regex("([0-9]+|\\+|\\-|\\*|\\/|\\=)")
        val matches = regex.findAll(prompt)
        for (match in matches) {
            val value = match.value
            if (value.length > 1 && value.all { it.isDigit() }) {
                for (digit in value) {
                    tokens.add(digit.toString())
                }
            } else {
                tokens.add(value)
            }
        }
        return tokens
    }

    private fun parseTimerDurationSeconds(normalized: String): Int {
        val minMatch = Regex("""(\d+)\s*(?:minutes?|mins?|m\b)""").find(normalized)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toIntOrNull() ?: 1
            return mins * 60
        }

        val secMatch = Regex("""(\d+)\s*(?:seconds?|secs?|s\b)""").find(normalized)
        if (secMatch != null) {
            return secMatch.groupValues[1].toIntOrNull() ?: 30
        }

        val hrMatch = Regex("""(\d+)\s*(?:hours?|hrs?|h\b)""").find(normalized)
        if (hrMatch != null) {
            val hrs = hrMatch.groupValues[1].toIntOrNull() ?: 1
            return hrs * 3600
        }

        val digitMatch = Regex("""(\d+)""").find(normalized)
        if (digitMatch != null) {
            val num = digitMatch.groupValues[1].toIntOrNull() ?: 60
            return if (num <= 10) num * 60 else num
        }

        return 60
    }
}
