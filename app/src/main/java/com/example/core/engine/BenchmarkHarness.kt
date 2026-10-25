package com.example.core.engine

import com.example.core.model.RectData
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import java.util.UUID

object BenchmarkHarness {

    fun createCalculatorInitialSnapshot(): UiSnapshot {
        val root = UiNode(
            id = "root_calc",
            packageName = "com.google.android.calculator",
            className = "android.widget.FrameLayout",
            text = null,
            contentDescription = "Calculator",
            resourceId = "com.google.android.calculator:id/main_calculator",
            bounds = RectData(0, 0, 1080, 2160),
            isClickable = false,
            isEditable = false,
            isEnabled = true,
            isFocused = false,
            isVisibleToUser = true,
            depth = 0,
            children = listOf(
                // Formula display
                UiNode(
                    id = "calc_display_container",
                    packageName = "com.google.android.calculator",
                    className = "android.widget.LinearLayout",
                    text = null,
                    contentDescription = null,
                    resourceId = "com.google.android.calculator:id/display_container",
                    bounds = RectData(48, 120, 1032, 600),
                    isClickable = false,
                    isEditable = false,
                    isEnabled = true,
                    isFocused = false,
                    isVisibleToUser = true,
                    depth = 1,
                    children = listOf(
                        UiNode(
                            id = "formula_view",
                            packageName = "com.google.android.calculator",
                            className = "android.widget.TextView",
                            text = "",
                            contentDescription = "Formula empty",
                            resourceId = "com.google.android.calculator:id/formula",
                            bounds = RectData(72, 200, 1008, 400),
                            isClickable = false,
                            isEditable = false,
                            isEnabled = true,
                            isFocused = false,
                            isVisibleToUser = true,
                            depth = 2
                        ),
                        UiNode(
                            id = "result_view",
                            packageName = "com.google.android.calculator",
                            className = "android.widget.TextView",
                            text = "0",
                            contentDescription = "Current result 0",
                            resourceId = "com.google.android.calculator:id/result_final",
                            bounds = RectData(72, 420, 1008, 560),
                            isClickable = false,
                            isEditable = false,
                            isEnabled = true,
                            isFocused = false,
                            isVisibleToUser = true,
                            depth = 2
                        )
                    )
                ),
                // Keypad
                UiNode(
                    id = "keypad_pad",
                    packageName = "com.google.android.calculator",
                    className = "android.widget.GridLayout",
                    text = null,
                    contentDescription = "Keypad",
                    resourceId = "com.google.android.calculator:id/pad_numeric",
                    bounds = RectData(48, 650, 1032, 2000),
                    isClickable = false,
                    isLongClickable = false,
                    isScrollable = true,
                    isEditable = false,
                    isEnabled = true,
                    isFocused = false,
                    isVisibleToUser = true,
                    depth = 1,
                    children = createCalculatorButtons()
                )
            )
        )

        return UiSnapshot(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.calculator",
            windowTitle = "Calculator",
            rootNode = root,
            totalNodeCount = root.allDescendants.size,
            isLive = false,
            sourceDescription = "Diagnostic Benchmark Environment (TECNO IN6 Profile)"
        )
    }

    fun createCalculatorAfterDigitSnapshot(digit: String): UiSnapshot {
        val initial = createCalculatorInitialSnapshot()
        // Mutate formula display to contain the digit
        val updatedChildren = initial.rootNode!!.children.map { child ->
            if (child.id == "calc_display_container") {
                val updatedDisplayChildren = child.children.map { dispChild ->
                    if (dispChild.id == "formula_view") {
                        dispChild.copy(
                            text = digit,
                            contentDescription = "Formula contains $digit"
                        )
                    } else dispChild
                }
                child.copy(children = updatedDisplayChildren)
            } else child
        }

        val updatedRoot = initial.rootNode.copy(children = updatedChildren)
        return UiSnapshot(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.calculator",
            windowTitle = "Calculator",
            rootNode = updatedRoot,
            totalNodeCount = updatedRoot.allDescendants.size,
            isLive = false,
            sourceDescription = "Diagnostic Benchmark Environment (Post-Action Observation)"
        )
    }

    private fun createCalculatorButtons(): List<UiNode> {
        val buttonDefs = listOf(
            Triple("7", "digit_7", RectData(72, 700, 280, 920)),
            Triple("8", "digit_8", RectData(310, 700, 520, 920)),
            Triple("9", "digit_9", RectData(550, 700, 760, 920)),
            Triple("÷", "op_div", RectData(790, 700, 1000, 920)),
            Triple("4", "digit_4", RectData(72, 950, 280, 1170)),
            Triple("5", "digit_5", RectData(310, 950, 520, 1170)),
            Triple("6", "digit_6", RectData(550, 950, 760, 1170)),
            Triple("×", "op_mul", RectData(790, 950, 1000, 1170)),
            Triple("1", "digit_1", RectData(72, 1200, 280, 1420)),
            Triple("2", "digit_2", RectData(310, 1200, 520, 1420)),
            Triple("3", "digit_3", RectData(550, 1200, 760, 1420)),
            Triple("-", "op_sub", RectData(790, 1200, 1000, 1420)),
            Triple("0", "digit_0", RectData(72, 1450, 280, 1670)),
            Triple(".", "dec_point", RectData(310, 1450, 520, 1670)),
            Triple("=", "eq", RectData(550, 1450, 760, 1670)),
            Triple("+", "op_add", RectData(790, 1450, 1000, 1670))
        )

        return buttonDefs.map { (label, idSuffix, bounds) ->
            UiNode(
                id = "btn_$idSuffix",
                packageName = "com.google.android.calculator",
                className = "android.widget.Button",
                text = label,
                contentDescription = "Button $label",
                resourceId = "com.google.android.calculator:id/$idSuffix",
                bounds = bounds,
                isClickable = true,
                isLongClickable = (label == "0" || label == "="),
                isScrollable = false,
                isEditable = false,
                isEnabled = true,
                isFocused = false,
                isVisibleToUser = true,
                depth = 2
            )
        }
    }
}
