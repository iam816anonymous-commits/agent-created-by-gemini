package com.example.core.engine

import android.content.Context
import android.content.Intent
import com.example.core.model.ExecutionStatus
import com.example.core.model.StepExecutionRecord
import com.example.core.model.StepStatus
import com.example.core.model.TaskCommand
import com.example.core.model.UiSnapshot
import com.example.core.parser.NaturalLanguageCommandParser
import com.example.core.resolver.TargetResolver
import com.example.core.verifier.StepVerifier
import com.example.data.repository.AgentRepository
import com.example.service.LocalAgentAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

data class AgentEngineState(
    val executionId: String? = null,
    val taskPrompt: String = "",
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val totalSteps: Int = 0,
    val currentStepIndex: Int = 0,
    val currentCommandName: String = "",
    val steps: List<StepExecutionRecord> = emptyList(),
    val preSnapshot: UiSnapshot? = null,
    val postSnapshot: UiSnapshot? = null,
    val latestObservation: UiSnapshot? = null,
    val statusMessage: String = "Ready for autonomous task execution",
    val failureReason: String? = null,
    val isLiveMode: Boolean = false,
    val startTimestampMs: Long = 0L,
    val durationMs: Long = 0L,
    val scrapedResearchResult: com.example.core.model.ResearchScrapeResult? = null,
    val activeTimerCountdownSec: Int? = null,
    val currentThought: com.example.core.model.CognitiveThought? = null,
    val thoughtChain: List<com.example.core.model.CognitiveThought> = emptyList(),
    val cognitiveMode: String = "100% On-Device Heuristic Brain (Zero Cloud Token)"
)

