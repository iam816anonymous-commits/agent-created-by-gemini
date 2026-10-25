# Repository Audit and Reference-Based Improvement Plan

**Primary repository:** `iam816anonymous-commits/agent-created-by-gemini`  
**Reviewed branch:** `main`  
**Reviewed head observed:** `aa413aa6df67ce73ad5a79eb038a37ae9b2efcfc` — `refactor: generalize device telemetry and state types` (2026-10-25)  
**Review type:** Remote source-level review of selected current files, recent commit diffs, repository READMEs, and reference-repository READMEs.  
**Not performed:** Full local checkout, Gradle build, npm test/build, device/emulator execution, full-tree review, or exhaustive audit of all execution entry points. Therefore this is a high-value triage, not a final certification.

## 1. Executive finding

The latest main-branch work made real progress, especially in hardware telemetry provenance and the beginnings of a browser-side `UnifiedHardwareController`. However, **the repository is not yet demonstrated to be a fully autonomous, end-to-end working agent**. Several important defects remain visible in the source excerpts reviewed, and the new tests have not been shown to run in CI or on a physical device.

The highest-risk architectural issue is that the project now has a TypeScript browser/dashboard hardware controller and a separate native Android `HardwareManager`. They may each be appropriate adapters for their runtime, but the current evidence does not establish one shared authoritative command contract and safety policy spanning both. The web controller also still contains behaviours that conflict with its “zero fabricated data” / “verified physical state” claims.

**Do not start a broad rewrite.** First establish the reproducible baseline, then fix the smallest set of issues needed for one real task to complete end-to-end.

## 2. What changed since the earlier issue list

The most recent main-branch commit inspected is `aa413aa6df67ce73ad5a79eb038a37ae9b2efcfc`. Its diff shows the following real changes:

### Improvements visible in source

1. **Storage metrics fallback removed.** `DeviceCapabilityDetector.getStorageMetrics()` now returns nullable metrics and `null` on failure rather than inventing 64 GB total / 32 GB available. Status: **FIXED_UNVERIFIED** until build, callers, and tests are checked.
2. **Several sensor readings made nullable.** `LiveSensorTelemetry` no longer initializes accelerometer, light, proximity, and pressure readings to plausible-looking values. Status: **PARTIALLY IMPROVED** because sensor event callbacks still substitute values such as 9.8, 0, 5, and 1013.25 when an event value is missing, and the complete sensor lifecycle/freshness path has not been tested.
3. **Hardcoded environment snapshot values reduced.** `EnvironmentTopologyEngine.PhysicalEnvironmentSnapshot` now uses nullable fields for several metrics and generic device identity. Status: **PARTIALLY IMPROVED** because `agentRamBudgetMb = 500`, `agentActiveRamMb = 42`, generic orientation, and other fixed policy values still need to be distinguished from actual measurements.
4. **Arbitrary native camera fallback removed.** `HardwareManager.findCamerasWithFlash()` no longer chooses the first camera if no flash-capable camera was identified. Status: **FIXED_UNVERIFIED** for this specific fallback; front-flash availability and device testing remain unverified.
5. **Native torch state is less optimistic.** `HardwareManager.setTorch()` checks whether an emitter was selected and returns failure if none was actuated; exceptions no longer set the requested torch state. Status: **PARTIALLY IMPROVED** because the state is still assigned immediately after dispatch, not independently confirmed via `CameraManager.TorchCallback`, and dual/front support is device-dependent.
6. **Wi-Fi state update is less optimistic in one path.** Opening the Wi-Fi panel no longer immediately assigns the requested state; it says it is awaiting a broadcast. Status: **PARTIALLY IMPROVED** because fallback paths still return a successful `ActuatorResult` when they only launch settings.
7. **Bluetooth enable prompt is described as awaiting authorization.** This is more honest, but direct `BluetoothAdapter.enable/disable` still updates state immediately on a returned boolean, and the fallback paths need consistent result semantics.
8. **Browser telemetry now has provenance types and nullable fields.** `src/hardware/types.ts`, `src/hardware/UnifiedHardwareController.ts`, and `src/utils/dynamicDeviceProbe.ts` introduce `MEASURED`, `ENUMERATED`, `ESTIMATED`, `UNAVAILABLE`, `UNSUPPORTED`, and related states. Status: **PARTIALLY IMPROVED**, not complete removal of fabricated data.
9. **Browser-side controller and Vitest tests added.** The latest diff adds `src/hardware/UnifiedHardwareController.ts`, `src/hardware/types.ts`, and `src/hardware/__tests__/UnifiedHardwareController.test.ts`, plus a `test` script. Status: **SOURCE PRESENT, TEST EXECUTION UNVERIFIED**. Several test assertions are broad enough to pass without proving the desired behaviour.
10. **Remote bridge hardening exists in prior commits.** The `8fb3fa4` change masks the pairing PIN in public telemetry, defaults to loopback binding, adds bounded request parsing, and adds a failed-PIN lockout. Status: **PARTIALLY IMPROVED**; adversarial and lifecycle tests still required.
11. **Approval gate has explicit plan approval state.** `TaskApprovalGate` has `approvePlan` and `canExecute`. Status: **SOURCE PRESENT, ENFORCEMENT UNVERIFIED** until every execution entry point is traced and tested.
12. **StepVerifier has freshness and semantic checks for UI actions.** It rejects missing/stale post-action snapshots and has target-specific UI verification logic. Status: **PARTIALLY IMPROVED** because `TaskCommand.ControlActuator` still returns `SUCCESS` merely because a signal was dispatched, and several other command types return success based on generic statements rather than command-specific evidence.

