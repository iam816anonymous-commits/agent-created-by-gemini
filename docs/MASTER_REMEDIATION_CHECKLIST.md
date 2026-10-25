# Master Remediation Checklist

**Primary repository:** `iam816anonymous-commits/agent-created-by-gemini`  
**Baseline reviewed:** `main` at observed head `aa413aa6df67ce73ad5a79eb038a37ae9b2efcfc` (2026-10-25).  
**Current status:** Initial triage only. No item in this file is certified `VERIFIED_FIXED` by this review.  
**Detailed findings:** See [REPOSITORY_AUDIT_AND_REFERENCE_IMPROVEMENT_PLAN.md](REPOSITORY_AUDIT_AND_REFERENCE_IMPROVEMENT_PLAN.md).

## Status definitions

- `NOT_AUDITED`: not enough current source/test evidence to decide.
- `CONFIRMED_OPEN`: current reviewed source directly demonstrates the defect or a material part of it.
- `PARTIALLY_IMPROVED`: recent change addresses part of the concern, but the full acceptance criteria are not proven.
- `FIXED_UNVERIFIED`: the exact source defect appears removed, but tests/build/device evidence has not been verified.
- `VERIFIED_FIXED`: required automated and runtime/device evidence exists. **No items are marked this in the initial review.**
- `BLOCKED`: verification requires a named missing device, permission, dependency, or owner decision.
- `NOT_APPLICABLE`: only with written evidence and rationale.

Priority definitions: P0 = blocker for safe core functionality; P1 = high-priority correctness/reliability; P2 = important quality/claims/performance issue.

## A. Core execution and autonomy

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| A01 | P0 | Planner/router output must be the exact plan consumed by execution; no plan discarded or silently regenerated. | PARTIALLY_IMPROVED — recent commit says router integrated, but plan consumption is not proven end-to-end. |
| A02 | P0 | Simulation and live/native execution must be visibly distinct; simulation cannot report physical success. | CONFIRMED_OPEN — UI choreography and fixed terminal success messages remain in `src/App.tsx`. |
| A03 | P0 | Success must require a command-specific observed postcondition. | CONFIRMED_OPEN — UI verifier has freshness checks, but actuator and generic command branches can return success without independent evidence. |
| A04 | P1 | Cancellation must propagate to child jobs, retries, loops, actuator waves, and queued steps. | NOT_AUDITED — emergency stop clears locks but does not prove all in-flight operations are cancelled. |
| A05 | P1 | Retries must inspect current state and avoid duplicate side effects. | CONFIRMED_OPEN — recovery plan can retry failed commands without proven idempotency/postcondition recheck. |
| A06 | P1 | Task/step state transitions must be consistent and terminal states immutable. | NOT_AUDITED |
| A07 | P1 | Unsupported or ambiguous natural-language requests must ask/decline, not guess. | NOT_AUDITED |
| A08 | P1 | Parsed plans, arguments, targets, and action parameters must be schema-validated. | NOT_AUDITED |

## B. Safety and consent

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| B01 | P0 | Approval gate must actually block execution across every entry point. | PARTIALLY_IMPROVED — `TaskApprovalGate.canExecute` exists; complete bypass testing not shown. |
| B02 | P0 | Skills, remote requests, cloud plans, overlay and scheduled tasks cannot bypass safety policy. | NOT_AUDITED |
| B03 | P1 | Recovery must not automatically dismiss sensitive/permission/security dialogs. | CONFIRMED_OPEN — recovery can synthesize a dismiss command and retry. |
| B04 | P1 | Permission-required, denied, revoked and unsupported outcomes must be distinct. | PARTIALLY_IMPROVED — typed browser statuses exist; native/agent-wide result parity unproven. |
| B05 | P1 | Emergency stop must stop task execution and all actuator operations, not only a browser controller. | CONFIRMED_OPEN — browser controller stop is incomplete for in-flight work; cross-runtime stop unproven. |
| B06 | P1 | Sensitive UI, credentials, screenshots and logs must be redacted and governed by retention/consent. | PARTIALLY_IMPROVED — privacy-related recovery change exists; full data-path audit required. |

