# Autonomous Agent Roadmap — Working Baseline First

**Primary repository:** `iam816anonymous-commits/agent-created-by-gemini`  
**Purpose:** Make the existing Android agent reliably perform real tasks end-to-end before expanding features or attempting broad refactors.  
**Status:** Roadmap only. This document does not claim that any issue is fixed or any phase is verified.  
**Branch:** `roadmap/autonomy-first-working-baseline`

---

## 1. Source-of-truth and governance

1. This repository is the **only primary implementation**. All product behaviour, contracts, tests, persistence, and final architecture must be owned here.
2. The owner's other repositories are **reference implementations only**. Do not merge repositories, copy entire modules, replace the architecture wholesale, or change those repositories as part of this work.
3. Before any code edit:
   - Read `docs/PHASE_SCOPE_GOVERNANCE_RULE.md` if it exists on the active branch.
   - If it is absent, inspect `docs/PHASE_0_GOVERNANCE_AND_AUDIT.md` and the existing phase/specification documents; report the missing expected file and use the existing authoritative governance source without silently inventing approval.
   - Perform and record a phase-alignment check: authorised phase, in-scope files, exclusions, dependencies, and acceptance criteria.
   - Inspect the current branch, working tree, recent changes, affected code, callers, interfaces, and tests before editing.
4. Make minimal, targeted changes only in files proven to need modification. Preserve all existing working behaviour and completed phases. No unrelated refactors, broad rewrites, or drive-by formatting.
5. Never delete, skip, weaken, loosen, or rewrite a test merely to make a build pass. If a test is wrong, document the evidence and obtain owner approval before changing its contract.
6. Keep each remediation batch small, review its diff, run focused tests, and then run relevant broader validation.
7. Never claim completion from code presence, mocked tests, a successful compile, a UI log, or an API acceptance response alone.

## 2. Product objective and definition of autonomy

Build a **local-first Android agent** that accepts a user goal, determines a safe executable plan, acts through supported Android capabilities, observes the real result, verifies the postcondition, recovers when appropriate, and reports the outcome honestly. The initial target is a low-resource Android device (including Android 8.1/API 27 and approximately 4 GB RAM / 64 GB storage), while respecting platform/version restrictions.

The core execution contract is:

`User Goal → Normalize → Plan → Capability/Permission Check → Safety Gate → Execute One Step → Fresh Observation → Independent Verification → Persist Evidence → Continue / Recover / Ask / Stop → Final Report`

Every entry point (native UI, voice, overlay, dashboard, remote bridge, scheduled task, skill, or AI planner) must converge on the same authoritative policy and execution pipeline. Entry points must not implement their own bypass execution paths.

Autonomy does **not** mean unrestricted action. Sensitive, destructive, financial, credential, security-setting, or ambiguous actions must pause for the appropriate user consent. The agent must be able to stop promptly and must never fabricate success.

## 3. Repository reference policy

Inspect the reference repositories read-only first. Create a reference matrix with exact file paths, symbols, tests, dependencies, licence/compatibility concerns, and evidence. A feature is not “better” merely because its README claims it.

| Reference repository | Inspect for | Rule |
|---|---|---|
| `iam816anonymous-commits/mobile_agent` | Android local-first planner/reflection, accessibility interaction, screen understanding, memory boundaries | Reuse ideas or small compatible implementations only after source and test review. |
| `iam816anonymous-commits/Mobile-muse-` | Command contracts, single execution pipeline, permission registry, observation snapshots, target resolution, verification, SQLite/Room event records | High-priority reference for contracts and observe-act-verify separation; compare against main repo before proposing changes. |
| `iam816anonymous-commits/skill-agent` | Capability-scoped skills, runtime consent, lifecycle, message routing, checkpointing, recovery, resource management | Reference for later skill/runtime orchestration; do not pull advanced features into the first working baseline. |
| `iam816anonymous-commits/autonomous_software_agent` | Checkpointing, critic/reflection, recovery, telemetry, adversarial evaluation patterns | Reference for later reliability/evaluation; do not import its software-engineering workflow as the mobile runtime. |
| `iam816anonymous-commits/open-knowledge-base` | Persistent, provenance-aware knowledge, incremental updates, contradiction/gap detection | Optional later reference for local knowledge; keep the initial task loop deterministic and lightweight. |
| `iam816anonymous-commits/Browser-` | Only if the web-research/browser subsystem is in scope: timeouts, worker lifecycle, audit/security boundaries | Not a source for Android control or a reason to add browser/stealth dependencies to the core agent. |

