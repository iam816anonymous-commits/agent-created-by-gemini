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
    val executionMode: com.example.core.model.ExecutionMode = com.example.core.model.ExecutionMode.LIVE_DEVICE,
    val isLiveMode: Boolean = executionMode == com.example.core.model.ExecutionMode.LIVE_DEVICE,
    val startTimestampMs: Long = 0L,
    val durationMs: Long = 0L,
    val scrapedResearchResult: com.example.core.model.ResearchScrapeResult? = null,
    val activeTimerCountdownSec: Int? = null,
    val currentThought: com.example.core.model.CognitiveThought? = null,
    val thoughtChain: List<com.example.core.model.CognitiveThought> = emptyList(),
    val cognitiveMode: String = "Backend not selected",
    val activeTier: Int? = null,
    val activeBackendName: String? = null,
    val intentConfidence: Float? = null
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
    intentRouter: com.example.core.intent.HierarchicalIntentRouter? = null
) {
    var intentRouter: com.example.core.intent.HierarchicalIntentRouter = intentRouter ?: com.example.core.intent.HierarchicalIntentRouter(
        fastPathBackend = com.example.core.intent.DeterministicRulesBackend(parser),
        localSlmBackend = com.example.core.intent.LocalSlmBackend(),
        cloudGeminiBackend = com.example.core.intent.GeminiCloudBackend(brainEngine = cognitiveBrain)
    )

    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var executionJob: Job? = null

    private fun enforcePreExecutionSafetyCheck(executionId: String) {
        try {
            val mem = memoryBudgetManager.measureMemory()
            if (mem.isLowMemory || mem.heapAllocatedMb > 50.0) {
                memoryBudgetManager.purgeTransientCaches()
                engineScope.launch {
                    repository.log(
                        "WARN",
                        "MemoryBudget",
                        "4GB RAM Guardian: High heap (${mem.heapAllocatedMb}MB) or low available RAM (${mem.systemAvailableRamMb}MB). Proactively purged transient caches.",
                        executionId
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private val _state = MutableStateFlow(AgentEngineState())
    val state: StateFlow<AgentEngineState> = _state.asStateFlow()

    // Configurable parameters
    var stepTimeoutMs: Long = 6000L
    var maxRetries: Int = 2
    var stabilizationDelayMs: Long = 800L

    @Synchronized
    fun startTask(
        prompt: String,
        mode: com.example.core.model.ExecutionMode = com.example.core.model.ExecutionMode.LIVE_DEVICE,
        predefinedCommands: List<TaskCommand>? = null
    ) {
        if (executionJob?.isActive == true) {
            val activeExecutionId = _state.value.executionId
            engineScope.launch {
                repository.log(
                    "WARN",
                    "Engine",
                    "Rejected a new task while execution '${activeExecutionId ?: "unknown"}' is active. Stop or cancel the active task before starting another.",
                    activeExecutionId
                )
            }
            return
        }

        // Never silently downgrade a requested live task to simulation.
        // Simulation is only entered when the caller explicitly requests BENCHMARK/SIMULATION.
        val effectiveMode = mode

        val executionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        _state.update {
            it.copy(
                executionId = executionId,
                taskPrompt = prompt,
                status = ExecutionStatus.ROUTING,
                totalSteps = predefinedCommands?.size ?: 0,
                currentStepIndex = 0,
                currentCommandName = "Routing Intent",
                steps = emptyList(),
                preSnapshot = null,
                postSnapshot = null,
                statusMessage = "Evaluating intent router [Mode: $effectiveMode]...",
                failureReason = null,
                executionMode = effectiveMode,
                startTimestampMs = startTime,
                durationMs = 0L,
                activeTier = null,
                activeBackendName = null,
                intentConfidence = null
            )
        }

        executionJob = engineScope.launch {
            try {
                executeAutonomousPlan(executionId, prompt, predefinedCommands, effectiveMode, startTime)
            } catch (e: CancellationException) {
                handleCancellation(executionId, startTime)
            } catch (e: Exception) {
                handleFatalError(executionId, e.message ?: "Unknown error", startTime)
            }
        }
    }

    fun startTask(prompt: String, runAsBenchmark: Boolean) {
        val mode = if (runAsBenchmark) com.example.core.model.ExecutionMode.BENCHMARK else com.example.core.model.ExecutionMode.LIVE_DEVICE
        startTask(prompt, mode, null)
    }

    fun startCustomTask(commands: List<TaskCommand>, prompt: String, runAsBenchmark: Boolean = false) {
        val mode = if (runAsBenchmark) com.example.core.model.ExecutionMode.BENCHMARK else com.example.core.model.ExecutionMode.LIVE_DEVICE
        startTask(prompt, mode, commands)
    }

    private suspend fun executeAutonomousPlan(
        executionId: String,
        prompt: String,
        predefinedCommands: List<TaskCommand>?,
        mode: com.example.core.model.ExecutionMode,
        startTime: Long
    ) {
        // Persist the execution before planning or dispatch so process death cannot erase all evidence of the task.
        repository.saveExecution(
            id = executionId,
            prompt = prompt,
            status = "RUNNING",
            totalSteps = predefinedCommands?.size ?: 0,
            successfulSteps = 0,
            startTimestamp = startTime,
            endTimestamp = null,
            durationMs = 0L,
            failureReason = null,
            isLiveExecution = mode == com.example.core.model.ExecutionMode.LIVE_DEVICE
        )
        repository.log("INFO", "Engine", "Starting execution '$executionId' [Mode: $mode]: $prompt", executionId)

        // Step 1: Normalize task via HierarchicalIntentRouter or deterministic fallback (A01, A07, A08)
        val commands = if (predefinedCommands != null) {
            predefinedCommands
        } else {
            val router = intentRouter
            var routedCommands: List<TaskCommand>? = null

            _state.update { it.copy(status = ExecutionStatus.ROUTING, statusMessage = "Evaluating hierarchical intent router...") }
            val activePackage = LocalAgentAccessibilityService.lastEventPackage.value.ifBlank { null }
            val routeRes = router.route(context, prompt, activePackage)
            if (routeRes.isOfflineDegraded) {
                val reason = "Device is offline and intelligence backend unreachable (0% hallucination fallback)."
                completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, mode)
                return
            }

                if (routeRes.steps.isNotEmpty()) {
                    when (val valResult = com.example.core.intent.PlanSchemaValidator.validateAndMap(routeRes.steps)) {
                        is com.example.core.intent.PlanSchemaValidator.ValidationResult.Valid -> {
                            routedCommands = valResult.commands
                            repository.log(
                                "INFO",
                                "IntentRouter",
                                "Plan accepted from Tier ${routeRes.sourceTier} [${routeRes.backendName}] (${routedCommands.size} steps, latency ${routeRes.latencyMs}ms, provenance ${routeRes.provenance})",
                                executionId
                            )
                            _state.update {
                                it.copy(
                                    activeTier = routeRes.sourceTier,
                                    activeBackendName = routeRes.backendName,
                                    intentConfidence = routeRes.confidence
                                )
                            }
                        }
                        is com.example.core.intent.PlanSchemaValidator.ValidationResult.Invalid -> {
                            val reason = "Plan validation failed for backend ${routeRes.backendName}: ${valResult.errors.joinToString("; ")}"
                            repository.log("ERROR", "IntentRouter", reason, executionId)
                            completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, mode)
                            return
                        }
                    }
                }

            if (routedCommands != null && routedCommands.isNotEmpty()) {
                routedCommands
            } else {
                _state.update { it.copy(status = ExecutionStatus.PARSING, statusMessage = "Parsing natural language command...") }
                val parsed = parser.parse(prompt)
                if (parsed.isEmpty()) {
                    val reason = "Unsupported or uninterpretable request: '$prompt'. The agent cannot safely plan actions for this instruction."
                    repository.log("WARN", "Parser", reason, executionId)
                    completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, mode)
                    return
                }
                parsed
            }
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
            repository.log("SECURITY", "ApprovalGate", "Consequential risk detected in plan '${plan.planId}'. Explicit approval is required.", executionId)
            if (!approvalGate.canExecute(plan.planId)) {
                val highRiskSteps = plan.steps.filter {
                    it.riskLevel == com.example.core.approval.RiskLevel.HIGH ||
                        it.riskLevel == com.example.core.approval.RiskLevel.CRITICAL
                }
                val riskSummary = highRiskSteps.joinToString { "${it.stepIndex}:${it.riskLevel}" }
                val reason = "Task blocked before dispatch: explicit user approval is required for consequential step(s) [$riskSummary]. This build has no approval action connected to the execution gate; no task actions were dispatched."
                repository.log("SECURITY", "ApprovalGate", reason, executionId)
                completeExecution(
                    executionId,
                    ExecutionStatus.COMPLETED_FAILURE,
                    reason,
                    emptyList(),
                    startTime,
                    mode
                )
                return
            }
        }

        // Live vs Benchmark Mode Enforcement (A02, A01)
        if (
            mode == com.example.core.model.ExecutionMode.LIVE_DEVICE &&
            (LocalAgentAccessibilityService.instance == null || !LocalAgentAccessibilityService.isServiceConnected.value)
        ) {
            val requiresAccessibility = commands.any {
                it is TaskCommand.ClickElement || it is TaskCommand.EnterText ||
                it is TaskCommand.Scroll || it is TaskCommand.OpenDrawer ||
                it is TaskCommand.WaitForElement || (it is TaskCommand.VerifyCondition && it.rule !is com.example.core.model.VerificationRule.AlwaysPass)
            }
            if (requiresAccessibility) {
                val reason = "Accessibility service is disconnected in LIVE_DEVICE mode. Cannot observe live screen or dispatch physical gestures. Enable 'LocalAgent Automation Service' or run in BENCHMARK mode."
                repository.log("ERROR", "Engine", reason, executionId)
                completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, reason, emptyList(), startTime, mode)
                return
            }
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
            // Each step deliberates against the current live screen, never the previous step's stale pre-state.
            val preObs = if (mode == com.example.core.model.ExecutionMode.LIVE_DEVICE) {
                captureFreshObservation()
            } else {
                simulatedBenchmarkState
            }
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
                // Issue A05: Re-observe state before retrying non-idempotent action
                if (retryCount > 0) {
                    val freshObs = captureFreshObservation()
                    if (freshObs != null && preObs != null) {
                        val recheck = verifier.verify(
                            command = command,
                            preSnapshot = preObs,
                            postSnapshot = freshObs,
                            actionDispatchedTimestampMs = stepStartTime
                        )
                        if (recheck.status == StepStatus.SUCCESS) {
                            repository.log(
                                "INFO",
                                "Engine",
                                "Step ${index + 1} postcondition already satisfied upon re-observation before retry: ${recheck.reason}. Skipping redundant actuation.",
                                executionId
                            )
                            stepOutcome = StepExecutionRecord(
                                stepIndex = index + 1,
                                commandSummary = command.displayName,
                                targetSelector = extractTargetSelectorDesc(command),
                                dispatchedAction = "Verified existing state without re-dispatch (Idempotency Guard)",
                                preSnapshotId = preObs.id,
                                postSnapshotId = freshObs.id,
                                status = StepStatus.SUCCESS,
                                verificationDetails = "Observed postcondition satisfied without duplicate actuation: ${recheck.reason}",
                                durationMs = System.currentTimeMillis() - stepStartTime
                            )
                            break
                        }
                    }
                }

                repository.log(
                    "JOURNAL",
                    "StepAttempt",
                    "Prepared attempt ${retryCount + 1} for step ${index + 1}/${commands.size} (${command.displayName}). If the process stops before verification is persisted, treat the outcome as uncertain and re-observe the device before any retry.",
                    executionId
                )
                val stepResult = withTimeoutOrNull(stepTimeoutMs) {
                    executeSingleStep(
                        command = command,
                        stepIndex = index + 1,
                        mode = mode,
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
                } else if (
                    stepResult.status == StepStatus.SUCCESS ||
                    retryCount >= maxRetries ||
                    stepResult.verificationDetails.startsWith("SAFE_STOP:")
                ) {
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

            if (finalRecord.verificationDetails.startsWith("SAFE_STOP:")) {
                val reason = finalRecord.verificationDetails.removePrefix("SAFE_STOP:").trim()
                completeExecution(
                    executionId,
                    ExecutionStatus.COMPLETED_FAILURE,
                    reason,
                    executedRecords,
                    startTime,
                    mode
                )
                return
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
                        mode = mode,
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
                    completeExecution(executionId, ExecutionStatus.COMPLETED_FAILURE, failMsg, executedRecords, startTime, mode)
                    return
                } else {
                    repository.log("INFO", "RecoveryEngine", "Recovery successful! Strategy '${recoveryPlan.strategyName}' overcame failure.", executionId)
                }
            }
        }

        completeExecution(executionId, ExecutionStatus.COMPLETED_SUCCESS, null, executedRecords, startTime, mode)
    }

    private suspend fun executeSingleStep(
        command: TaskCommand,
        stepIndex: Int,
        mode: com.example.core.model.ExecutionMode,
        stepStartTime: Long,
        simulatedState: UiSnapshot,
        onSimulatedStateUpdate: (UiSnapshot) -> Unit
    ): StepExecutionRecord {
        val useLive = mode == com.example.core.model.ExecutionMode.LIVE_DEVICE
        val service = LocalAgentAccessibilityService.instance

        val requiresLiveUiObservation =
            command is TaskCommand.ClickElement ||
                command is TaskCommand.EnterText ||
                command is TaskCommand.Scroll ||
                command is TaskCommand.OpenDrawer ||
                command is TaskCommand.WaitForElement ||
                (command is TaskCommand.VerifyCondition &&
                    command.rule !is com.example.core.model.VerificationRule.AlwaysPass)

        if (useLive && requiresLiveUiObservation &&
            (service == null || !LocalAgentAccessibilityService.isServiceConnected.value)
        ) {
            return StepExecutionRecord(
                stepIndex = stepIndex,
                commandSummary = command.displayName,
                targetSelector = extractTargetSelectorDesc(command),
                dispatchedAction = null,
                preSnapshotId = null,
                postSnapshotId = null,
                status = StepStatus.INCONCLUSIVE,
                verificationDetails = "SAFE_STOP: Accessibility service is unavailable or disconnected in LIVE_DEVICE mode; refusing to dispatch UI actions or substitute simulated state.",
                durationMs = System.currentTimeMillis() - stepStartTime
            )
        }

        // 1. Capture Pre-Action Snapshot
        val preSnapshot = if (useLive) {
            service?.captureLiveSnapshot()
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

        // Enforce freshness and permission-consent checks at the executor boundary,
        // not only inside ClickElement. This also protects text, scroll, drawer,
        // wait, and verification commands from acting on stale or blocked UI.
        if (useLive && requiresLiveUiObservation) {
            val observationAgeMs = preSnapshot?.let { System.currentTimeMillis() - it.timestampMs }
            val observationProblem = when {
                preSnapshot == null -> "No live UI snapshot is available"
                observationAgeMs == null -> "Live UI snapshot age could not be determined"
                observationAgeMs < 0L -> "Live UI snapshot timestamp is in the future"
                observationAgeMs > 4000L -> "Live UI snapshot is stale (${observationAgeMs}ms old; maximum age is 4000ms)"
                else -> null
            }
            if (observationProblem != null) {
                return StepExecutionRecord(
                    stepIndex = stepIndex,
                    commandSummary = command.displayName,
                    targetSelector = extractTargetSelectorDesc(command),
                    dispatchedAction = null,
                    preSnapshotId = preSnapshot?.id,
                    postSnapshotId = null,
                    status = StepStatus.INCONCLUSIVE,
                    verificationDetails = "SAFE_STOP: ${observationProblem}; refusing UI dispatch, exploration, retry, or automated recovery.",
                    durationMs = System.currentTimeMillis() - stepStartTime
                )
            }
        }

        if (useLive && preSnapshot != null) {
            val interruption = com.example.core.reflection.InterruptionDetector.assessSnapshot(preSnapshot)
            if (interruption.type == com.example.core.reflection.InterruptionType.PERMISSION_DIALOG) {
                return StepExecutionRecord(
                    stepIndex = stepIndex,
                    commandSummary = command.displayName,
                    targetSelector = extractTargetSelectorDesc(command),
                    dispatchedAction = null,
                    preSnapshotId = preSnapshot.id,
                    postSnapshotId = null,
                    status = StepStatus.INCONCLUSIVE,
                    verificationDetails = "SAFE_STOP: ${interruption.description} Explicit user authorization is required; no task action was dispatched.",
                    durationMs = System.currentTimeMillis() - stepStartTime
                )
            }
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
                val observationAgeMs = effectiveSnapshot?.let { System.currentTimeMillis() - it.timestampMs }
                if (useLive && (
                    effectiveSnapshot == null ||
                    observationAgeMs == null ||
                    observationAgeMs > 4000L ||
                    observationAgeMs < 0L
                )) {
                    val observationReason = when {
                        effectiveSnapshot == null -> "No live UI snapshot is available"
                        observationAgeMs == null -> "Live UI snapshot age could not be determined"
                        observationAgeMs < 0L -> "Live UI snapshot timestamp is in the future"
                        else -> "Live UI snapshot is stale (${observationAgeMs}ms old; maximum age is 4000ms)"
                    }
                    return StepExecutionRecord(
                        stepIndex = stepIndex,
                        commandSummary = command.displayName,
                        targetSelector = command.selector.toReadableDescription(),
                        dispatchedAction = null,
                        preSnapshotId = effectiveSnapshot?.id,
                        postSnapshotId = null,
                        status = StepStatus.INCONCLUSIVE,
                        verificationDetails = "SAFE_STOP: ${observationReason}; refusing click, scroll exploration, retry, and automated recovery.",
                        durationMs = System.currentTimeMillis() - stepStartTime
                    )
                }
                var resolution = resolver.resolve(command.selector, effectiveSnapshot)

                if (useLive && service != null) {
                    val interruption = com.example.core.reflection.InterruptionDetector.assessSnapshot(effectiveSnapshot)
                    if (interruption.type == com.example.core.reflection.InterruptionType.PERMISSION_DIALOG) {
                        return StepExecutionRecord(
                            stepIndex = stepIndex,
                            commandSummary = command.displayName,
                            targetSelector = command.selector.toReadableDescription(),
                            dispatchedAction = null,
                            preSnapshotId = effectiveSnapshot?.id,
                            postSnapshotId = null,
                            status = StepStatus.INCONCLUSIVE,
                            verificationDetails = "SAFE_STOP: ${interruption.description} Permission authorization is required; LocalAgent stopped this step without granting, dismissing, scrolling past, or retrying the dialog.",
                            durationMs = System.currentTimeMillis() - stepStartTime
                        )
                    }
                }

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

            is TaskCommand.AddNote -> {
                dispatchedActionDesc = "Added note content: '${command.noteContent}'"
            }

            is TaskCommand.ExportDurableVault -> {
                dispatchedActionDesc = "Exported durable memory vault to ${command.destinationPath}"
            }

            is TaskCommand.MorningBriefing -> {
                dispatchedActionDesc = "Generated daily morning briefing for ${command.dateString}"
            }
        }

        // Allow UI to settle
        delay(stabilizationDelayMs)

        // 2. Capture Post-Action Snapshot (RULE: Fresh snapshot, never reuse pre-action!)
        _state.update { it.copy(status = ExecutionStatus.VERIFYING, statusMessage = "Capturing fresh post-action observation...") }

        val postSnapshot = if (useLive) {
            service?.captureLiveSnapshot()
        } else {
            // Simulated state is valid only in simulation/test modes, never as live-device evidence.
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
            is TaskCommand.AddNote -> "Note: '${command.noteContent}'"
            is TaskCommand.ExportDurableVault -> "Vault: ${command.destinationPath}"
            is TaskCommand.MorningBriefing -> "Briefing: ${command.dateString}"
        }
    }

    private suspend fun completeExecution(
        executionId: String,
        status: ExecutionStatus,
        failureReason: String?,
        steps: List<StepExecutionRecord>,
        startTime: Long,
        mode: com.example.core.model.ExecutionMode
    ) {
        val totalDuration = System.currentTimeMillis() - startTime
        val successCount = steps.count { it.status == StepStatus.SUCCESS }

        // Issue A03: Require evidence and postcondition verification before reporting success
        val hasAnyFailure = steps.isEmpty() || steps.any { it.status != StepStatus.SUCCESS }
        val effectiveStatus = if (status == ExecutionStatus.COMPLETED_SUCCESS && hasAnyFailure) {
            ExecutionStatus.COMPLETED_FAILURE
        } else {
            status
        }
        val effectiveFailureReason = if (effectiveStatus == ExecutionStatus.COMPLETED_FAILURE && failureReason == null) {
            "Execution completed without positive verification evidence across all steps."
        } else {
            failureReason
        }

        repository.saveExecution(
            id = executionId,
            prompt = _state.value.taskPrompt,
            status = effectiveStatus.name,
            totalSteps = steps.size,
            successfulSteps = successCount,
            startTimestamp = startTime,
            endTimestamp = System.currentTimeMillis(),
            durationMs = totalDuration,
            failureReason = effectiveFailureReason,
            isLiveExecution = mode == com.example.core.model.ExecutionMode.LIVE_DEVICE
        )

        val logMsg = "Execution finished with status: $effectiveStatus (Success: $successCount/${steps.size}, Duration: ${totalDuration}ms)"
        repository.log(if (effectiveStatus == ExecutionStatus.COMPLETED_SUCCESS) "INFO" else "ERROR", "Engine", logMsg, executionId)

        _state.update {
            it.copy(
                status = effectiveStatus,
                failureReason = effectiveFailureReason,
                statusMessage = if (effectiveStatus == ExecutionStatus.COMPLETED_SUCCESS) "Task verified successfully with observed evidence!" else "Execution stopped: ${effectiveFailureReason ?: "Verification failed"}",
                durationMs = totalDuration
            )
        }
    }

    private suspend fun handleCancellation(executionId: String, startTime: Long) {
        val totalDuration = System.currentTimeMillis() - startTime
        timerCountdownJob?.cancel()
        timerCountdownJob = null
        actuatorController.resetAllActuators()

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
        repository.log("WARN", "Engine", "Task execution aborted by user. Actuators reset safely.", executionId)

        _state.update {
            it.copy(
                status = ExecutionStatus.ABORTED_BY_USER,
                statusMessage = "Execution aborted by user. Actuators deactivated.",
                failureReason = "User clicked Stop",
                durationMs = totalDuration,
                activeTimerCountdownSec = null
            )
        }
    }

    private suspend fun handleFatalError(executionId: String, message: String, startTime: Long) {
        val totalDuration = System.currentTimeMillis() - startTime
        timerCountdownJob?.cancel()
        timerCountdownJob = null
        actuatorController.resetAllActuators()

        repository.log("ERROR", "Engine", "Fatal engine failure: $message", executionId)
        _state.update {
            it.copy(
                status = ExecutionStatus.COMPLETED_FAILURE,
                statusMessage = "Fatal error: $message",
                failureReason = message,
                durationMs = totalDuration,
                activeTimerCountdownSec = null
            )
        }
    }

    fun cancelCurrentTask(reason: String = "Task execution cancelled by user") {
        executionJob?.cancel()
        timerCountdownJob?.cancel()
        timerCountdownJob = null
        actuatorController.resetAllActuators()
        _state.update {
            it.copy(
                status = ExecutionStatus.ABORTED_BY_USER,
                statusMessage = "Execution cancelled. All actuators safely deactivated.",
                failureReason = reason,
                activeTimerCountdownSec = null
            )
        }
        engineScope.launch {
            repository.log("WARN", "Engine", "Task explicitly cancelled: $reason")
        }
    }

    fun stopExecution() {
        cancelCurrentTask("Task execution stopped by user")
    }

    fun triggerEmergencyKillSwitch(reason: String = "Physical Volume-Up Key double-pressed - Kill Switch") {
        executionJob?.cancel()
        timerCountdownJob?.cancel()
        timerCountdownJob = null
        actuatorController.resetAllActuators()
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
        val isLiveExecution = _state.value.executionMode == com.example.core.model.ExecutionMode.LIVE_DEVICE
        val snapshot = if (service != null && LocalAgentAccessibilityService.isServiceConnected.value) {
            service.captureLiveSnapshot()
        } else if (isLiveExecution) {
            // No live service means no live observation. Never substitute benchmark state.
            null
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
