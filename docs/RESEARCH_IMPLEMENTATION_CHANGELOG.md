# Research-Informed Implementation: Before/After Record

Base: `ffad2d0d56f8c146d7cd5d065c903dc782a0e8b2` (main at branch creation)

## Scope

This first implementation is deliberately limited to two safety properties inspired by mobile-agent research on fresh observations, explicit user control, and evidence-based action dispatch. It does not port another project's runtime or introduce a new dependency.

## Changes

### 1. Coordinate-only target resolution

**Before:** `TargetResolver` could manufacture a clickable `UiNode` from normalized coordinates, including a hardcoded 1080×2400 fallback, and return `Success`. The executor treated that success as permission to dispatch a tap.

**After:** If the snapshot has no accessible nodes or no semantic target matches, resolution fails explicitly. Coordinate guesses are not converted into verified target matches. This intentionally disables the unsafe fallback until a separate, explicitly unverified coordinate-action contract and safe dispatch policy are designed.

### 2. Android permission dialogs

**Before:** `InterruptionDetector` created a click command for text such as “Allow” and the executor could dispatch it as automatic recovery. Failed target resolution could also trigger scroll exploration and retry/recovery.

**After:** Permission-controller screens are classified by the window package or node package and require explicit user action. The detector returns no grant/dismiss command. The live click executor stops before dispatch, retries and automated recovery, and records a consent-required inconclusive result.

### 3. Missing or stale live observations

**Before:** A missing live snapshot could fall back to simulated state; a missing/stale target could also enter the self-healing path and attempt a scroll.

**After:** Live mode never substitutes simulated state when the accessibility service or snapshot is unavailable. Missing, stale (>4 seconds), or future-dated observations stop the click step before dispatch, scroll exploration, retry, or recovery. The result is explicitly inconclusive.

### 4. Recovery engine

**Before:** Autonomous recovery could still formulate a scroll/retry plan when a permission dialog was detected but no automatic dismiss command was available.

**After:** The recovery planner returns an empty action plan with a consent-required reason for permission dialogs.

### 5. Regression coverage

- Updated legacy coordinate-fallback tests to assert failure without semantic target evidence.
- Updated recovery tests to assert that permission dialogs do not generate grant, dismiss, scroll, or retry commands.
- Added coverage for permission-controller node-package detection when the window package differs.

## Evidence and limitations

- Source changes and regression-test updates are committed on a separate branch; no UI source files were changed.
- The first CI run executed 71 unit tests and failed three legacy assertions that expected the now-disabled coordinate fallback or automatic permission bypass. Those assertions were updated to test the new safety contract rather than weaken unrelated coverage.
- A subsequent CI run is in progress; the final result must be checked before considering the branch merge-ready. The Android APK build is downstream of unit tests and has not yet been confirmed for the latest changes.
- Physical-device permission behavior, accessibility event timing, real target dispatch and post-action state verification remain **NOT VERIFIED** until exercised on a device.
- No changes were made to main and no pull request was merged.


### 6. Live-mode integrity and consequential-action approval

**Before:** A request for LIVE_DEVICE execution silently became SIMULATION when the accessibility service was disconnected. The risk gate classified high/critical plans and logged that the gate was active, but execution continued without checking approval.

**After:** Requested execution mode is preserved. If a live task requires accessibility and the service is disconnected, execution stops instead of pretending to operate the phone. High/critical plans are blocked before task actions are dispatched unless the exact plan is approved through the existing gate.

**UI constraint:** No approval action is currently wired from the existing UI to `TaskApprovalGate.approvePlan`. To honor the no-UI-change requirement, this patch fails closed and reports that approval is unavailable; sensitive tasks remain blocked until an approval path is implemented in a separately authorized UI change.



### 7. Single execution ownership, live observation integrity, and physical actuator truth

**Before:** Starting a new task cancelled an active task and immediately started another, allowing potential overlap during non-cancellable dispatch. A missing live accessibility service could also cause `captureFreshObservation()` to return a benchmark snapshot. The direct torch APIs reported simulated success and changed state even when no physical torch was available.

**After:** A second task request is rejected while an execution is active; the current task must be stopped before another starts. Live observation returns `null` when the live service is unavailable rather than substituting benchmark state. Torch APIs return explicit failure when physical hardware/API support is unavailable and do not mutate physical-state telemetry.

**Validation:** Added a regression test for truthful torch state. The current CI run must validate the latest commit; hardware-specific behavior remains NOT VERIFIED until tested on a physical device.


### 8. Durable execution start and per-step attempt journal

**Before:** The execution history row was written only when a task finished, so process death during planning or action execution could leave no durable execution record. Step deliberation could also reuse the previous step's pre-action observation.

**After:** A `RUNNING` execution record is persisted before planning starts; each step attempt writes a durable journal entry before entering the executor; and every live step deliberates using a newly requested live observation. The agent does not automatically resume or replay a task after process death.

**Limitation:** Automatic startup reconciliation of orphaned `RUNNING` records and physical process-kill tests are still outstanding. A durable journal improves evidence but does not, by itself, prove whether a system action completed.


### 9. Executor-boundary safety for every UI command

**Before:** Missing/stale observation and permission-dialog guards were concentrated in the click path. Text entry, scroll, drawer, wait, and verification commands could therefore reach different handling paths.

**After:** The executor now checks live-service connectivity, snapshot freshness, and permission-controller state before any UI-observation-dependent command. A detected permission dialog stops the whole step before app launch or other task action as well. Safe stops are terminal for the current task and are not retried or sent to automated recovery.

**Validation:** Regression coverage for permission classification and recovery already exists; the latest CI run must confirm compilation and tests for the executor-boundary change. Physical-device validation remains NOT VERIFIED.