## C. Flashlight, audio, haptics and actuators

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| C01 | P0 | Torch success must reflect actual supported hardware and evidence, never simulated output. | PARTIALLY_IMPROVED — native missing-emitter failure improved; browser physical state is not independently verified. |
| C02 | P0 | Requested, dispatched, observed and verified actuator states must not be conflated. | CONFIRMED_OPEN — browser controller derives observed state from request parameters. |
| C03 | P0 | Wave operation lifecycle/coroutines must not self-cancel or outlive task ownership. | NOT_AUDITED — cancellation propagation and pattern loop need targeted tests. |
| C04 | P1 | Front/rear/dual emitters must be discovered, never inferred from index or label alone. | PARTIALLY_IMPROVED — native dynamic flash-facing discovery and removal of arbitrary fallback; browser still infers rear from index 0. |
| C05 | P1 | Torch-off/cleanup must await supported shutdown and release stream/resources safely. | CONFIRMED_OPEN — emergency stop does not await `applyConstraints`; ordinary OFF cleanup is incomplete. |
| C06 | P1 | Torch strength must be used only when supported; no fake brightness fallback. | NOT_AUDITED |
| C07 | P0 | Placeholder actuators must return unsupported/unverified, never success. | CONFIRMED_OPEN — native `StepVerifier` reports actuator dispatch as SUCCESS; UI has simulated hardware claims. |
| C08 | P1 | Audio/ToneGenerator/AudioContext resources must be closed and owned. | CONFIRMED_OPEN — browser audio dispatch creates an AudioContext without visible close/cleanup. |
| C09 | P1 | Validate waveform frequency, duration, amplitude and pattern parameters. | NOT_AUDITED |
| C10 | P1 | Native and browser actions must share consistent result semantics. | CONFIRMED_OPEN — native `ActuatorResult` and browser `HardwareOperationResult` remain separate contracts. |

## D. Wi-Fi and Bluetooth

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| D01 | P0 | Wi-Fi observed state must come from current state/broadcast, not optimistic request state. | PARTIALLY_IMPROVED — optimistic update removed in one path; settings fallback can still return success without verifying toggle. |
| D02 | P0 | Bluetooth state must be independently observed after request/authorization. | PARTIALLY_IMPROVED — prompt is described as awaiting authorization; direct adapter path and fallback results still need verification. |
| D03 | P1 | Android-version restrictions and runtime permissions must be handled explicitly. | NOT_AUDITED |
| D04 | P1 | External Settings changes and permission revocation must refresh state and listeners safely. | PARTIALLY_IMPROVED — broadcasts are registered, but lifecycle/disposal and all transitions unproven. |
| D05 | P1 | Opening Settings must not be reported as successful hardware state change. | CONFIRMED_OPEN — native fallback returns success after launching Settings. |

## E. Accessibility and UI automation

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| E01 | P0 | Text input must target the correct editable node, not a nearby/text-matching node. | NOT_AUDITED |
| E02 | P0 | Click dispatch must not count as proof that the intended action occurred. | PARTIALLY_IMPROVED — semantic UI verification exists; actuator/generic branches still overstate success. |
| E03 | P1 | Snapshots must be fresh and tied to the expected foreground package/window. | PARTIALLY_IMPROVED — StepVerifier checks timestamp/generation/root; launch-to-action path needs E2E proof. |
| E04 | P1 | Coordinate fallback must be bounded, justified, and independently verified. | CONFIRMED_OPEN — recovery can fall back to coordinate-centered/scroll exploration without demonstrated guardrails. |
| E05 | P1 | Accessibility nodes must be recycled/released on every path. | NOT_AUDITED |
| E06 | P1 | Gestures must use observed geometry and validated bounds, not fixed blind values. | NOT_AUDITED |
| E07 | P1 | App-launch and observation timeouts must be bounded and correctly classified. | NOT_AUDITED |
| E08 | P1 | Recovery must avoid unsafe dialog dismissal, blind scrolls, and blind retries. | CONFIRMED_OPEN — current recovery logic proposes dismissal/scroll/retry sequences. |
| E09 | P1 | Accessibility service disconnect must pause/stop active tasks and produce explicit status. | NOT_AUDITED |
| E10 | P1 | Physical-device E2E acceptance suite must exist and run. | NOT_AUDITED — no device evidence reviewed. |

## F. Telemetry and capability discovery

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| F01 | P0 | Browser probe must never fabricate RAM/CPU/GPU/battery/network values. | PARTIALLY_IMPROVED — nullable/provenance model added, but profiler reintroduces 4096/1850/720 and probe uses display defaults. |
| F02 | P0 | Storage metrics must return unavailable on failure, not guessed capacity. | FIXED_UNVERIFIED — native detector returns null on exception; caller/build/tests not verified. |
| F03 | P0 | Sensor readings must be genuine values, not plausible fallback numbers. | PARTIALLY_IMPROVED — nullable defaults added, but native event parsing still uses numeric fallbacks. |
| F04 | P1 | Every sensor reading must expose freshness, permission and unavailable/stale status. | PARTIALLY_IMPROVED — browser controller has a stale threshold; native per-sensor status is not established. |
| F05 | P0 | Physical environment snapshot must not present fixed values as measured device facts. | PARTIALLY_IMPROVED — several fields now nullable, but fixed budget/active-memory defaults remain and UI has fixed claims. |
| F06 | P1 | Capability registry must be authoritative for all runtimes and action entry points. | PARTIALLY_IMPROVED — browser registry added; native Android `HardwareManager` remains a separate authority. |
| F07 | P1 | Browser-host capabilities must not be conflated with native Android capabilities. | CONFIRMED_OPEN — browser discovery is labelled real physical discovery and app UI mixes browser/native claims. |
| F08 | P2 | Recommendations must use verified telemetry and runtime validation, not assumed RAM/GPU. | CONFIRMED_OPEN — unmeasured RAM still selects a specific model; profiler fallback values remain. |

