# Jules Execution Brief: Autonomous, Device-Adaptive Agent and Fully Connected App

**Primary repository:** `iam816anonymous-commits/agent-created-by-gemini`  
**Purpose:** Turn the existing application into a real, device-adaptive agent with truthful runtime discovery, justified local-LLM recommendations, verified downloads, and a coherent end-to-end experience across every screen and sub-screen.  
**Execution mode:** Audit first; then implement small, testable vertical slices. This document is a target architecture and execution contract, not proof that any feature currently works.

---

## 0. Mandatory Jules operating rules

1. Before editing any file, read `docs/PHASE_SCOPE_GOVERNANCE_RULE.md`. If it does not exist on the working branch, do not invent it or silently ignore governance: inspect and follow the authoritative existing phase/governance document (currently known as `docs/PHASE_0_GOVERNANCE_AND_AUDIT.md` if still present), record the discrepancy, and ask for clarification only if the scope cannot be resolved safely.
2. Inspect the current branch, HEAD, working tree, build instructions, module boundaries, existing screens, navigation, repositories, services, state holders, permissions, tests, and CI. Record the exact commit SHA and baseline results before changing code.
3. This is the primary implementation repository. Other repositories named below are **read-only references**, not targets for edits. Inspect their actual code, tests, licenses, dependency versions, and architecture before borrowing anything.
4. Make the smallest targeted change that proves one end-to-end capability. Preserve working behavior and completed phases. Do not perform unrelated refactors, bulk rewrites, or speculative abstractions.
5. Never delete, weaken, skip, or rewrite a test merely to make a change pass. Add regression tests for changed behavior and retain meaningful failure cases.
6. Do not claim a feature is implemented because a button exists, a log says success, a command was accepted, a mock passed, a build compiled, or a screen changed. Report separately: source inspection, unit tests, integration tests, emulator tests, and physical-device tests.
7. Do not add fabricated sample devices, fake sensor values, placeholder model downloads, hardcoded benchmark results, pretend progress, simulated success, or demo values to production flows. If data is unavailable, show **Unknown / Not available**, why it is unavailable, and a retry or settings action when appropriate.
8. Do not ask for permission repeatedly if the app can explain and request it at the correct boundary. Do not bypass Android permissions, user confirmation, background-execution limits, or model license restrictions.
9. Keep a living implementation ledger at `docs/AUTONOMY_IMPLEMENTATION_STATUS.md`: each item needs status, files changed, tests executed, evidence, device/runtime tested, known gaps, and next step. Use statuses `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `IMPLEMENTED_UNVERIFIED`, and `VERIFIED`. Only mark `VERIFIED` with reproducible evidence.
10. Work phase-by-phase. Do not jump ahead to advanced intelligence, a large model, or hardware features while the core path is not connected and verified.

## 1. Product objective

Build one integrated Android agent that can:

- Discover the actual device and runtime capabilities dynamically.
- Determine device model/manufacturer, Android version/API level, ABI, available RAM, storage space, thermal/battery state where permission/API allows, available accelerators, and required permissions.
- Explain which local LLMs are realistically compatible with this specific device and why; offer a trustworthy direct download/install flow only for artifacts that are verified compatible and legally redistributable.
- Let the user choose local, remote, or hybrid inference, with explicit privacy, network, cost, latency, and offline trade-offs.
- Execute tasks through a shared orchestrator and real capability adapters, with authorization, current observations, action results, postcondition verification, cancellation, and recovery.
- Make every screen, sub-screen, menu, card, toggle, and action either connect to a real use case or clearly be unavailable. No disconnected decorative controls.
- Maintain one coherent source of truth for device profile, model inventory, selected model, permissions, active task, hardware capabilities, operation state, errors, and event history.
- Give the user honest, evidence-based status. A request accepted is not the same as an operation completed.

## 2. Required architecture: one connected system

Audit existing architecture before choosing file names or replacing components. Reuse good existing abstractions where possible. The intended responsibility boundaries are:

```text
Android UI (screens, subscreens, navigation)
                 |
          ViewModels / UI state
                 |
         Use-case / Facade layer
                 |
      Task Orchestrator + Event stream
       /           |            \
Policy &       Capability       State /
Approval       Registry         Persistence
       \           |            /
           Execution adapters
       /          |           \