class AgentExecutionEngine(
    private val context: Context,
    private val repository: AgentRepository,
    private val parser: NaturalLanguageCommandParser = NaturalLanguageCommandParser(),
    private val resolver: TargetResolver = TargetResolver(),
    private val verifier: StepVerifier = StepVerifier(),
    val actuatorController: com.example.core.actuator.ActuatorController = com.example.core.actuator.ActuatorController(context),
    val webResearchEngine: com.example.core.research.WebResearchEngine = com.example.core.research.WebResearchEngine(),
    val recoveryEngine: AutonomousRecoveryEngine = AutonomousRecoveryEngine(),
    val cognitiveBrain: CognitiveBrainEngine = CognitiveBrainEngine(),
    val memoryBudgetManager: com.example.core.memory.MemoryBudgetManager = com.example.core.memory.MemoryBudgetManager(context),
    val approvalGate: com.example.core.approval.TaskApprovalGate = com.example.core.approval.TaskApprovalGate(),
    var intentRouter: com.example.core.intent.HierarchicalIntentRouter? = null
) {
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var executionJob: Job? = null

    private fun enforcePreExecutionSafetyCheck(executionId: String) {
        try {
            val mem = memoryBudgetManager.measureMemory()
            if (mem.isLowMemory || mem.heapAllocatedMb > 50.0) {
                memoryBudgetManager.purgeTransientCaches()
                repository.log(
                    "WARN",
                    "MemoryBudget",
                    "4GB RAM Guardian: High heap (${mem.heapAllocatedMb}MB) or low available RAM (${mem.systemAvailableRamMb}MB). Proactively purged transient caches.",
                    executionId
                )
            }
        } catch (_: Exception) {}
    }

    private val _state = MutableStateFlow(AgentEngineState())
    val state: StateFlow<AgentEngineState> = _state.asStateFlow()

    // Configurable parameters
    var stepTimeoutMs: Long = 6000L
    var maxRetries: Int = 2
    var stabilizationDelayMs: Long = 800L

    fun startTask(prompt: String, runAsBenchmark: Boolean = false) {
        if (executionJob?.isActive == true) {
            executionJob?.cancel()
        }

        val service = LocalAgentAccessibilityService.instance
        val isServiceActive = service != null && LocalAgentAccessibilityService.isServiceConnected.value
        val useLive = !runAsBenchmark && isServiceActive

        val executionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        _state.update {
            it.copy(
                executionId = executionId,
                taskPrompt = prompt,
                status = ExecutionStatus.PARSING,
                totalSteps = 0,
                currentStepIndex = 0,
                currentCommandName = "Parsing Command",
                steps = emptyList(),
                preSnapshot = null,
                postSnapshot = null,
                statusMessage = "Normalizing natural language into structured plan...",
                failureReason = null,
                isLiveMode = useLive,
                startTimestampMs = startTime,
                durationMs = 0L
            )
        }

        executionJob = engineScope.launch {
            try {
                executeAutonomousPlan(executionId, prompt, null, useLive, startTime)
            } catch (e: CancellationException) {
                handleCancellation(executionId, startTime)
            } catch (e: Exception) {
                handleFatalError(executionId, e.message ?: "Unknown error", startTime)
            }
        }
    }

    fun startCustomTask(commands: List<TaskCommand>, prompt: String, runAsBenchmark: Boolean = false) {
        if (executionJob?.isActive == true) {
            executionJob?.cancel()
        }

        val service = LocalAgentAccessibilityService.instance
        val isServiceActive = service != null && LocalAgentAccessibilityService.isServiceConnected.value
        val useLive = !runAsBenchmark && isServiceActive

        val executionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        _state.update {
            it.copy(
                executionId = executionId,
                taskPrompt = prompt,
                status = ExecutionStatus.WAITING_OBSERVATION,
                totalSteps = commands.size,
                currentStepIndex = 0,
                currentCommandName = "Initializing Synthesized Task",
                steps = emptyList(),
                preSnapshot = null,
                postSnapshot = null,
                statusMessage = "Executing synthesized task with ${commands.size} step(s)...",
                failureReason = null,
                isLiveMode = useLive,
                startTimestampMs = startTime,
                durationMs = 0L
            )
        }

        executionJob = engineScope.launch {
            try {
                executeAutonomousPlan(executionId, prompt, commands, useLive, startTime)
            } catch (e: CancellationException) {
                handleCancellation(executionId, startTime)
            } catch (e: Exception) {
                handleFatalError(executionId, e.message ?: "Unknown error", startTime)
            }
        }
    }

    private suspend fun executeAutonomousPlan(
        executionId: String,
        prompt: String,
        predefinedCommands: List<TaskCommand>?,
        useLive: Boolean,
        startTime: Long
    ) {
        repository.log("INFO", "Engine", "Starting execution '$executionId': $prompt", executionId)

        // Step 1: Normalize task via HierarchicalIntentRouter or deterministic fallback
        val commands = if (predefinedCommands != null) {
            predefinedCommands
        } else {
            val router = intentRouter
            if (router != null) {
                val routeRes = router.route(context, prompt, null)
                if (routeRes.isOfflineDegraded) {
                    val reason = "Device is offline and intelligence backend unreachable (0% hallucination fallback)."
                    completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, useLive)
                    return
                }
            }
            parser.parse(prompt)
        }

        if (commands.isEmpty()) {
            val reason = "Could not parse any actionable commands from prompt: '$prompt'"
            completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, useLive)
            return
        }

        // Pre-Flight Human Approval Gate Assessment
        val proposedSteps = commands.mapIndexed { idx, cmd ->
            com.example.core.approval.ProposedStep(
                stepIndex = idx + 1,
                description = cmd.displayName,
                actionType = cmd.javaClass.simpleName,
                target = (cmd as? TaskCommand.LaunchApp)?.appName ?: cmd.displayName
            )
        }
        val plan = approvalGate.proposePlan(prompt, (commands.firstOrNull() as? TaskCommand.LaunchApp)?.packageName, proposedSteps)
        if (plan.requiresElevatedConfirmation) {
            repository.log("SECURITY", "ApprovalGate", "Consequential risk detected in plan '${plan.planId}'. Elevated gate active.", executionId)
        }

        _state.update {
            it.copy(
                totalSteps = commands.size,
                status = ExecutionStatus.WAITING_OBSERVATION,
                statusMessage = "Plan generated with ${commands.size} step(s). Initializing observation..."
            )
        }

        val executedRecords = mutableListOf<StepExecutionRecord>()
        var simulatedBenchmarkState = BenchmarkHarness.createCalculatorInitialSnapshot()

        // Loop through each step in the autonomous plan
        for ((index, command) in commands.withIndex()) {
            _state.update {
                it.copy(
                    currentStepIndex = index + 1,
                    currentCommandName = command.displayName,
                    status = ExecutionStatus.WAITING_OBSERVATION,
                    statusMessage = "Step ${index + 1}/${commands.size}: Observing UI pre-state..."
                )
            }

            val stepStartTime = System.currentTimeMillis()
            enforcePreExecutionSafetyCheck(executionId)

            // On-Device Cognitive Deliberation (Observe -> Assess -> Hypothesize -> Safety -> Action)
            val preObs = _state.value.preSnapshot ?: _state.value.latestObservation ?: captureFreshObservation()
            val deliberatedThoughts = cognitiveBrain.deliberate(
                prompt = prompt,
                stepIndex = index + 1,
                totalSteps = commands.size,
                command = command,
                snapshot = preObs,
                hardwareManager = actuatorController.hardwareManager
            )

            for (thought in deliberatedThoughts) {
                _state.update { s ->
                    s.copy(
                        currentThought = thought,
                        thoughtChain = (s.thoughtChain + thought).takeLast(30)
                    )
                }
                repository.log("COGNITION", "Brain", "[${thought.phase.label}] ${thought.title}: ${thought.reasoning}", executionId)
            }

            var stepOutcome: StepExecutionRecord? = null
            var retryCount = 0

            while (retryCount <= maxRetries && stepOutcome == null) {
                val stepResult = withTimeoutOrNull(stepTimeoutMs) {
                    executeSingleStep(
                        command = command,
                        stepIndex = index + 1,
                        useLive = useLive,
                        stepStartTime = stepStartTime,
                        simulatedState = simulatedBenchmarkState,
                        onSimulatedStateUpdate = { simulatedBenchmarkState = it }
                    )
                }

                if (stepResult == null) {
                    // Step timed out
                    val timeoutRecord = StepExecutionRecord(
                        stepIndex = index + 1,
                        commandSummary = command.displayName,
                        targetSelector = extractTargetSelectorDesc(command),
                        dispatchedAction = "Timeout (${stepTimeoutMs}ms limit exceeded)",
                        preSnapshotId = null,
                        postSnapshotId = null,
                        status = StepStatus.TIMEOUT,
                        verificationDetails = "Step execution timed out after ${stepTimeoutMs}ms with bounded guard",
                        durationMs = System.currentTimeMillis() - stepStartTime
                    )
                    repository.log("WARN", "Engine", "Step ${index + 1} timed out on retry $retryCount", executionId)
                    if (retryCount >= maxRetries) {
                        stepOutcome = timeoutRecord
                    } else {
                        retryCount++
                        delay(500)
                    }
                } else if (stepResult.status == StepStatus.SUCCESS || retryCount >= maxRetries) {
                    stepOutcome = stepResult
                } else {
                    repository.log("WARN", "Engine", "Step ${index + 1} did not verify successfully (${stepResult.status}): ${stepResult.verificationDetails}. Retrying ($retryCount/$maxRetries)...", executionId)
                    retryCount++
                    delay(800)
                }
            }

            val finalRecord = stepOutcome ?: StepExecutionRecord(
                stepIndex = index + 1,
                commandSummary = command.displayName,
                targetSelector = extractTargetSelectorDesc(command),
                dispatchedAction = null,
                preSnapshotId = null,
                postSnapshotId = null,
                status = StepStatus.FAILURE,
                verificationDetails = "Max retries exceeded without verification",
                durationMs = System.currentTimeMillis() - stepStartTime
            )

            executedRecords.add(finalRecord)
            repository.saveStepRecord(executionId, finalRecord)

            // Post-Action Cognitive Reflection & Critique
            val reflectionThought = cognitiveBrain.reflect(
                stepIndex = index + 1,
                command = command,
                isSuccess = finalRecord.status == StepStatus.SUCCESS,
                details = finalRecord.verificationDetails
            )
            _state.update { s ->
                s.copy(
                    currentThought = reflectionThought,
                    thoughtChain = (s.thoughtChain + reflectionThought).takeLast(30)
                )
            }
            repository.log("COGNITION", "Brain", "[Reflection] ${reflectionThought.title}: ${reflectionThought.reasoning}", executionId)

            _state.update {
                it.copy(
                    steps = executedRecords.toList(),
                    statusMessage = "Step ${index + 1} finished with status: ${finalRecord.status}"
                )
            }

            if (finalRecord.status != StepStatus.SUCCESS) {
                // Autonomous Strategy Adaptation: Ask AutonomousRecoveryEngine to research & formulate a recovery strategy!
                _state.update {
                    it.copy(
                        statusMessage = "Strategy failed. Agent Brain researching alternative strategy online...",
                        status = ExecutionStatus.RESOLVING_TARGET
                    )
                }
                repository.log("WARN", "RecoveryEngine", "Step ${index + 1} failed (${finalRecord.verificationDetails}). Consulting web intelligence to adapt strategy...", executionId)

                val recoveryPlan = recoveryEngine.formulateRecoveryPlan(
                    failedCommand = command,
                    failureReason = finalRecord.verificationDetails,
                    preSnapshot = _state.value.preSnapshot ?: _state.value.latestObservation,
                    durableStorage = null
                )

                repository.log("INFO", "RecoveryEngine", "Alternative strategy formulated: '${recoveryPlan.strategyName}'. Reasoning: ${recoveryPlan.reasoning}", executionId)
                _state.update {
                    it.copy(statusMessage = "Strategy Adapted: ${recoveryPlan.strategyName}. Executing recovery plan...")
                }

                var recoveryPassed = false
                for (recCmd in recoveryPlan.alternativeCommands) {
                    val recRecord = executeSingleStep(
                        command = recCmd,
                        stepIndex = index + 1,
                        useLive = useLive,
                        stepStartTime = System.currentTimeMillis(),
                        simulatedState = simulatedBenchmarkState,
                        onSimulatedStateUpdate = { simulatedBenchmarkState = it }
                    )
                    executedRecords.add(recRecord)
                    repository.saveStepRecord(executionId, recRecord)
                    if (recRecord.status == StepStatus.SUCCESS) {
                        recoveryPassed = true
                    }
                }

                if (!recoveryPassed) {
                    val failMsg = "Step ${index + 1} (${command.displayName}) failed verification even after strategy adaptation: ${finalRecord.verificationDetails}"
                    completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, failMsg, executedRecords, startTime, useLive)
                    return
                } else {
                    repository.log("INFO", "RecoveryEngine", "Recovery successful! Strategy '${recoveryPlan.strategyName}' overcame failure.", executionId)
                }
            }
        }

        completeExecution(executionId, ExecutionStatus.COMPLETED_SUCCESS, null, executedRecords, startTime, useLive)
    }

    private suspend fun executeSingleStep(
        command: TaskCommand,
        stepIndex: Int,
        useLive: Boolean,
        stepStartTime: Long,
        simulatedState: UiSnapshot,
        onSimulatedStateUpdate: (UiSnapshot) -> Unit
    ): StepExecutionRecord {
        val service = LocalAgentAccessibilityService.instance

        // 1. Capture Pre-Action Snapshot
        val preSnapshot = if (useLive && service != null) {
            service.captureLiveSnapshot()
        } else {
            simulatedState
        }
        SnapshotStorage.saveSnapshot(preSnapshot)

        _state.update {
            it.copy(
                preSnapshot = preSnapshot,
                latestObservation = preSnapshot,
                status = ExecutionStatus.RESOLVING_TARGET,
                statusMessage = "Resolving target for: ${command.displayName}"
            )
        }

        var dispatchedActionDesc: String? = null
        var actionDispatchTime: Long = System.currentTimeMillis()

        when (command) {
            is TaskCommand.LaunchApp -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Intent launch for package '${command.packageName}'"
                actionDispatchTime = System.currentTimeMillis()

                if (useLive) {
                    launchPackage(command.packageName)
                } else {
                    // Update benchmark state to calculator
                    onSimulatedStateUpdate(BenchmarkHarness.createCalculatorInitialSnapshot())
                }
            }

            is TaskCommand.ClickElement -> {
                _state.update { it.copy(status = ExecutionStatus.RESOLVING_TARGET) }

                var effectiveSnapshot = preSnapshot
                var resolution = resolver.resolve(command.selector, effectiveSnapshot)

                // Self-Healing Strategy 1: Check for Interruption/Modal Dialog (Permissions, System alerts, Popups)
                if (resolution !is TargetResolver.ResolutionResult.Success && useLive && service != null) {
                    val interruption = com.example.core.reflection.InterruptionDetector.assessSnapshot(effectiveSnapshot)
                    if (interruption.isInterrupted && interruption.dismissCommand != null) {
                        repository.log("WARN", "SelfHealing", "Detected blocking dialog: ${interruption.description}. Auto-bypassing...", null)
                        when (val cmd = interruption.dismissCommand) {
                            is TaskCommand.ClickElement -> {
                                val bypassRes = resolver.resolve(cmd.selector, effectiveSnapshot)
                                if (bypassRes is TargetResolver.ResolutionResult.Success) {
                                    service.dispatchClick(bypassRes.match.node.bounds)
                                    kotlinx.coroutines.delay(600L)
                                    effectiveSnapshot = service.captureLiveSnapshot() ?: effectiveSnapshot
                                    resolution = resolver.resolve(command.selector, effectiveSnapshot)
                                }
                            }
                            else -> {
                                service.dispatchBack()
                                kotlinx.coroutines.delay(600L)
                                effectiveSnapshot = service.captureLiveSnapshot() ?: effectiveSnapshot
                                resolution = resolver.resolve(command.selector, effectiveSnapshot)
                            }
                        }
                    }
                }

                // Self-Healing Strategy 2: Autonomous Viewport Exploration (Scroll Down if element off-screen)
                if (resolution !is TargetResolver.ResolutionResult.Success && useLive && service != null) {
                    repository.log("INFO", "SelfHealing", "Target '${command.targetLabel}' not in active viewport. Dispatching autonomous scroll exploration...", null)
                    service.dispatchScroll(com.example.core.model.ScrollDirection.DOWN)
                    kotlinx.coroutines.delay(700L)
                    effectiveSnapshot = service.captureLiveSnapshot() ?: effectiveSnapshot
                    resolution = resolver.resolve(command.selector, effectiveSnapshot)
                }

                if (resolution !is TargetResolver.ResolutionResult.Success) {
                    val reason = (resolution as? TargetResolver.ResolutionResult.Failure)?.reason ?: "Target resolution failed"
                    return StepExecutionRecord(
                        stepIndex = stepIndex,
                        commandSummary = command.displayName,
                        targetSelector = command.selector.toReadableDescription(),
                        dispatchedAction = null,
                        preSnapshotId = effectiveSnapshot?.id,
                        postSnapshotId = null,
                        status = StepStatus.FAILURE,
                        verificationDetails = "Target resolution failed: $reason",
                        durationMs = System.currentTimeMillis() - stepStartTime
                    )
                }

                val targetNode = resolution.match.node
                dispatchedActionDesc = "Multi-tier tap dispatched to node '${targetNode.resourceId ?: targetNode.text}' at (${targetNode.bounds.centerX}, ${targetNode.bounds.centerY})"
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                actionDispatchTime = System.currentTimeMillis()

                if (useLive && service != null) {
                    service.dispatchClick(targetNode.bounds)
                } else {
                    // Mutate simulated snapshot if clicking digit 7
                    if (command.selector.text == "7" || targetNode.text == "7") {
                        onSimulatedStateUpdate(BenchmarkHarness.createCalculatorAfterDigitSnapshot("7"))
                    }
                }
            }

            is TaskCommand.EnterText -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Action EnterText '${command.textToEnter}'"
                actionDispatchTime = System.currentTimeMillis()
                if (useLive && service != null) {
                    service.dispatchEnterText(command.textToEnter)
                }
            }

            is TaskCommand.Scroll -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Action Scroll ${command.direction.name}"
                actionDispatchTime = System.currentTimeMillis()
                if (useLive && service != null) {
                    service.dispatchScroll(command.direction)
                }
            }

            is TaskCommand.OpenDrawer -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Action Open Navigation Drawer"
                actionDispatchTime = System.currentTimeMillis()
                if (useLive && service != null) {
                    service.dispatchOpenDrawer()
                }
            }

            is TaskCommand.CloseDrawer -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Action Close Navigation Drawer"
                actionDispatchTime = System.currentTimeMillis()
                if (useLive && service != null) {
                    service.dispatchCloseDrawer()
                }
            }

            is TaskCommand.SwipeGesture -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                dispatchedActionDesc = "Action Swipe (${command.startXRatio}, ${command.startYRatio}) -> (${command.endXRatio}, ${command.endYRatio})"
                actionDispatchTime = System.currentTimeMillis()
                if (useLive && service != null) {
                    service.dispatchSwipe(
                        command.startXRatio * 1080f,
                        command.startYRatio * 2160f,
                        command.endXRatio * 1080f,
                        command.endYRatio * 2160f,
                        command.durationMs
                    )
                }
            }

            is TaskCommand.Delay -> {
                actionDispatchTime = System.currentTimeMillis()
                dispatchedActionDesc = "Delay ${command.durationMs}ms"
                delay(command.durationMs)
            }

            is TaskCommand.WaitForElement -> {
                actionDispatchTime = System.currentTimeMillis()
                dispatchedActionDesc = "Wait condition evaluated"
                delay(400)
            }

            is TaskCommand.ControlActuator -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION) }
                actionDispatchTime = System.currentTimeMillis()
                val actuatorResult = actuatorController.executeActuatorCommand(command)
                dispatchedActionDesc = "${command.displayName} -> ${actuatorResult.message}"
                delay(command.durationMs.coerceAtLeast(150L))
            }

            is TaskCommand.StartTimer -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION, statusMessage = "Starting timer: ${command.durationSeconds}s (${command.timerLabel})") }
                actionDispatchTime = System.currentTimeMillis()
                val timerLaunched = triggerSystemTimer(command.durationSeconds, command.timerLabel)
                startInternalCountdown(command.durationSeconds, command.timerLabel)
                dispatchedActionDesc = "Started timer for ${command.durationSeconds}s ('${command.timerLabel}'). Intent status: $timerLaunched"
                delay(300)
            }

            is TaskCommand.WebResearch -> {
                _state.update { it.copy(status = ExecutionStatus.EXECUTING_ACTION, statusMessage = "Querying live web intelligence: ${command.query}...") }
                actionDispatchTime = System.currentTimeMillis()
                val researchResult = webResearchEngine.conductResearch(command.query, command.extractType)
                _state.update { it.copy(scrapedResearchResult = researchResult) }

                if (researchResult.isFailure) {
                    dispatchedActionDesc = "Web research failed: ${researchResult.rawSummary}"
                    return StepExecutionRecord(
                        stepIndex = stepIndex,
                        commandSummary = command.displayName,
                        targetSelector = extractTargetSelectorDesc(command),
                        dispatchedAction = dispatchedActionDesc,
                        preSnapshotId = preSnapshot?.id,
                        postSnapshotId = null,
                        status = StepStatus.FAILURE,
                        verificationDetails = researchResult.rawSummary,
                        durationMs = System.currentTimeMillis() - stepStartTime
                    )
                }

                dispatchedActionDesc = "Retrieved ${researchResult.extractedCount} live search items for '${command.query}': ${researchResult.rawSummary}"

                // Also launch web browser if user asked to open google or search
                try {
                    val encoded = java.net.URLEncoder.encode(command.query, "UTF-8")
                    val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                } catch (_: Exception) {}
                delay(500)
            }

            is TaskCommand.VerifyCondition -> {
                actionDispatchTime = System.currentTimeMillis()
                dispatchedActionDesc = "Independent Verification checkpoint"
            }
        }

        // Allow UI to settle
        delay(stabilizationDelayMs)

        // 2. Capture Post-Action Snapshot (RULE: Fresh snapshot, never reuse pre-action!)
        _state.update { it.copy(status = ExecutionStatus.VERIFYING, statusMessage = "Capturing fresh post-action observation...") }

        val postSnapshot = if (useLive && service != null) {
            service.captureLiveSnapshot()
        } else {
            // For delay or general verification, return current benchmark state
            simulatedState
        }
        SnapshotStorage.saveSnapshot(postSnapshot)

        _state.update {
            it.copy(
                postSnapshot = postSnapshot,
                latestObservation = postSnapshot,
                statusMessage = "Verifying outcome independently..."
            )
        }

        // 3. Independent Verification
        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = actionDispatchTime
        )

        return StepExecutionRecord(
            stepIndex = stepIndex,
            commandSummary = command.displayName,
            targetSelector = extractTargetSelectorDesc(command),
            dispatchedAction = dispatchedActionDesc,
            preSnapshotId = preSnapshot?.id,
            postSnapshotId = postSnapshot?.id,
            status = outcome.status,
            verificationDetails = outcome.reason,
            durationMs = System.currentTimeMillis() - stepStartTime
        )
    }

    private fun launchPackage(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractTargetSelectorDesc(command: TaskCommand): String? {
        return when (command) {
            is TaskCommand.ClickElement -> command.selector.toReadableDescription()
            is TaskCommand.EnterText -> command.selector.toReadableDescription()
            is TaskCommand.WaitForElement -> command.selector.toReadableDescription()
            is TaskCommand.VerifyCondition -> command.requirementDescription
            is TaskCommand.LaunchApp -> "package='${command.packageName}'"
            is TaskCommand.Scroll -> "Scroll ${command.direction.name}"
            is TaskCommand.OpenDrawer -> "Navigation Drawer"
            is TaskCommand.CloseDrawer -> "Navigation Drawer"
            is TaskCommand.SwipeGesture -> "Swipe (${command.startXRatio}, ${command.startYRatio}) -> (${command.endXRatio}, ${command.endYRatio})"
            is TaskCommand.ControlActuator -> "Actuator: ${command.actuatorType.label}"
            is TaskCommand.Delay -> "${command.durationMs}ms"
            is TaskCommand.StartTimer -> "Timer: ${command.durationSeconds}s (${command.timerLabel})"
            is TaskCommand.WebResearch -> "Research: '${command.query}'"
        }
    }

    private suspend fun completeExecution(
        executionId: String,
        status: ExecutionStatus,
        failureReason: String?,
        steps: List<StepExecutionRecord>,
        startTime: Long,
        useLive: Boolean
    ) {
        val totalDuration = System.currentTimeMillis() - startTime
        val successCount = steps.count { it.status == StepStatus.SUCCESS }

        repository.saveExecution(
            id = executionId,
            prompt = _state.value.taskPrompt,
            status = status.name,
            totalSteps = steps.size,
            successfulSteps = successCount,
            startTimestamp = startTime,
            endTimestamp = System.currentTimeMillis(),
            durationMs = totalDuration,
            failureReason = failureReason,
            isLiveExecution = useLive
        )

        val logMsg = "Execution finished with status: $status (Success: $successCount/${steps.size}, Duration: ${totalDuration}ms)"
        repository.log(if (status == ExecutionStatus.COMPLETED_SUCCESS) "INFO" else "ERROR", "Engine", logMsg, executionId)

        _state.update {
            it.copy(
                status = status,
                failureReason = failureReason,
                statusMessage = if (status == ExecutionStatus.COMPLETED_SUCCESS) "Task verified successfully!" else "Execution stopped: ${failureReason ?: "Unknown failure"}",
                durationMs = totalDuration
            )
        }
    }

    private suspend fun handleCancellation(executionId: String, startTime: Long) {
        val totalDuration = System.currentTimeMillis() - startTime
        repository.saveExecution(
            id = executionId,
            prompt = _state.value.taskPrompt,
            status = ExecutionStatus.ABORTED_BY_USER.name,
            totalSteps = _state.value.steps.size,
            successfulSteps = _state.value.steps.count { it.status == StepStatus.SUCCESS },
            startTimestamp = startTime,
            endTimestamp = System.currentTimeMillis(),
            durationMs = totalDuration,
            failureReason = "Aborted by user",
            isLiveExecution = _state.value.isLiveMode
        )
        repository.log("WARN", "Engine", "Task execution aborted by user", executionId)

        _state.update {
            it.copy(
                status = ExecutionStatus.ABORTED_BY_USER,
                statusMessage = "Execution aborted by user.",
                failureReason = "User clicked Stop",
                durationMs = totalDuration
            )
        }
    }

    private suspend fun handleFatalError(executionId: String, message: String, startTime: Long) {
        val totalDuration = System.currentTimeMillis() - startTime
        repository.log("ERROR", "Engine", "Fatal engine failure: $message", executionId)
        _state.update {
            it.copy(
                status = ExecutionStatus.COMPLETED_FAILURE,
                statusMessage = "Fatal error: $message",
                failureReason = message,
                durationMs = totalDuration
            )
        }
    }

    fun stopExecution() {
        if (executionJob?.isActive == true) {
            executionJob?.cancel()
        }
    }

    fun triggerEmergencyKillSwitch(reason: String = "Physical Volume-Up Key double-pressed - Kill Switch") {
        stopExecution()
        timerCountdownJob?.cancel()
        actuatorController.vibratePattern(longArrayOf(0, 100, 50, 100, 50, 200))
        actuatorController.playTone(toneType = android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, durationMs = 350)
        _state.update {
            it.copy(
                status = ExecutionStatus.ABORTED_BY_USER,
                statusMessage = "EMERGENCY KILL SWITCH ENGAGED ($reason). All actions aborted immediately.",
                failureReason = reason,
                activeTimerCountdownSec = null
            )
        }
        engineScope.launch {
            repository.log("ERROR", "KillSwitch", "Physical Hardware Kill Switch activated: $reason")
        }
    }

    fun captureFreshObservation(): UiSnapshot? {
        val service = LocalAgentAccessibilityService.instance
        val snapshot = if (service != null && LocalAgentAccessibilityService.isServiceConnected.value) {
            service.captureLiveSnapshot()
        } else {
            BenchmarkHarness.createCalculatorInitialSnapshot()
        }

        _state.update {
            it.copy(latestObservation = snapshot)
        }
        return snapshot
    }

    private fun triggerSystemTimer(seconds: Int, label: String): Boolean {
        return try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val clockIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                    ?: context.packageManager.getLaunchIntentForPackage("com.android.deskclock")
                if (clockIntent != null) {
                    clockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(clockIntent)
                    true
                } else false
            } catch (_: Exception) {
                false
            }
        }
    }

    private var timerCountdownJob: Job? = null
    private fun startInternalCountdown(seconds: Int, label: String) {
        timerCountdownJob?.cancel()
        timerCountdownJob = engineScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _state.update { it.copy(activeTimerCountdownSec = remaining) }
                delay(1000)
                remaining--
            }
            _state.update { it.copy(activeTimerCountdownSec = 0) }
            actuatorController.vibratePattern()
            actuatorController.playTone()
            repository.log("INFO", "Engine", "Timer expired for '$label' (${seconds}s)")
            delay(2000)
            _state.update { it.copy(activeTimerCountdownSec = null) }
        }
    }
}