If the owner identifies additional reference repositories, add them to the matrix only after inspecting them. Do not assume any repository is correct, current, secure, or compatible without evidence. Record “not inspected” rather than guessing.

## 4. Roadmap and exit gates

Execute in order. These are remediation workstreams, not permission to ignore the repository's formal phase governance. Before implementation, map each workstream to the currently authorised project phase and stop if the phase scope does not allow it.

### Stage A — Freeze baseline and discover truth

**Goal:** Know what currently works before modifying it.

Tasks:
- Inspect the complete repo structure, current branch/head, working-tree status, governance documents, build scripts, Android manifest, permissions, app entry points, execution pipeline, actuator implementations, telemetry, tests, and CI.
- Build an inventory of user-visible controls and commands, their actual handlers, and their terminal outcomes.
- Reproduce representative commands on emulator/device where possible; separate source evidence, automated test evidence, and physical-device evidence.
- Create `docs/MASTER_REMEDIATION_CHECKLIST.md` (or update it if it already exists) with stable issue IDs, severity, evidence, owner, affected files, fix/test plan, status, and residual limitation.
- Establish baseline build, lint, unit test, instrumentation test, and CI results. Capture failures without “fixing” unrelated failures.
- Inspect reference repositories and record proven candidates in a separate comparison matrix. Do not change reference repositories.

**Exit gate:** Reproducible baseline and issue inventory exist; no issue is marked fixed without evidence. No feature code changes are required in this stage unless separately authorised.

### Stage B — Make one real end-to-end task work

**Goal:** Prove the core loop before adding more intelligence.

Use a narrow, repeatable acceptance scenario such as **“Open Calculator and tap 7”** on a supported test device.

Required behaviour:
1. Parse and normalize the request into typed commands.
2. Resolve the app dynamically using Android package/launcher APIs; do not assume package names or installed apps.
3. Check capability, permissions, foreground state, and action safety before execution.
4. Launch the app and wait for a fresh observation of the expected app/window.
5. Resolve the target against the current accessibility tree; reject stale snapshots and ambiguous matches.
6. Execute the click through the authoritative executor.
7. Capture a fresh post-action observation and verify a defensible outcome (for example, the expected display value includes 7), not just that an accessibility action returned true.
8. Persist task/step events and verification evidence.
9. Return success only when the postcondition is verified. Otherwise return a typed failure/uncertain status with the reason.
10. Ensure cancellation and the emergency stop prevent further steps.

Add deterministic tests for parser → plan → policy → executor → verifier state transitions, plus regression tests for existing supported actions.

**Exit gate:** The acceptance scenario passes repeatedly on an actual supported device with captured evidence, and automated regression tests pass. If physical hardware is unavailable, mark device acceptance **NOT VERIFIED**; do not claim the agent works end-to-end yet.

### Stage C — Establish a single authoritative execution and capability boundary

**Goal:** Eliminate competing control paths and optimistic state.

