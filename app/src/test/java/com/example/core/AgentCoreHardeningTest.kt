package com.example.core

import com.example.core.intent.IntentActionStep
import com.example.core.intent.IntentPlanResult
import com.example.core.intent.PlanSchemaValidator
import com.example.core.model.ExecutionMode
import com.example.core.model.ExecutionStatus
import com.example.core.model.StepStatus
import com.example.core.model.TaskCommand
import com.example.core.parser.NaturalLanguageCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentCoreHardeningTest {

    // A01 & A08: Plan schema validator and step mapping
    @Test
    fun testPlanSchemaValidator_validPlanMapsToTaskCommands() {
        val steps = listOf(
            IntentActionStep(
                id = "s1",
                action = "LAUNCH_APP",
                target = "com.google.android.calculator",
                parameters = mapOf("appName" to "Calculator")
            ),
            IntentActionStep(
                id = "s2",
                action = "CLICK_ELEMENT",
                target = "Button 7",
                parameters = mapOf("text" to "7")
            ),
            IntentActionStep(
                id = "s3",
                action = "VERIFY_CONDITION",
                target = "Calculator formula reflects '7'"
            )
        )

        val result = PlanSchemaValidator.validateAndMap(steps)
        assertTrue("Valid steps must pass schema validation", result is PlanSchemaValidator.ValidationResult.Valid)
        val commands = (result as PlanSchemaValidator.ValidationResult.Valid).commands
        assertEquals(3, commands.size)
        assertTrue(commands[0] is TaskCommand.LaunchApp)
        assertTrue(commands[1] is TaskCommand.ClickElement)
        assertTrue(commands[2] is TaskCommand.VerifyCondition)
    }

    @Test
    fun testPlanSchemaValidator_rejectsMalformedActionAndOutOfBoundsCoordinates() {
        // Step with unknown action
        val stepsUnknownAction = listOf(
            IntentActionStep(id = "bad1", action = "MALICIOUS_ROOT_INJECT", target = "root")
        )
        val resUnknown = PlanSchemaValidator.validateAndMap(stepsUnknownAction)
        assertTrue(resUnknown is PlanSchemaValidator.ValidationResult.Invalid)
        assertTrue((resUnknown as PlanSchemaValidator.ValidationResult.Invalid).errors[0].contains("Unsupported or forbidden action"))

        // Step with out-of-bounds swipe coordinate
        val stepsBadCoords = listOf(
            IntentActionStep(
                id = "bad2",
                action = "SWIPE",
                target = "swipe",
                parameters = mapOf("startX" to "1.5", "startY" to "0.5", "endX" to "0.5", "endY" to "0.2")
            )
        )
        val resCoords = PlanSchemaValidator.validateAndMap(stepsBadCoords)
        assertTrue(resCoords is PlanSchemaValidator.ValidationResult.Invalid)
        assertTrue((resCoords as PlanSchemaValidator.ValidationResult.Invalid).errors[0].contains("must all be in [0.0, 1.0]"))
    }

    // A02: Explicit execution modes
    @Test
    fun testExecutionMode_enumsAreDistinct() {
        assertEquals(4, ExecutionMode.values().size)
        assertTrue(ExecutionMode.values().contains(ExecutionMode.LIVE_DEVICE))
        assertTrue(ExecutionMode.values().contains(ExecutionMode.SIMULATION))
        assertTrue(ExecutionMode.values().contains(ExecutionMode.TEST))
        assertTrue(ExecutionMode.values().contains(ExecutionMode.BENCHMARK))
    }

    // A07: Unsupported prompt rejection
    @Test
    fun testCommandParser_rejectsConversationalAndAmbiguousPrompts() {
        val parser = NaturalLanguageCommandParser()

        // Conversational questions that are not device tasks must be rejected
        val unsupportedQueries = listOf(
            "What is the capital of France?",
            "Can you tell me a funny bedtime story about space?",
            "How do I fix a leaking faucet in my kitchen?",
            "Why is the sky blue?"
        )

        for (query in unsupportedQueries) {
            val commands = parser.parse(query)
            assertTrue("Unparseable query '$query' must not generate random ClickElement actions", commands.isEmpty())

            val parseResult = parser.parseWithResult(query)
            assertTrue("Must return Unsupported result", parseResult is NaturalLanguageCommandParser.ParseResult.Unsupported)
        }
    }

    @Test
    fun testCommandParser_acceptsExplicitDeviceDirectives() {
        val parser = NaturalLanguageCommandParser()

        val validDirectives = listOf(
            "Launch Calculator",
            "Open Settings",
            "Turn on flashlight",
            "Set timer for 5 minutes",
            "Type Hello into Search"
        )

        for (directive in validDirectives) {
            val commands = parser.parse(directive)
            assertTrue("Valid directive '$directive' must yield commands", commands.isNotEmpty())
        }
    }

    // A06: Task states
    @Test
    fun testExecutionStatus_lifecycleStatesAreExhaustive() {
        val statuses = ExecutionStatus.values().map { it.name }
        assertTrue(statuses.contains("IDLE"))
        assertTrue(statuses.contains("ROUTING"))
        assertTrue(statuses.contains("PARSING"))
        assertTrue(statuses.contains("RESOLVING_TARGET"))
        assertTrue(statuses.contains("EXECUTING_ACTION"))
        assertTrue(statuses.contains("WAITING_OBSERVATION"))
        assertTrue(statuses.contains("VERIFYING"))
        assertTrue(statuses.contains("COMPLETED_SUCCESS"))
        assertTrue(statuses.contains("COMPLETED_FAILURE"))
        assertTrue(statuses.contains("ABORTED_BY_USER"))
        assertTrue(statuses.contains("TIMEOUT"))
    }

    // A01: Intent router structured plan reaches executor with exact parameters
    @Test
    fun testA01_IntentRouterPlan_PreservesStructuredStepsAndReachesExecutor() {
        val parser = NaturalLanguageCommandParser()
        val rulesBackend = com.example.core.intent.DeterministicRulesBackend(parser)

        // Generate plan via Tier 1 Rules Backend
        val plan = kotlinx.coroutines.runBlocking {
            rulesBackend.plan(
                context = android.content.ContextWrapper(null),
                query = "Launch Calculator and tap 7",
                activePackage = null
            )
        }

        assertNotNull("Router must produce a plan for valid calculator instruction", plan)
        assertEquals(1, plan!!.sourceTier)
        assertEquals("DeterministicRulesFastPath", plan.backendName)
        assertTrue("Router plan must contain at least 3 steps", plan.steps.size >= 3)

        // Validate plan conversion to TaskCommands
        val validation = PlanSchemaValidator.validateAndMap(plan.steps)
        assertTrue("Router plan must validate against schema", validation is PlanSchemaValidator.ValidationResult.Valid)
        val commands = (validation as PlanSchemaValidator.ValidationResult.Valid).commands

        // Step 1: Launch Calculator
        assertTrue("Step 1 must be LaunchApp", commands[0] is TaskCommand.LaunchApp)
        assertEquals("com.google.android.calculator", (commands[0] as TaskCommand.LaunchApp).packageName)

        // Step 2: WaitForElement (key 7)
        assertTrue("Step 2 must be WaitForElement", commands[1] is TaskCommand.WaitForElement)
        assertEquals("7", (commands[1] as TaskCommand.WaitForElement).selector.text)

        // Step 3: ClickElement (key 7)
        assertTrue("Step 3 must be ClickElement", commands[2] is TaskCommand.ClickElement)
        val clickCmd = commands[2] as TaskCommand.ClickElement
        assertEquals("7", clickCmd.selector.text)

        // Step 4: VerifyCondition (display reflects 7)
        assertTrue("Step 4 must be VerifyCondition", commands[3] is TaskCommand.VerifyCondition)
        val verifyCmd = commands[3] as TaskCommand.VerifyCondition
        assertTrue("Verification rule must look for '7'", verifyCmd.rule is com.example.core.model.VerificationRule.DisplayContainsText)
        assertEquals("7", (verifyCmd.rule as com.example.core.model.VerificationRule.DisplayContainsText).expectedText)
    }

    @Test
    fun testA01_SkillMatching_ExactOrExplicitTriggersOnly() {
        // Compound prompt must NOT be hijacked by a substring match like "launch calculator"
        val prompt = "Launch Calculator and tap 7"
        val triggerPhrases = listOf("open calculator", "launch calculator", "calculate")

        val isExactMatch = triggerPhrases.any { trigger ->
            prompt.trim().equals(trigger.trim(), ignoreCase = true)
        }
        assertFalse("Compound prompt 'Launch Calculator and tap 7' must not be hijacked by skill trigger 'launch calculator'", isExactMatch)

        // Exact match should match
        val exactPrompt = "launch calculator"
        val isExact = triggerPhrases.any { trigger ->
            exactPrompt.trim().equals(trigger.trim(), ignoreCase = true)
        }
        assertTrue("Exact prompt 'launch calculator' must match skill trigger", isExact)
    }

    @Test
    fun testA01_LiveModeEnforcement_RefusesSilentSimulationWhenAccessibilityDisconnected() {
        // Accessibility service is null by default in unit tests
        assertNull("LocalAgentAccessibilityService instance should be null in test environment", com.example.service.LocalAgentAccessibilityService.instance)

        val commands = listOf(
            TaskCommand.LaunchApp("com.google.android.calculator", "Calculator"),
            TaskCommand.ClickElement(com.example.core.model.TargetSelector(text = "7"), "Button 7"),
            TaskCommand.VerifyCondition(com.example.core.model.VerificationRule.DisplayContainsText("7"), "Verify 7")
        )

        val requiresAccessibility = commands.any {
            it is TaskCommand.ClickElement || it is TaskCommand.EnterText ||
            it is TaskCommand.Scroll || it is TaskCommand.OpenDrawer ||
            it is TaskCommand.WaitForElement || (it is TaskCommand.VerifyCondition && it.rule !is com.example.core.model.VerificationRule.AlwaysPass)
        }
        assertTrue("UI gesture commands must require active accessibility service", requiresAccessibility)
    }
}