## G. AI, cloud and offline behaviour

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| G01 | P0 | Router/planner output must be consumed without silent plan replacement. | PARTIALLY_IMPROVED — router integration commit exists; end-to-end plan lineage not proven. |
| G02 | P0 | Offline claims must match actual cloud/network state and fallback behaviour. | PARTIALLY_IMPROVED — WebResearchEngine has explicit failure/cache provenance; all UI and agent claims not audited. |
| G03 | P1 | AI, cached knowledge, web results and inferred facts need visible provenance. | PARTIALLY_IMPROVED — hardware provenance exists; provenance not consistent across all outputs. |
| G04 | P1 | Local model must be present, load successfully, and pass real-device resource checks. | NOT_AUDITED — recommendation/preset does not prove model load. |
| G05 | P1 | Model output must be parsed/validated and treated as untrusted. | NOT_AUDITED |
| G06 | P1 | Cloud failure, offline fallback, retries and privacy policy must be explicit. | NOT_AUDITED |
| G07 | P2 | Benchmark/performance claims must be measured and reproducible. | CONFIRMED_OPEN — UI prints hardware-specific zero-allocation/throughput claims without evidence in the reviewed path. |

## H. Music/torch synchronization

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| H01 | P0 | Audio and physical torch must share a clock/scheduler if synchronization is claimed. | NOT_AUDITED |
| H02 | P0 | AudioTrack/AudioContext ownership and cleanup must be explicit. | CONFIRMED_OPEN — browser audio contexts are created without visible close/cleanup. |
| H03 | P0 | Torch jobs must stop when playback ends, fails, is cancelled or emergency stop fires. | NOT_AUDITED |
| H04 | P1 | Camera/torch identifiers must be dynamically resolved, not assumed. | PARTIALLY_IMPROVED — native camera discovery improved; browser fallback still uses camera order. |
| H05 | P1 | Audio failure must stop dependent light sequence when synchronization requires both. | NOT_AUDITED |
| H06 | P1 | External beat detection must be implemented and permissioned before claimed. | NOT_AUDITED |
| H07 | P2 | Sync accuracy/latency must be measured, not asserted. | NOT_AUDITED |
| H08 | P1 | Strobe safety limits and explicit consent must be enforced. | CONFIRMED_OPEN — browser strobe authorization branch contains no enforcing logic. |

## I. Recorder and skills

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| I01 | P1 | Skill selection must be deterministic enough to reject weak/ambiguous matches. | NOT_AUDITED |
| I02 | P1 | Replay must verify skill preconditions against current live state. | NOT_AUDITED |
| I03 | P1 | Skill postconditions must be independently verified before recording success. | NOT_AUDITED |
| I04 | P1 | Selectors must be versioned, validated, and re-resolved before actions. | NOT_AUDITED |
| I05 | P2 | Skill schema versioning and migration must be tested. | NOT_AUDITED |
| I06 | P0 | Learned/recorded skills must not bypass approval, permission or risk checks. | PARTIALLY_IMPROVED — approval gate exists, but runtime enforcement across skills is unproven. |

## J. Remote control and security

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| J01 | P0 | No hardcoded reusable remote token; pairing secret must be session-bound and never logged. | PARTIALLY_IMPROVED — recent code masks public telemetry and generates session PIN; full lifecycle and call sites not audited. |
| J02 | P0 | Remote tasks must pass through the same safety, permission and verification pipeline. | NOT_AUDITED |
| J03 | P1 | Remote listener must be loopback-only by default and non-loopback mode tightly controlled. | PARTIALLY_IMPROVED — loopback default added; non-loopback configuration and exposure tests required. |
| J04 | P1 | Adversarial tests must cover brute force, malformed requests, origins and oversized bodies. | PARTIALLY_IMPROVED — rate limit/bounded parser source changes exist; test evidence not reviewed. |
| J05 | P1 | Shutdown/session expiry must close sockets and invalidate tokens/jobs. | NOT_AUDITED |
| J06 | P1 | Telemetry/logging must redact secrets and sensitive screen content. | PARTIALLY_IMPROVED — PIN masking and privacy changes exist; full path audit required. |