Tasks:
- Map all action entry points to one authoritative dispatcher/execution pipeline.
- Centralize capability discovery, permissions, action validation, result types, cancellation, state observation, and event logging. Platform-specific adapters are allowed; duplicate authoritative controllers/state stores are not.
- Define a shared action result contract that distinguishes verified success, accepted-but-unverified, unsupported, permission required/denied, invalid request, busy, timeout, cancelled, and failed.
- Separate desired/requested state, command acceptance, and independently observed physical/UI state.
- Ensure dashboard/terminal messages are derived from actual execution results, not hardcoded success text.
- Make browser-only simulation explicit and impossible to confuse with native Android hardware control.
- Keep hardware and UI operations behind typed interfaces with clear lifecycle and cleanup ownership.

**Exit gate:** Every in-scope command path is traceable to the same policy/executor; tests prove unsupported or failed operations cannot report verified success.

### Stage D — P0 safety and honest execution

**Goal:** No dangerous bypasses or false success.

Address all confirmed P0 defects first, including relevant issues in the existing checklist:
- Approval gate must block execution, not merely display a warning.
- All entry points (skills, remote bridge, cloud planner, console, overlay, scheduled jobs) must pass through the same safety gate.
- Emergency stop cancels active and queued work, child coroutines/jobs, loops, actuator operations, and follow-up steps; it leaves the system in a known safe state.
- Remove fabricated telemetry and fake success from device discovery, sensors, actuators, storage, network, model loading, and memory-governor claims.
- Ensure every action result is tied to an observed postcondition or clearly marked unverified.
- Validate parameters and permissions before hardware/system actions.
- Remove hardcoded secrets and verify remote-session authentication, expiry, and loopback/network restrictions.
- Redact credentials and sensitive user content from logs, task history, screenshots, and persisted memory.
- Fail closed when safety status or permission status cannot be established.

**Exit gate:** Every P0 checklist item is either **VERIFIED FIXED** with evidence or remains an explicit blocker. No “complete” declaration while any P0 remains open.

### Stage E — Reliable observation, action, verification, and recovery

**Goal:** Recover safely from normal Android variability.

Tasks:
- Refresh observations after app launch, window/focus changes, UI mutations, and actions; enforce snapshot generation/freshness and expected package/window checks.
- Re-acquire live nodes immediately before action; avoid acting on stale AccessibilityNodeInfo references.
- Use deterministic target matching and confidence thresholds; require clarification rather than guessing when targets are ambiguous.
- Use coordinate/OCR fallback only when justified, permissioned, bounded, and separately verified.
- Handle service disconnection, app crash, timeout, permission revocation, and foreground changes as explicit state transitions.
- Make retries idempotent or inspect current state before retrying to avoid duplicate successful actions.
- Bound retry count, total task duration, per-step timeouts, tree size/depth, memory, and concurrent operations.
- Re-plan only from fresh evidence, preserve completed-step records, and never blindly replay destructive steps.
- Guarantee cleanup of accessibility nodes, camera sessions, audio, vibration, coroutine jobs, listeners, and resources in success, failure, timeout, and cancellation paths.

**Exit gate:** Regression tests cover stale observations, focus/package mismatch, timeout, cancellation, duplicate retries, service disconnect, and resource cleanup. Real-device tests cover the most important transitions.

### Stage F — Dynamic hardware and sensor/actuator truth

**Goal:** Discover only what the platform actually exposes and control it through the same authority.

Tasks:
- Replace hardcoded device/RAM/CPU/GPU/battery/network/sensor/actuator fallback values with measured values or explicit unavailable/unsupported statuses.
- Attach provenance, timestamp/freshness, source API, permission, and confidence/status to each capability/measurement.
- Distinguish native Android APIs from browser APIs; browser APIs cannot be treated as a complete inventory of Android hardware.
- Dynamically enumerate supported cameras and torch capability; never infer independent front/rear/dual LED control from labels or model names.
- Distinguish physical actuator output from visual simulation. For wave/strobe patterns, prove which patterns the device API supports; report unsupported modulation honestly.
- Verify hardware action outcomes where the platform provides observable state; otherwise report accepted-but-unverified.
- Apply safe bounds to strobe frequency/duration and provide immediate cancellation/torch-off cleanup.
- Use actual runtime/device measurements before recommending or loading a local model. A recommendation is not proof of model availability, load success, or safe memory usage.