Accessibility   Device       Inference
/UI actions     hardware     runtime
       \          |           /
      Observation + Verification
                 |
    Result, evidence, telemetry, recovery
```

The diagram is conceptual. Fit it to the current codebase; do not create duplicate layers if equivalent working ones already exist.

### 2.1 Single source of truth

Create or adapt one typed domain state model rather than maintaining independent screen-local copies of the same facts. At minimum, define explicit contracts for:

- `DeviceProfile`: values, source, observed-at timestamp, confidence/availability, and refresh status.
- `Capability`: capability identifier, supported/unsupported/unknown status, reason, permission state, adapter, and verification evidence.
- `ModelArtifact`: model ID, exact revision, publisher, format, quantization, artifact size, context limit, license, runtime compatibility, source URL, checksum/signature if available, and compatibility evidence.
- `ModelRecommendation`: candidate model, reasons, constraints, estimated storage/RAM requirements, confidence, and caveats. Estimates must be labeled as estimates, not measurements.
- `TaskRequest`, `ExecutionPlan`, `ApprovalDecision`, `ActionResult`, `ObservationSnapshot`, `VerificationResult`, and `TaskReport`.
- `OperationState`: idle, discovering, planning, awaiting approval, downloading, verifying artifact, loading model, executing, verifying result, cancelling, completed, failed, or blocked.
- `AgentEvent`: timestamp, correlation/task ID, source, severity, event type, and safe diagnostic details.

Use stable IDs and typed results. Avoid using display strings as the internal API. Propagate cancellation and errors through every layer. UI state should be derived from domain state, not used as the authority for hardware or task success.

### 2.2 End-to-end action contract

Every user action must follow the same lifecycle where applicable:

1. Validate the request and current prerequisites.
2. Refresh any stale device/capability/model state needed for the action.
3. Build a plan with explicit capability and permission requirements.
4. Check risk and obtain user approval for sensitive or consequential actions.
5. Resolve the real adapter and execute with bounded timeout/cancellation.
6. Collect fresh post-action observations from the real platform or runtime.
7. Verify a command-specific postcondition independently of the dispatch return value.
8. Persist the truthful result and evidence; publish state/events to all relevant screens.
9. If it fails, return a typed failure with reason, safe recovery choices, and no false success.
10. Ensure retries are bounded and idempotent where possible; do not blindly repeat side effects.

For example, “Open Calculator and tap 7” is complete only after the app is identified/launched, a fresh accessibility snapshot for the intended app is obtained, the target is resolved and clicked, and a fresh observation verifies the expected result. A launch command accepted or a click dispatched is not sufficient.

## 3. Dynamic device profiling — no invented measurements

Build a device-profile pipeline using Android APIs and the actual runtime. Audit existing implementations before adding another profiler.

### 3.1 Values to discover

- Manufacturer, commercial model/device identifier where available, Android release/API level, build fingerprint only where appropriate, ABI/CPU architecture.
- Total memory from a platform source; current available memory from `ActivityManager.MemoryInfo`; app memory/process constraints where available. Keep total RAM separate from currently available RAM and app heap limits.
- Internal storage total and currently available bytes from filesystem/stat APIs. Distinguish app-private storage from general free storage and from external/removable storage.
- Runtime-supported acceleration: CPU features and only those GPU/NPU/runtime delegates actually exposed and usable by the selected inference runtime.
- Battery/thermal status when available and permitted; refresh at sensible lifecycle/operation boundaries rather than continuously polling.
- Required permissions and service readiness (e.g., accessibility service enabled) as separate facts from device hardware capability.
- Camera, torch, microphone, audio, sensors, Wi-Fi/Bluetooth, and other hardware through capability-specific platform APIs. Presence in a browser or a device spec sheet is not proof that an Android adapter can operate it.

### 3.2 Data provenance and freshness

Each observed field must carry at least:
- `value` (nullable if unknown),
- `source` (specific API/adapter),
- `observedAt`,
- `status` (`AVAILABLE`, `UNAVAILABLE`, `PERMISSION_REQUIRED`, `UNSUPPORTED`, `STALE`, `ERROR`),
- optional reason/error category.

Never replace null/exception with values such as 4 GB RAM, 360×640 screen size, default sensor readings, a generic camera, or a fixed free-storage amount. Never use `value || default` when zero/null has semantic meaning. Keep policy budgets (for example, “reserve X MB for Android”) explicitly labeled as policy estimates and configurable, never as measured device values.

Refresh profiles when the app starts, on explicit refresh, after a meaningful lifecycle change, before a model recommendation/download/load, and when a relevant capability changes. Add timeouts and stale-data rules. Do not make every screen independently query and reinterpret the device.

### 3.3 Honest UI for missing data

When a field cannot be measured:
- Show “Unavailable” or “Unknown”, not a guessed number.
- Explain the reason if known (permission, API limitation, unsupported device, transient error).
- Offer Retry or Open Settings only when it is a valid remedy.
- Prevent downstream recommendations that require the missing value, or mark them provisional with clear assumptions.
- Record the failed probe for diagnostics without exposing sensitive identifiers unnecessarily.

## 4. Device-specific LLM recommendation and download

This is a core product requirement. It must be a real pipeline, not a hardcoded list of “recommended models”.

### 4.1 Separate measured facts, model metadata, and estimates

Use three sources of truth:
1. **Device facts:** discovered from the current device/runtime.
2. **Model catalog:** versioned metadata fetched from a trusted source or maintained in a signed/versioned catalog; each entry must link to the publisher's canonical model/artifact page.
3. **Compatibility estimator:** transparent rules that compare the two and produce reasons and uncertainty.

Do not claim a model fits merely because its parameter count looks small. Consider artifact download size, installed size, tokenizer/config assets, runtime-specific format, quantization, context length/KV cache, peak load memory, concurrent app pressure, native runtime overhead, ABI, Android API, acceleration support, and free storage safety margin. Use published or measured metadata when available; mark unknown requirements as unknown.

### 4.2 Recommendation outcomes

Each candidate should be classified as:
- **Compatible — verified:** artifact and runtime combination has reproducible compatibility evidence on this device class/runtime.
- **Likely compatible — estimate:** all known constraints appear to fit, but no device-specific verification exists.
- **Not recommended:** known incompatibility or insufficient measured resources.
- **Cannot determine:** critical metadata or device data is missing.

Explain why. Rank by a user-selectable objective: lowest memory, best quality within resource limits, offline use, fastest startup, privacy, or balanced. Do not silently optimize only for model quality.

A recommendation must never guarantee tokens/sec or answer quality unless measured under a defined benchmark. If performance benchmarking is added, use real timings, warm/cold runs, device/thermal conditions, prompt/context settings, and disclose that results vary.

### 4.3 Model catalog and direct download

- Inspect the model license and redistribution terms. Prefer linking to the official publisher/model page or official artifact URL. Never mirror or redistribute weights unless licensing and hosting terms explicitly permit it.
- The app may show a **Download** action only if it has a valid artifact URL, known expected size, supported format/runtime, license metadata, and a verification strategy. Otherwise show **Open official model page** or **Unavailable**, not a fake download button.
- For downloadable files, use Android DownloadManager or a properly scoped foreground download service as appropriate. Display actual bytes received versus verified expected bytes, with truthful pause/resume/cancel/error states.
- Verify SHA-256 or publisher-provided signature when available. If no authoritative checksum/signature exists, explicitly disclose that verification is limited; do not label it verified.
- Store model files in appropriate app-accessible storage, prevent path traversal, validate file type/config, handle low storage, and avoid exposing arbitrary filesystem paths.
- Download into a temporary location; only promote to the model inventory after completion and integrity/metadata checks. Clean up partial files safely.
- On install/load, validate runtime compatibility and memory constraints again. Loading failure must return a typed error and preserve the previously working model/configuration.
- Track catalog revision, model revision, license, artifact size, checksum status, local file state, runtime state, and last error. Revalidate stale catalog metadata.
- Offer remote/hybrid alternatives only with clear privacy/network/cost disclosures and an explicit user choice. Never send user content to a remote service without the configured consent and security model.

### 4.4 No hardcoded model recommendation

Do not encode “if RAM > X then model A” as the only recommendation logic or show a static list as if dynamically discovered. A versioned compatibility policy may contain thresholds, but the thresholds must be documented, tested, and distinguished from actual measurements. Model metadata should come from a real catalog. If catalog/network is unavailable, use a clearly labeled cached catalog with its age, or show that recommendations cannot be refreshed.

## 5. Connected screen and navigation plan

First inventory all current screens, sub-screens, tabs, menus, dialogs, buttons, toggles, cards, and routes. Map each one to a use case, state source, action, result, and tests. Do not build replacement screens before this audit.

The following is the intended information architecture; merge with existing screens when appropriate rather than creating duplicates.

### A. Home / Agent
- Current task input, task progress, approvals, result, and next safe action.
- Capability summary from the capability registry.
- Recent tasks/events from persisted records.
- Buttons invoke the orchestrator; no local fake task-completion timers.

### B. Device
- Overview: discovered model, OS/API, ABI, RAM/storage with provenance/freshness.
- Resources: live available RAM/storage and thermal/battery data when available.
- Capabilities: hardware/service/permission status with reasons.
- Diagnostics: last probe result, retry, safe diagnostic export.
- Every value is read from the shared device profile; refresh invalidates stale values and updates all consumers.

### C. Models
- Recommended: candidates produced by the compatibility engine with reasons and caveats.
- Catalog/Search: current real catalog, filterable by runtime, size, license, offline/remote, and format.
- Model details: publisher, license, exact revision, artifact size, resource estimate, compatibility status, source link.
- Download Manager: real progress, pause/resume/cancel, integrity verification, retry.
- Installed Models: scan managed model storage, actual installed metadata, loaded/unloaded state, remove action with confirmation.
- Runtime: selected model, load/unload, runtime errors, measured memory/performance only when instrumented.
- Model selection must update the central selected-model state and all dependent screens. Downloaded is not the same as installed, and installed is not the same as loaded.

### D. Tasks / Skills
- Task history, details, plan, approval, execution events, verified outcome, retry/cancel.
- Skills/capabilities list only entries registered with valid manifests, declared permissions, supported adapters, and tests.
- A skill is unavailable if its real dependency is absent; never show a working status because a manifest or UI card exists.

### E. Hardware / Actions
- One capability page per supported class where it is useful (torch, camera, sensors, audio, network, accessibility/UI control).
- Show discovered support, permission, adapter readiness, last real observation, and last action result.
- Each action goes through the shared policy/orchestration/adapter/verification path. No separate screen-specific fake controllers.
- “Open Settings” is an instruction/navigation outcome, not proof that Wi-Fi/Bluetooth/torch was changed.

### F. Permissions / Safety
- Centralized permission and service status.
- Explain why a permission is needed, request it at the point of use, re-check actual grant/service state afterward.
- Approval state is enforced by the execution layer, not merely displayed by the UI.
- Emergency stop must cancel or safely terminate all cancellable active operations, await cleanup where possible, and report any actuator whose safe state could not be confirmed.

### G. Activity / Diagnostics / Settings
- Event stream and logs tied to task IDs, redacted appropriately.
- Runtime settings are persisted and validated; UI must read back the applied configuration.
- Diagnostics must show real measurements and explicit “not measured” fields. No hardcoded benchmark results, CRCs, file paths, sensor inventories, or success logs.

### Navigation contract

Every visible interactive element must have:
- a defined route or use case,
- a real state source,
- loading/empty/error/permission-required states,
- a result or feedback path,
- cancellation/back behavior where relevant,
- tests for the behavior.

Create a screen-to-use-case matrix in `docs/APP_SCREEN_CONNECTION_MATRIX.md` during the audit. A control that has no real behavior should be disabled with a clear explanation or removed from the active UI; do not leave it looking functional.

## 6. Execution, observation, safety, and truthful results

- Keep planning separate from execution. A plan is not an action.
- Every command resolves a real capability adapter; unsupported commands fail explicitly.
- Require fresh observations before UI targeting and after actions. Snapshots need timestamp/generation, package/window identity, and validity checks.
- Verify each action against a command-specific postcondition. Do not treat dispatch messages, button clicks, API acceptance, or optimistic state mutation as verification.
- Maintain task/correlation IDs across planning, approval, action, observation, verification, and persistence.
- Use bounded retries, exponential/backoff where appropriate, deadlines, cancellation, and idempotency keys for retryable operations.
- Recovery must inspect current state and risk before retrying; no blind retries for consequential side effects.
- Apply centralized policy to all entry points: UI, skill, voice/agent input, remote control, and background triggers. Do not rely on a UI-only approval gate.
- Log factual evidence and redact secrets, tokens, PINs, private screen content, and unnecessary identifiers.
- When verification is impossible, return `COMPLETED_UNVERIFIED` or `ACTION_ACCEPTED_UNVERIFIED` as appropriate, never `SUCCESS_VERIFIED`.

## 7. Read-only reference repositories

Inspect exact branches/commits and relevant source/tests. Record findings in `docs/REFERENCE_REPOSITORY_COMPARISON.md`. Use these as idea sources only; the primary repository remains the only implementation target unless the user explicitly changes scope.

- `iam816anonymous-commits/Mobile-muse-`: possible reference for unified dispatch, typed action/result contracts, observation snapshots, target resolution, permission registry.
- `iam816anonymous-commits/mobile_agent`: possible reference for Android accessibility lifecycle, planner/reflection, screen understanding, memory boundaries.
- `iam816anonymous-commits/skill-agent`: possible reference for skill manifests, capability authorization, lifecycle, checkpoint recovery.
- `iam816anonymous-commits/autonomous_software_agent`: possible reference for checkpoints, reflection/recovery, telemetry and fault-injection evaluation.
- `iam816anonymous-commits/open-knowledge-base`: possible later reference for provenance-aware persistent knowledge; do not prioritize before reliable execution.
- `iam816anonymous-commits/Browser-`: only relevant browser/remote security, network timeouts, and web-specific patterns; do not treat browser APIs as proof of Android hardware capability.

For each candidate, record: repository URL, commit SHA, files/symbols examined, tests and test results, license/dependency compatibility, reusable concept, adaptation needed, security risks, and decision (`reuse concept`, `port minimal code`, or `reject`). Never copy an entire repository or import a large dependency without a specific demonstrated need. Do not assume code is correct because it exists in another repository.

## 8. Phased delivery and acceptance gates

### Phase 0 — Baseline, governance, and inventory
- Reconcile governance documents and phase boundaries.
- Record branch/HEAD/worktree and baseline build, unit tests, instrumentation tests, lint, and CI status. If a command cannot run, state why.
- Inventory modules, routes, controls, state stores, device probes, model/runtime code, action adapters, permissions, persistence, and tests.
- Create/update `docs/APP_SCREEN_CONNECTION_MATRIX.md`, `docs/REFERENCE_REPOSITORY_COMPARISON.md`, and `docs/AUTONOMY_IMPLEMENTATION_STATUS.md`.
- Gate: baseline and unresolved risks are documented; no product code change is made until scope and high-risk contracts are understood.

### Phase 1 — Real device profile and capability registry
- Implement/repair one canonical device profile with provenance, freshness, nullable fields, and failure states.
- Connect all device screens to it.
- Gate: unit tests prove missing/failed probes remain unknown, never become fabricated values; emulator/physical evidence is separately recorded.

### Phase 2 — Model catalog, compatibility, recommendation
- Define catalog schema and trusted-source strategy.
- Implement transparent compatibility decisions based on device facts and runtime metadata.
- Gate: deterministic tests for unknown RAM/storage, insufficient disk, unsupported ABI/runtime, stale catalog, and multiple ranking objectives. No unsupported candidate may be called verified-compatible.

### Phase 3 — Safe model download/install/load lifecycle
- Wire catalog -> details -> download -> integrity check -> install -> runtime load -> selected model.
- Gate: test cancellation, network interruption, corrupt artifact, insufficient storage, checksum mismatch, unsupported format, load OOM/failure, restart/resume behavior, and recovery to prior model.

### Phase 4 — Unified task orchestration and verified action path
- Connect task input to planning, permission/approval, adapter execution, fresh observation, postcondition verification, result persistence, and UI state.
- Gate: end-to-end “Open Calculator and tap 7” or equivalent platform-available scenario passes with evidence at each step; failure cases produce truthful failures.

### Phase 5 — Connect all screens and sub-screens
- Complete the screen-to-use-case matrix.
- Ensure every active control invokes a real use case and every state is sourced from the relevant domain/service.
- Gate: navigation tests, UI tests for loading/empty/error/permission states, and action-to-result integration tests. No orphan controls or duplicate sources of truth.

### Phase 6 — Hardware adapters and safety
- Verify each hardware action with the actual Android API and independent observation/callback where available.
- Enforce approval and capability checks at all execution entry points; implement cancellation and cleanup.
- Gate: physical-device tests for each claimed hardware capability, plus permission denial, unavailable hardware, API errors, interruption, and cleanup cases. Do not claim hardware success from emulator-only evidence.

### Phase 7 — Resilience, observability, and resource management
- Add bounded concurrency, timeouts, cancellation propagation, stale-state handling, structured events, safe recovery, and resource-aware scheduling.
- Gate: fault-injection tests for network loss, app/service restart, stale observation, timeout, concurrent commands, cancellation races, storage pressure, and memory pressure.

### Phase 8 — Release readiness
- Verify clean build, lint, unit/instrumentation tests, dependency/license review, security/privacy review, migration compatibility, accessibility, and a reproducible physical-device test report.
- Gate: no critical/high unresolved safety or data-integrity issues; all claims labeled with evidence level.

Do not collapse these phases into one large PR. Each phase should have a focused diff and explicit acceptance criteria.

## 9. Required tests and evidence

At minimum add/retain tests for:

- Device probes return unknown/error instead of fallback numbers on exceptions or missing data.
- Device profile refresh updates all screens and invalidates stale recommendations.
- Model recommendation uses actual available resources and rejects missing critical metadata.
- Model artifact URL, license, size, revision, format, and checksum handling are validated.
- Download progress comes from actual bytes; cancel/retry/resume and corruption paths are real.
- Downloaded/installed/loaded/selected model states remain distinct.
- A failed model load does not erase the previous working selection.
- Approval cannot be bypassed by alternate entry points.
- Fresh UI observation belongs to the expected app/window before target selection.
- UI actions are verified with a fresh postcondition, not a dispatch return string.
- Hardware state changes only after platform evidence; settings launch is not actuator success.
- Emergency stop cancels supported in-flight operations and reports any unconfirmed safe state.
- Every visible control has a connected behavior or is explicitly disabled/unavailable.
- Telemetry and diagnostics do not invent values and do not leak secrets.

Test layers:
1. Unit tests for policies, compatibility, typed contracts, and state transitions.
2. Adapter/integration tests using fakes only for deterministic boundary testing; label them as fakes.
3. Android instrumentation/emulator tests for lifecycle, permissions, navigation, downloads, and accessibility where available.
4. Physical-device tests for hardware, thermal/memory behavior, real inference performance, and device-specific compatibility.
5. CI evidence with exact workflow/run identifiers where available.

A fake is allowed in a test fixture; it must never be presented as live production data.

## 10. Implementation discipline and PR format

For each phase or vertical slice:
1. State scope, files likely affected, and acceptance test before editing.
2. Inspect callers, interfaces, data flow, dependencies, and current tests.
3. Make the smallest implementation that closes one real end-to-end gap.
4. Add regression tests and run the narrow tests first, then relevant full checks.
5. Inspect the complete diff for unrelated changes, fabricated values, optimistic state updates, dead controls, and unhandled errors.
6. Update the implementation ledger with actual evidence and unresolved gaps.
7. Summarize changed files, behavior before/after, tests and exact outcomes, device used, known limitations, and the next smallest task.

Never say “complete”, “fully autonomous”, “dynamic”, “hardware-integrated”, or “verified” without defining what was tested and showing evidence.

## 11. Definition of done

The application is ready to claim device-adaptive integrated behavior only when:

- Device facts are genuinely discovered and carry provenance/freshness; unknown remains unknown.
- Recommendations are generated from a real model catalog plus actual device/runtime constraints, with transparent reasoning and uncertainty.
- Any offered download points to a valid, authorized artifact and has real progress, integrity handling, cancellation, and installation/load states.
- The chosen model is actually loaded by a compatible runtime, not merely selected in UI.
- Tasks flow through one orchestrator and typed contracts, with authorization, fresh observations, independently verified postconditions, persistence, and honest reporting.
- All active screens and sub-screens are connected to shared state and real use cases.
- Production UI contains no dummy data, fake progress, fabricated measurements, or simulated success.
- CI and relevant tests pass, and hardware claims have physical-device evidence where required.
- Documentation identifies remaining limitations instead of hiding them.

**First instruction to Jules:** Perform Phase 0 only. Audit and document the current repository and reference repositories; do not implement product features in the same change. Present the baseline and a proposed Phase 1 file-level plan, then proceed sequentially according to the repository's governance rules.