No source-level change listed above is proof of a successful build, a passing test suite, or physical device operation. The latest commit's combined status endpoint returned no status checks; this does not prove tests failed or passed, only that no check results were returned by that query.

## 3. Confirmed issues from inspected current source

### P0 — Fix before calling the agent functional

#### P0-1: Browser “zero fabricated data” policy still has fallbacks
- `src/components/DeviceModelProfiler.tsx`: `totalRamMb: dynamicProbe.totalRamMb || 4096`, `osOverheadMb || 1850`, `activeAppBufferMb || 720`. These reintroduce hardcoded values after telemetry correctly returned `null`.
- `src/utils/dynamicDeviceProbe.ts`: screen width/height fall back to 360/640, pixel ratio to 1, and touch points to 0. These should be tagged as UI defaults or unavailable, not presented as measured hardware.
- `src/hardware/UnifiedHardwareController.ts`: when RAM is unmeasured, `computeAdaptiveRecommendation` still names SmolLM2-360M and asserts a conservative RAM policy. A model recommendation must say that device suitability is **unknown** until measurements/runtime validation support a recommendation.
- `isRealPhysicalDiscovery: true` is assigned by the browser controller even though browser-exposed metrics may describe the browser host and are not a full physical-device inventory.
- Fix: keep missing values null; distinguish browser-host telemetry from native-device telemetry; do not assert a model fit or applied memory governor when neither has been verified.

#### P0-2: Actuator “observed physical state” is not independent observation
- `src/hardware/UnifiedHardwareController.ts` updates `observedPhysicalState` from `params.enabled ?? true` when a command is merely accepted. This copies requested state into the field intended to describe observed state.
- The browser torch OFF path returns `COMMAND_ACCEPTED_UNVERIFIED`, which is appropriately cautious, but other state bookkeeping can still imply a physical state from the request.
- Native `HardwareManager` changes torch StateFlows immediately after `CameraManager.setTorchMode` returns, without showing independent callback confirmation in the reviewed source.
- Fix: maintain requested state, dispatch result, and observed state as separate fields. Only update verified physical state from a real callback/supported sensor; otherwise leave it unknown and report accepted-but-unverified.

#### P0-3: Dashboard still simulates hardware and prints success-like claims
- `src/App.tsx` contains `compose light` flows that toggle UI state using timers, hardcoded camera labels `0/1/DUAL`, fixed pulse arrays, and then log `[PASS] Optical choreography executed`. This path is not shown dispatching each pulse through the authoritative physical actuator API.
- `apply recommended` changes UI state and prints that a memory governor ceiling was set, although the reviewed path only selects a tier/preset and emits logs; no runtime model load or memory-governor acknowledgement is shown.
- `dump vault` prints a fixed path, CRC, size, and skill count. `benchmark` prints a fixed zero-allocation claim and hardware-specific performance language.
- Fix: label simulations explicitly; route real operations through the shared executor; derive every terminal success line from actual results and evidence; remove unsupported benchmark/storage assertions.

