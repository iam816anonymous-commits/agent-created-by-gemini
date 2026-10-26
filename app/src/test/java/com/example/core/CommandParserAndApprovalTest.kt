package com.example.core

import com.example.core.approval.ExecutionPlan
import com.example.core.approval.ProposedStep
import com.example.core.approval.RiskLevel
import com.example.core.approval.TaskApprovalGate
import com.example.core.model.TaskCommand
import com.example.core.parser.NaturalLanguageCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CommandParserAndApprovalTest {

    @Test
    fun testCommandParser_parsesCalculatorDirectiveWithoutAssumingSpecificVendor() {
        val parser = NaturalLanguageCommandParser()
        val commands = parser.parse("Open calculator and calculate 7 * 8")

        assertTrue("Should contain launch app", commands.any { it is TaskCommand.LaunchApp })
        val launch = commands.filterIsInstance<TaskCommand.LaunchApp>().first()
        assertEquals("Calculator", launch.appName)
        // Checks that fallbacks are populated for dynamic device resolution
        assertTrue("Fallback packages must be provided", launch.fallbackPackages.isNotEmpty())
    }

    @Test
    fun testCommandParser_parsesGenericOpenAppCommand() {
        val parser = NaturalLanguageCommandParser()
        val commands = parser.parse("Open Camera")

        val launch = commands.filterIsInstance<TaskCommand.LaunchApp>().firstOrNull()
        assertNotNull("Should parse launch command", launch)
        assertEquals("Camera", launch?.appName)
    }

    @Test
    fun testTaskApprovalGate_autoClassifiesRiskLevels() {
        val gate = TaskApprovalGate()

        // 1. Critical risk action: Delete
        val criticalPlan = gate.proposePlan(
            rawTask = "Delete all notes in Keep",
            resolvedPackage = "com.google.android.keep",
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Delete note item",
                    actionType = "CLICK",
                    target = "Delete permanently"
                )
            )
        )

        assertTrue("Critical action must require elevated confirmation", criticalPlan.requiresElevatedConfirmation)
        assertEquals(RiskLevel.CRITICAL, criticalPlan.steps[0].riskLevel)

        // 2. Low risk action: Read or view
        val lowRiskPlan = gate.proposePlan(
            rawTask = "View weather in morning briefing",
            resolvedPackage = null,
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Read forecast summary",
                    actionType = "OBSERVE",
                    target = "Weather widget"
                )
            )
        )

        assertFalse("Low risk action does not require elevated confirmation", lowRiskPlan.requiresElevatedConfirmation)
        assertEquals(RiskLevel.LOW, lowRiskPlan.steps[0].riskLevel)
    }

    @Test
    fun testTaskApprovalGate_approvalFlow() {
        val gate = TaskApprovalGate()
        val plan = gate.proposePlan(
            rawTask = "Open Settings",
            resolvedPackage = "com.android.settings",
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Launch Settings",
                    actionType = "LAUNCH",
                    target = "Settings"
                )
            )
        )

        assertFalse("Plan cannot execute before approval", gate.canExecute(plan.planId))

        val approved = gate.approvePlan(plan.planId)
        assertTrue(approved)
        assertTrue("Plan can execute after explicit approval", gate.canExecute(plan.planId))
    }


    @Test
    fun testTaskApprovalGate_consequentialPlanCannotExecuteUntilApproved() {
        val gate = TaskApprovalGate()
        val plan = gate.proposePlan(
            rawTask = "Transfer money to a recipient",
            resolvedPackage = "com.phonepe.app",
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Make payment",
                    actionType = "CLICK",
                    target = "Pay recipient"
                )
            )
        )

        assertTrue("Financial actions must require elevated confirmation", plan.requiresElevatedConfirmation)
        assertFalse("Consequential plan must remain blocked before explicit approval", gate.canExecute(plan.planId))
        assertTrue("Only explicit approval for this plan unlocks the gate", gate.approvePlan(plan.planId))
        assertTrue("Approved plan should pass the gate", gate.canExecute(plan.planId))
    }

    @Test
    fun testTaskApprovalGate_doesNotFalselyFlagShoppingAsPinCredential() {
        val gate = TaskApprovalGate()
        val plan = gate.proposePlan(
            rawTask = "Add shopping list to Keep notes",
            resolvedPackage = "com.google.android.keep",
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Type shopping items",
                    actionType = "TYPE",
                    target = "shopping_list_edit_text"
                )
            )
        )

        assertFalse("Shopping should not trigger elevated confirmation", plan.requiresElevatedConfirmation)
        assertEquals(RiskLevel.LOW, plan.steps[0].riskLevel)

        // Contrast with actual PIN change
        val pinPlan = gate.proposePlan(
            rawTask = "Change lockscreen PIN",
            resolvedPackage = "com.android.settings",
            steps = listOf(
                ProposedStep(
                    stepIndex = 1,
                    description = "Enter new PIN code",
                    actionType = "TYPE",
                    target = "pin_input_field"
                )
            )
        )
        assertTrue("Actual PIN change must trigger elevated confirmation", pinPlan.requiresElevatedConfirmation)
        assertEquals(RiskLevel.HIGH, pinPlan.steps[0].riskLevel)
    }
}
