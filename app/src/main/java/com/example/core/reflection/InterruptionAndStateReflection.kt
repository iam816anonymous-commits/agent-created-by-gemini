package com.example.core.reflection

import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot

enum class InterruptionType {
    NONE,
    PERMISSION_DIALOG,
    ANR_SYSTEM_DIALOG,
    SYSTEM_ALERT_DIALOG,
    GENERIC_DISMISSABLE_POPUP
}

data class InterruptionAssessment(
    val isInterrupted: Boolean,
    val type: InterruptionType,
    val dismissCommand: TaskCommand? = null,
    val description: String = ""
)

data class StateSignature(
    val hash: Long,
    val nodeCount: Int,
    val packageName: String
) {
    fun isStagnantWith(other: StateSignature?): Boolean {
        if (other == null) return false
        return this.hash == other.hash && this.packageName == other.packageName
    }
}

/**
 * Studied from Alibaba Mobile-Agent-v2 Reflection Unit:
 * Interruption Detector inspects the current UI hierarchy to identify blocking system
 * popups, permission controllers, or ANR prompts and generates corrective bypass commands.
 */
object InterruptionDetector {

    private val PERMISSION_PACKAGES = setOf(
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller"
    )

    private val ALLOW_BUTTON_TEXTS = listOf(
        "While using the app",
        "Only this time",
        "Allow",
        "GRANT",
        "Continue"
    )

    private val DISMISS_BUTTON_TEXTS = listOf(
        "Dismiss",
        "Cancel",
        "Not now",
        "Close app",
        "Wait",
        "OK",
        "Close"
    )

    fun assessSnapshot(snapshot: UiSnapshot?): InterruptionAssessment {
        if (snapshot == null) {
            return InterruptionAssessment(isInterrupted = false, type = InterruptionType.NONE)
        }

        val pkg = snapshot.packageName.lowercase()

        // 1. Android Permission Controller Interruption
        if (PERMISSION_PACKAGES.any { pkg.contains(it) }) {
            val allNodes = snapshot.allNodes
            val allowNode = allNodes.firstOrNull { node ->
                val txt = node.text?.trim() ?: ""
                ALLOW_BUTTON_TEXTS.any { txt.equals(it, ignoreCase = true) }
            }

            if (allowNode != null) {
                val buttonText = allowNode.text?.trim() ?: "Allow"
                return InterruptionAssessment(
                    isInterrupted = true,
                    type = InterruptionType.PERMISSION_DIALOG,
                    dismissCommand = TaskCommand.ClickElement(
                        selector = TargetSelector(text = buttonText, clickableOnly = false),
                        targetLabel = "Permission: '$buttonText'"
                    ),
                    description = "Detected Android permission request dialog in '$pkg'. Prepared grant/dismiss command."
                )
            }
        }

        // 2. Android ANR / System crash dialog
        if (pkg == "android" || pkg == "com.android.systemui") {
            val allNodes = snapshot.allNodes
            val isAnr = allNodes.any {
                it.text?.contains("isn't responding", ignoreCase = true) == true ||
                it.text?.contains("has stopped", ignoreCase = true) == true
            }

            if (isAnr) {
                val waitOrCloseNode = allNodes.firstOrNull { node ->
                    val txt = node.text?.trim() ?: ""
                    txt.equals("Wait", ignoreCase = true) || txt.equals("Close app", ignoreCase = true)
                }
                val btnText = waitOrCloseNode?.text?.trim() ?: "Wait"
                return InterruptionAssessment(
                    isInterrupted = true,
                    type = InterruptionType.ANR_SYSTEM_DIALOG,
                    dismissCommand = TaskCommand.ClickElement(
                        selector = TargetSelector(text = btnText),
                        targetLabel = "System Dialog: '$btnText'"
                    ),
                    description = "Detected System ANR or App Crash popup. Prepared dismissal."
                )
            }
        }

        // 3. Generic In-App Rating or Update Alert Popup
        val allNodes = snapshot.allNodes
        val dismissCandidate = allNodes.firstOrNull { node ->
            val txt = node.text?.trim() ?: ""
            node.isClickable && DISMISS_BUTTON_TEXTS.any { txt.equals(it, ignoreCase = true) }
        }

        // Only flag if the total screen is modal (few elements) or contains alert keywords
        val hasAlertKeywords = allNodes.any {
            val t = it.text?.lowercase() ?: ""
            t.contains("rate our app") || t.contains("update available") || t.contains("terms of service")
        }

        if (dismissCandidate != null && hasAlertKeywords) {
            val label = dismissCandidate.text?.trim() ?: "Dismiss"
            return InterruptionAssessment(
                isInterrupted = true,
                type = InterruptionType.GENERIC_DISMISSABLE_POPUP,
                dismissCommand = TaskCommand.ClickElement(
                    selector = TargetSelector(text = label),
                    targetLabel = "Dismiss Modal: '$label'"
                ),
                description = "Detected modal prompt ('$label'). Prepared dismissal."
            )
        }

        return InterruptionAssessment(isInterrupted = false, type = InterruptionType.NONE)
    }
}

/**
 * Studied from Google Research AndroidWorld & DroidBot:
 * Computes a fast 64-bit structural FNV-1a hash across the interactive UI nodes.
 * Used for detecting no-op actions and loop stagnation without memory allocations.
 */
object StateSignatureHasher {

    private const val FNV_64_INIT = -0x41329823cb45687fL
    private const val FNV_64_PRIME = 0x100000001b3L

    fun computeSignature(snapshot: UiSnapshot?): StateSignature {
        if (snapshot == null) {
            return StateSignature(hash = 0L, nodeCount = 0, packageName = "")
        }

        var hash = FNV_64_INIT
        val allNodes = snapshot.allNodes
        var interactiveCount = 0

        for (node in allNodes) {
            if (node.isClickable || node.isEditable || node.isScrollable || !node.text.isNullOrEmpty()) {
                interactiveCount++
                // Hash resourceId
                node.resourceId?.let { id ->
                    for (ch in id) {
                        hash = (hash xor ch.code.toLong()) * FNV_64_PRIME
                    }
                }
                // Hash text
                node.text?.let { txt ->
                    for (ch in txt) {
                        hash = (hash xor ch.code.toLong()) * FNV_64_PRIME
                    }
                }
                // Hash bounds
                hash = (hash xor (node.bounds.left.toLong() shl 16 or node.bounds.top.toLong())) * FNV_64_PRIME
                hash = (hash xor (node.bounds.right.toLong() shl 16 or node.bounds.bottom.toLong())) * FNV_64_PRIME
            }
        }

        return StateSignature(
            hash = hash,
            nodeCount = interactiveCount,
            packageName = snapshot.packageName
        )
    }
}