**Exit gate:** Capability registry and actuator tests distinguish discovered, unavailable, unsupported, permission-denied, estimated, stale, and verified states; physical actuator claims are device-tested or explicitly unverified.

### Stage G — Durable state, skills, and bounded learning

**Goal:** Learn reusable workflows without allowing learned content to bypass safety.

Tasks:
- Store task, step, observation reference, result, verification, failure, and cancellation events in SQLite/Room with migrations and bounded retention.
- Recover safely after process death: resume only from a valid checkpoint and re-check current device state before any action.
- Give every skill a typed manifest, version, capability requirements, permissions, preconditions, ordered actions, postconditions, rollback/stop policy, and verification strategy.
- Treat recorded/replayed skills as untrusted proposals; validate selectors and re-check preconditions at runtime.
- Separate skill candidate, tested, approved, and rejected states. Success counts alone must not auto-authorize sensitive actions.
- Learn only from evidence-backed outcomes; preserve provenance and allow correction/deletion of user memory.

**Exit gate:** Tests cover persistence, migrations, process restart, interrupted tasks, invalid/stale skills, and approval enforcement. The base task loop must remain functional with learning disabled.

### Stage H — Planning, local/cloud reasoning, and knowledge

**Goal:** Add intelligence only after deterministic execution is dependable.

Tasks:
- Make plans structured, schema-validated, dependency-aware, bounded, and explicit about preconditions and postconditions.
- Ensure the orchestrator consumes the actual plan; do not regenerate or discard it between planning and execution.
- Revalidate every planned action against live capabilities and safety policy immediately before execution.
- Keep deterministic fast paths for simple, well-defined actions.
- Treat local models as optional and measured against real device memory, latency, and thermal constraints. Avoid loading large models automatically on low-memory devices.
- Use cloud reasoning only under an explicit connectivity/privacy policy; cloud failure must trigger honest fallback or stop, never fake offline capability.
- Validate all model outputs and treat web pages, documents, skills, and tool outputs as untrusted data, not executable instructions.
- Add local knowledge/retrieval only when it improves task performance; record sources, freshness, confidence, and contradictions.

**Exit gate:** Tests prove invalid model output, unavailable cloud, stale knowledge, prompt injection, and model failure cannot bypass policy or produce fabricated completion.

### Stage I — Remote control, dashboard, and multi-entry-point parity

**Goal:** Ensure every UI is an honest supervisor for the same agent.

Tasks:
- Trace every dashboard button, terminal command, overlay action, remote request, and native UI control to its actual backend operation.
- Remove UI-only “applied” states that do not correspond to runtime changes.
- Secure remote control with authenticated sessions, least privilege, request size/time limits, replay protection where appropriate, explicit pairing/consent, session expiry, and safe shutdown.
- Show real task state, current step, permissions, observed results, evidence, and failure reasons.
- Make emergency stop available and effective across all entry points.
- Do not expose sensitive screen content or credentials in remote telemetry by default.

**Exit gate:** End-to-end tests demonstrate entry-point parity and no alternate path can bypass authorization, verification, or cancellation.

### Stage J — Hardening, compatibility, and release certification

**Goal:** Demonstrate reliability, not merely compile success.

Tasks:
- Run unit, integration, instrumentation, migration, security, cancellation, and regression suites.
- Validate minimum supported Android API and the current target API; document API-specific restrictions and permission behaviour.
- Test on physical hardware with accessibility enabled, permissions granted/denied, low memory, network changes, app switching, screen lock/unlock, and interruption.
- Capture reproducible logs and metrics for task success rate, false-success rate, timeouts, cancellation latency, resource leaks, and recovery.
- Run adversarial tests for ambiguous commands, unsafe prompts, malicious skill content, remote requests, and UI state mismatch.
- Inspect final diff and dependencies; confirm no unrelated changes, weakened tests, secrets, or unverified claims.
- Publish a final evidence matrix linking each issue to its tests and device evidence.