#### P0-4: Browser emergency stop can claim verified torch-off without waiting for it
- `emergencyStop()` calls `applyConstraints({torch:false})` without awaiting the returned promise, stops the track, and immediately records `SUCCESS_VERIFIED`.
- It clears actuator locks but does not show an abort/cancellation signal propagated to every in-flight operation. Clearing a lock is not the same as cancelling the operation.
- Fix: await bounded shutdown, stop tracks in a `finally` path, track in-flight operations with cancellation tokens/AbortController-like ownership, and only report verified success when the operation's verification contract is satisfied. Otherwise report failure or unverified.

#### P0-5: Approval-gate enforcement must be proven end-to-end
- `TaskApprovalGate` exposes `canExecute(planId)`, but a gate API existing is not proof all execution entry points enforce it.
- Recovery can synthesize commands, skills and remote requests may reach other entry points, and the reviewed snippets do not establish a single non-bypassable authorization boundary.
- Fix: trace every entry point to the final dispatcher and test that no action executes until the applicable approval/permission policy returns allow. Ensure recovery cannot bypass the gate.

### P1 — High-priority correctness and architecture defects

#### P1-1: “Unified controller” currently covers browser hardware only
- The TypeScript controller centralizes browser APIs, while native Android has a separate `app/src/main/java/com/example/core/hardware/HardwareManager.kt`.
- A cross-runtime system should not force a browser controller to call Android APIs it cannot access. Instead, define one **shared action/result/capability contract and policy/execution orchestration boundary**, with native Android and browser adapters implementing that contract. One authority does not mean one physical API or one giant source file.
- Fix: inventory all entry points and action owners; make the native and browser adapters report into the same domain-level result model; prevent duplicate controllers from owning conflicting state.

#### P1-2: Sensor enumeration can overstate capability
- The browser controller labels `DeviceMotionEvent` as accelerometer support and `DeviceOrientationEvent` as a gyroscope-like sensor. These APIs expose motion/orientation events, not a reliable enumeration of the physical sensor inventory.
- Camera `enumerateDevices()` proves video inputs, not torch capability. Camera labels may be empty before permission; the code marks a camera `ENUMERATED` and `supportsWaveModulation: true` before testing its track's `torch` capability.
- The code uses `idx === 0` to infer a back/environment camera when the label is absent. That is an assumption, not hardware discovery.
- Fix: distinguish API availability from sensor/actuator presence; discover torch support only from a permissioned live track's capabilities; never infer independent front/rear flash emitters from camera order or labels.

#### P1-3: Flashlight waveform implementation is not equivalent to all claimed patterns
- In the inspected controller, only `STROBE_BURST` applies thresholded torch on/off constraints; other wave formats are calculations/animation, not verified physical torch modulation.
- The strobe confirmation branch contains only a comment and does not actually block without authorization.
- `applyConstraints` calls in the animation loop are not awaited, so cadence and physical state are not synchronized or measured.
- Fix: implement only patterns the runtime supports, gate strobe explicitly, validate frequency/duration, serialize and await hardware changes, and report unsupported modulation honestly.

#### P1-4: Resource lifecycle is incomplete
- Browser audio dispatch creates an `AudioContext` and oscillator but does not show closing the context after the tone. It can accumulate contexts over repeated calls.
- Torch OFF attempts constraints but does not consistently release the camera stream/track. Emergency cleanup does not await torch-off.
- Native `HardwareManager` creates an unbounded `CoroutineScope(Dispatchers.Default)`, registers a BroadcastReceiver and sensor listeners, but the reviewed class does not show a matching public disposal method that cancels the scope, unregisters receivers, unregisters sensors, cancels timed torch jobs, and stops vibration.
- Fix: explicit owner/lifecycle, bounded operation timeouts, guaranteed cleanup for success/failure/cancel, and tests for repeated use and disposal.