## K. Resource management and concurrency

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| K01 | P1 | Hardware managers must dispose listeners, scopes, streams and jobs. | CONFIRMED_OPEN — reviewed native manager registers receivers/sensors and owns a scope; matching disposal was not found in reviewed class. |
| K02 | P0 | Coroutines/jobs/loops must have clear parent ownership and cancellation. | NOT_AUDITED — browser emergency stop clears locks without proving in-flight cancellation. |
| K03 | P1 | Every network/hardware/action operation needs bounded timeout and typed timeout outcome. | PARTIALLY_IMPROVED — web research has timeouts; hardware/action coverage unproven. |
| K04 | P1 | Cancellation tests must prove no follow-up actions occur. | NOT_AUDITED |
| K05 | P1 | Low-memory behaviour must be measured on target device, not only calculated. | NOT_AUDITED |
| K06 | P1 | Concurrency must prevent conflicting operations, including stop/start races. | PARTIALLY_IMPROVED — browser actuator locks exist; lock clearing is not cancellation and race tests are weak. |
| K07 | P2 | Logs must be structured, correlated and redact sensitive content. | PARTIALLY_IMPROVED — structured result types exist in browser controller; complete correlation and redaction unproven. |

## L. UI, build, diagnostics and release claims

| ID | Pri | Issue / acceptance criterion | Initial status |
|---|---|---|---|
| L01 | P0 | Every dashboard status/claim must derive from real runtime evidence or be labelled simulation. | CONFIRMED_OPEN — fixed benchmark/vault values and simulated optical choreography remain. |
| L02 | P2 | Preset chips must distinguish selection from actual runtime configuration. | CONFIRMED_OPEN — preset/profile selection is not evidence of model loading/governor configuration. |
| L03 | P0 | Offline self-check must report only checks actually executed and passed. | NOT_AUDITED |
| L04 | P1 | Every button/command must map to an actual handler and typed result. | NOT_AUDITED |
| L05 | P1 | Android API/SDK compatibility claims must match manifest, code guards and device test matrix. | NOT_AUDITED — README claims API 26–36+, while repository history includes API 37 claims; reconcile before release. |
| L06 | P1 | Permissions matrix must document runtime permissions and OS restrictions by API level. | NOT_AUDITED |
| L07 | P2 | Performance, memory, allocation and battery claims must be benchmarked. | CONFIRMED_OPEN — fixed UI benchmark messages are not backed by measured results in reviewed path. |
| L08 | P1 | Release acceptance suite must include E2E/device scenarios and final evidence. | NOT_AUDITED — no physical-device evidence reviewed. |

## Immediate priority queue

1. **A02/A03/L01/C02:** eliminate simulated success and separate requested state from observed/verified state.
2. **F01/F03/F05/F07/F08:** remove remaining hardcoded fallbacks and stop treating browser telemetry as native device truth.
3. **B01/B02/B05/I06/J02:** prove approval/safety/stop enforcement across every entry point.
4. **C05/C07/C08/H08/K01/K02:** fix actuator verification, strobe authorization, cancellation and cleanup.
5. **A01/G01/E03/E10:** prove the end-to-end Calculator acceptance scenario and plan lineage.
6. **D01/D02/D05:** standardize network-radio outcomes and independent state refresh.
7. Then continue through remaining P1/P2 issues in dependency order.

## Reference repositories to inspect (read-only)

- `iam816anonymous-commits/Mobile-muse-`: command/result/verification contracts, unified dispatcher, observation and target resolution, permission registry.
- `iam816anonymous-commits/mobile_agent`: Android accessibility, planner/reflection, OCR and memory boundaries.
- `iam816anonymous-commits/skill-agent`: skill manifests, capability authorization, lifecycle and checkpoint recovery.
- `iam816anonymous-commits/autonomous_software_agent`: checkpoint/recovery, reflection, telemetry and chaos evaluation concepts.
- `iam816anonymous-commits/open-knowledge-base`: provenance-aware persistent knowledge (later, after core execution works).
- `iam816anonymous-commits/Browser-`: remote/web research security and timeout patterns only if those subsystems are in scope.

For each proposed reuse, add exact source path/symbol, commit SHA, relevant test, compatibility analysis, and decision (reuse concept / port minimal implementation / reject). Do not copy modules wholesale or change reference repositories.

## Change log

- Initial review created 2026-10-25 based on source snippets, recent commit diffs, and README-level reference review.
- Latest reviewed main commit includes nullable telemetry/storage changes and a browser unified hardware controller. These improvements are recorded as partial or unverified where acceptance evidence is missing.
- Update status only after focused tests and required runtime/device evidence are recorded.