**Exit gate:** All P0 issues verified closed; all P1/P2 issues verified or explicitly dispositioned with rationale; release build and required tests pass; physical-device limitations are visible; no unverified feature is described as complete.

---

## 5. Priority order for the first remediation batches

Do not try to implement all stages in one giant change. Once Stage A is complete and the formal phase allows it, work in small batches in this order:

1. **Batch 1 — Truthful core loop:** command normalization, app launch, fresh observation, target resolution, action, postcondition verification, truthful result.
2. **Batch 2 — P0 safety:** approval enforcement, single safety path, emergency stop/cancellation, false-success elimination, secret/privacy checks.
3. **Batch 3 — Single execution/capability authority:** shared contracts and entry-point parity with minimal compatible changes.
4. **Batch 4 — Observation and recovery:** stale snapshot, retry/idempotency, timeouts, service disconnect, resource cleanup.
5. **Batch 5 — Hardware discovery and actuators:** provenance-backed dynamic inventory and truthful control results.
6. **Batch 6 — Persistence and skill lifecycle:** event evidence, safe checkpoint recovery, verified skill preconditions/postconditions.
7. **Batch 7 — Planning and reasoning:** plan consumption, structured validation, measured local/cloud fallback.
8. **Batch 8 — Remote/dashboard hardening and release certification.**

If the audit shows a different dependency order is necessary, document the evidence and revise the order without silently changing the authorised phase scope.

## 6. Required checklist status model

Use these statuses consistently:
- `NOT_AUDITED` — not yet examined.
- `CONFIRMED` — defect reproduced or supported by sufficient source evidence.
- `IN_PROGRESS` — scoped fix is underway.
- `FIXED_UNVERIFIED` — code changed, but acceptance evidence is incomplete.
- `VERIFIED_FIXED` — required tests passed and the required runtime/device evidence exists.
- `BLOCKED` — a named dependency, permission, device, API, or owner decision prevents verification.
- `NOT_APPLICABLE` — evidence and rationale recorded; never use to hide an inconvenient issue.

Every issue entry must include: stable ID, priority, affected component, reproduction/evidence, root cause, smallest safe fix, regression test, validation command/results, device/API tested, status, and remaining limitation.

## 7. Mandatory rules for every Jules/Gemini task

Paste or preserve these rules in every task:
- Read the governance file and complete a phase-alignment check before editing.
- Main repo only: `iam816anonymous-commits/agent-created-by-gemini`.
- Reference repositories are read-only sources of ideas/evidence unless the owner explicitly authorizes otherwise.
- Inspect code, call sites, contracts, dependencies, tests, and diffs before edits.
- Make minimal, targeted changes only in files proven to need modification.
- Preserve previously working behaviour and completed phases.
- No unrelated refactoring or scope expansion.
- Add regression tests for the defect and affected existing behaviour.
- Never weaken, skip, delete, or bypass tests to get green results.
- Do not mark an issue verified without actual recorded evidence.
- Distinguish source-level, automated-test, emulator, and physical-device evidence.
- If a requirement cannot be tested, mark it `BLOCKED` or `FIXED_UNVERIFIED` and state exactly why.
- At the end, report changed files, issue IDs addressed, tests and exact results, device/API tested, remaining risks, and the next smallest batch. Do not claim the entire project is complete while any required gate remains open.

## 8. Final success definition

The agent is “working” only when it can reliably complete a small set of real, repeatable, low-risk Android tasks through the same execution pipeline and prove the result from fresh observations. It is “autonomous” only when it can safely sequence steps, detect failure, recover within bounds, checkpoint progress, stop on risk or uncertainty, and report honest evidence. Advanced learning, large local models, broad sensor control, and elaborate dashboard features are secondary to that verified foundation.