#### P1-5: Native sensor readings still synthesize fallback values
- `HardwareManager.onSensorChanged` uses `?: 0f`, `?: 9.8f`, `?: 5f`, and `?: 1013.25f` if sensor arrays are missing. Sensor events should be validated; invalid/missing readings should be skipped or marked unavailable, not replaced with plausible values.
- Native telemetry has a timestamp but does not itself expose stale/unavailable status for each reading.
- Fix: validate array length/value, track per-sensor last sample time/status, and expose sensor presence separately from current reading.

#### P1-6: Native Wi-Fi/Bluetooth result semantics remain inconsistent
- Wi-Fi fallback opens system settings and can return `ActuatorResult(true,...)` even though the requested toggle is not verified.
- Bluetooth fallback also returns success when a prompt/settings panel was opened; direct adapter calls may update the StateFlow before a broadcast confirms actual state.
- Fix: use statuses such as `COMMAND_ACCEPTED_UNVERIFIED`, `PERMISSION_REQUIRED`, and `UNSUPPORTED`; update observed state only from broadcasts/current state reads; unregister receivers on disposal.

#### P1-7: Recovery can dismiss dialogs and replay failed commands automatically
- `AutonomousRecoveryEngine.formulateRecoveryPlan` can return a dismiss command followed by the original failed command, or scroll then retry. The reviewed logic does not show risk classification/approval/postcondition checks at this decision point.
- Dismissing permission, payment, credential, security, or destructive dialogs may be unsafe; blindly replaying a command may duplicate a successful but unobserved action.
- Fix: classify interruption and target; never dismiss sensitive dialogs automatically; refresh and verify state before retry; use bounded retries and idempotency rules; require approval when risk increases.

#### P1-8: Verifier still grants success to commands without independent proof
- `StepVerifier` has strong snapshot freshness checks, but its `ControlActuator` branch returns `SUCCESS` based on dispatch text. `WebResearch` returns success with a generic statement. The default branch can return success at 0.9 confidence without a command-specific postcondition.
- Fix: each command type must have a specific verification contract. Missing evidence is `INCONCLUSIVE`, not success. The executor must not overwrite an inconclusive verifier result.

#### P1-9: Web UI duplicates hardware actions after dispatch
- `src/App.tsx` calls the unified controller for chime/haptic, then also calls `playWebAudioChime()` or `triggerHapticFeedback()` in the promise callback regardless of whether the controller result is unsupported or failed. This can duplicate an action on supported devices and create a simulated action after a failure.
- Fix: remove duplicate side-effect paths or clearly separate preview simulation from physical dispatch; only update UI based on the returned operation result.

#### P1-10: Model recommendation still uses hardcoded presets and does not prove runtime fit
- Device presets in `DeviceModelProfiler.tsx` are reasonable as explicit reference profiles, but must not be silently treated as measured device data.
- The live preset currently substitutes 4096 MB RAM, 1850 MB OS overhead and 720 MB buffer if values are absent; it maps mobile devices to SDK 27 and desktop to SDK 34, and supplies SIMD labels like NEON/WebGL2/SIMD/WASM as a fixed list.
- `apply recommended` selects a preset/tier and logs a memory ceiling without evidence the model was loaded or the runtime accepted a governor setting.
- Fix: preserve presets only as labelled examples; use nullable dynamic profile fields; separately test model file availability, runtime load, peak memory, latency, and failure cleanup on the actual target device.

## 4. Reference-repository improvement strategy

Use the repositories as evidence sources, not as wholesale replacements:

| Main-repo concern | First reference to inspect | What to compare before reusing |
|---|---|---|
| One execution pipeline, typed action results, stale-safe observation, target resolution, verification | `Mobile-muse-` | `GoalDispatcher`, action/result/verification contracts, observation bounds, resolver priorities, permission registry, and tests. Verify exact paths in source. |
| Android accessibility lifecycle, local planner/reflection, OCR fallback, memory boundaries | `mobile_agent` | Actual accessibility service, live target acquisition, postcondition checks, cancellation, and low-RAM behaviour. |
| Skill manifests, capabilities, approval/revocation, checkpoints and process-death recovery | `skill-agent` | Manifest schema, lifecycle state machine, capability authorization and migration tests. |
| Recovery, checkpointing, reflection, telemetry and chaos evaluation | `autonomous_software_agent` | State recovery and evaluation design; adapt concepts only, do not import its Python software-agent runtime into Android. |
| Provenance-aware knowledge and incremental synthesis | `open-knowledge-base` | Consider only after the deterministic execution loop works; keep raw source and citations. |
| HTTP timeouts, web worker lifecycle and remote security | `Browser-` | Only if the web research/remote subsystem is in scope; do not import unrelated scanning or stealth capabilities. |

The README files for these repositories were inspected; their full implementation quality has **not** yet been verified. Stage A must inspect source and tests at exact paths and create a reference matrix with commit SHA, file/symbol, test evidence, compatibility, license, and recommended action: reuse concept, port minimal code, or reject.

## 5. First working baseline — acceptance contract

Do not begin by adding more autonomous features. Prove one small real task:

**Acceptance scenario:** “Open Calculator and tap 7.”

Expected sequence:
1. Normalize to typed `LAUNCH_APP` then `CLICK` commands.
2. Resolve Calculator through Android PackageManager/launcher discovery, not a hardcoded package guess.
3. Check permission, policy, current task state and approval requirement.
4. Launch Calculator and wait for a fresh, interactive observation of the expected foreground package.
5. Resolve button 7 from the latest accessibility tree; reject stale or ambiguous targets.
6. Dispatch one click through the authoritative executor.
7. Capture a post-action snapshot with a newer generation/time and verify the Calculator display shows 7.
8. Persist step result and verification evidence.
9. Report success only if the postcondition is proven; otherwise return failure/inconclusive with evidence.
10. Verify cancellation/emergency stop prevents later steps.

Required tests:
- Parser/normalizer and plan-to-executor wiring.
- Missing, stale, wrong-package and non-interactive observations.
- Ambiguous/missing target and wrong editable field.
- Action dispatch success with failed postcondition.
- Cancellation during launch, action, retry and actuator wave.
- Approval/permission bypass attempts through every entry point.
- Retry after uncertain outcome without duplicate side effects.
- Cleanup after exception, timeout, cancellation and service disconnect.
- Physical-device test on supported Android API, with logs. If no physical-device test is run, mark it NOT VERIFIED.

## 6. Required implementation order

1. **Baseline audit:** establish build/test/CI status, full entry-point map, governance/phase alignment, and master checklist.
2. **One real task:** make launch → fresh observation → target resolution → click → postcondition verification work.
3. **P0 safety:** approval enforcement, honest results, emergency-stop propagation, removal of fake success, remote secrets/privacy.
4. **Shared contract:** one policy/execution/result contract with native and browser adapters; eliminate duplicate action paths.
5. **Observation/recovery:** stale snapshots, retry idempotency, safe modal handling, bounded timeouts, lifecycle cleanup.
6. **Hardware truth:** capability provenance, real sensor readings, torch and network result semantics, safe waveform handling.
7. **Persistence/skills:** durable task evidence, preconditions/postconditions, process-death recovery, learning validation.
8. **Reasoning and cloud/local model:** only after deterministic execution passes.
9. **Remote/dashboard parity and certification:** every control traceable to real operation; all claims evidence-backed.

Each batch must be small, testable, and mapped to the existing formal phase gate. Do not skip ahead if the governance document does not authorize the change.

## 7. Evidence required before completion

For every issue, retain:
- Reproduction and root cause.
- Minimal diff and affected files.
- Regression test(s) and exact commands/results.
- Build/lint/CI status.
- Emulator/device, Android API, browser and permission context where applicable.
- Logs or snapshots proving the postcondition.
- Whether evidence is source-only, automated, emulator, or physical-device.
- Known limitations and unresolved issues.

No issue may be marked `VERIFIED_FIXED` based only on code comments, a commit message, test source existing, or a UI success message. The final report must list every issue ID and evidence link.

## 8. Governance-file finding

At the reviewed `main` ref, fetching `docs/PHASE_SCOPE_GOVERNANCE_RULE.md` returned 404, while `docs/PHASE_0_GOVERNANCE_AND_AUDIT.md` exists and describes sequential phase gates and a three-tier evidence discipline. Before implementation, confirm whether the governance file exists on the working branch or is referenced under another path. Do not assume the roadmap branch itself authorizes implementation.
