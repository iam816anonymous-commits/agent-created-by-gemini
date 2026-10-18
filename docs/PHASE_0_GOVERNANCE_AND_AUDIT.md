# PHASE 0 — GOVERNANCE, SPECIFICATIONS, AND REFERENCE AUDIT
**Authoritative LocalAgent Architectural Specification & Traceability Matrix**
**Document Version:** 1.0.0  
**Status:** DELIVERED FOR OWNER EXIT-GATE REVIEW (Phase 0)

---

## 1. Phase-Scope Governance Rules

The development of LocalAgent is governed by strict sequential gates. These rules prevent scope creep, unverified claims, and premature feature implementation:

1. **Sequential Execution Only:** Phases 0 through 22 represent distinct developmental gates. Implementation must proceed strictly phase-by-phase.
2. **Phase 0 Exit Gate Prerequisite:** Implementation of code features for subsequent phases requires explicit approval of the Phase 0 specifications and reference audit.
3. **Boundaries & Exclusions:** Each phase must declare its allowed files, interface boundaries, dependencies, and exclusions prior to code changes.
4. **Three-Tier Evidence Discipline:**
   - **Source Evidence:** Code or configuration exists in the repository.
   - **Test Evidence:** Unit or instrumentation tests executed with logged automated outcomes.
   - **Physical-Device Evidence:** Tests executed on physical Android hardware (e.g. TECNO Camon i Click, Android 8.1 API 27) with documented logs (`adb shell dumpsys meminfo`, `logcat`). If a test was not run on hardware, it must remain explicitly labeled **NOT VERIFIED**.
5. **No Speculative Model Assumptions:** Local SLM inference is treated as strictly optional until empirical on-device memory and latency measurements justify model loading.
6. **No Destructive External Interference:** The agent must never terminate third-party processes (`killBackgroundProcesses`) to satisfy memory constraints.

---

## 2. Complete Repository Inventory

### 2.1 Native Android Application (`/app`)
- **Build & Configuration:**
  - `app/build.gradle.kts`: Android application build script (compileSdk 36, minSdk 24, targetSdk 36).
  - `build.gradle.kts`: Root Gradle build configuration.
  - `settings.gradle.kts`: Project module configuration.
  - `gradle/libs.versions.toml`: Version catalog (AGP 9.1.1, Kotlin 2.2.10, Room 2.7.0, Compose BOM).
- **Manifest & Permissions:**
  - `app/src/main/AndroidManifest.xml`: Declares `LocalAgentAccessibilityService`, `MainActivity`, `<queries>` for launcher intents, and fine-grained permissions.
- **Core Architecture (`app/src/main/java/com/example/core`):**
  - `actuator/`: Hardware bridges (Camera Torch, Haptics, Audio Tone, System Settings Panels).
  - `approval/TaskApprovalGate.kt`: Human-in-the-loop task execution proposal and approval gate.
  - `diagnostics/`: System metrics and diagnostic loggers.
  - `discovery/AppDiscoveryManager.kt`: Dynamic launcher resolution via `PackageManager.queryIntentActivities` with zero hardcoded package assumptions.
  - `engine/`: Multi-step orchestrator, execution timeline, and autonomous recovery engine.
  - `learning/LearningManager.kt`: Structured procedural skill lifecycle backed by Room SQLite persistence.
  - `memory/MemoryBudgetManager.kt`: Strict 64 MB heap and 50 MB storage boundary manager.
  - `model/`: Domain models (`TaskCommand`, `UiSnapshot`, `UiNode`, `TargetSelector`, `ExecutionStatus`).
  - `parser/NaturalLanguageCommandParser.kt`: Regex and token-based deterministic intent parser.
  - `recorder/`: Visual interaction recording and task synthesizer.
  - `research/WebResearchEngine.kt`: Zero-login external web queries and structured fact extractor.
  - `resolver/TargetResolver.kt`: Freshness-checked accessibility node resolver with multi-attribute scoring.
  - `verifier/StepVerifier.kt`: Multi-modal independent post-action verifier with staleness detection.
- **Data & Persistence Layer (`app/src/main/java/com/example/data`):**
  - `local/LocalAgentDatabase.kt`: Room Database (v5) with entities `task_executions`, `step_records`, `diagnostic_logs`, `synthesized_tasks`, `skills`, and `learning_records`. Includes non-destructive `MIGRATION_4_5`.
  - `storage/DurableMemoryStorageManager.kt`: Public storage / SAF JSON export and import manager.
- **Native UI Layer (`app/src/main/java/com/example/ui`):**
  - `MainActivity.kt`: Hardware volume-up kill switch listener and edge-to-edge Compose container.
  - `AgentViewModel.kt`: Coroutine-driven state orchestrator.
  - `MainScreen.kt` & `screens/`: Dashboard, Device Actuators, History, Snapshot Gallery, and Timeline.
- **Automated Test Suite (`app/src/test` and `app/src/androidTest`):**
  - `app/src/test/java/com/example/core/CoreAutonomousAgentTest.kt`: Regression tests for parser, resolver, verifier, synthesizer, and kill-switch logic.
  - `app/src/test/java/com/example/core/LearningPersistenceTest.kt`: Unit tests for Room SQLite persistence, approval vs verification separation, process-restart recovery, and migration integrity.
  - `app/src/test/java/com/example/ExampleRobolectricTest.kt`: Context string extraction test.
  - `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`: Instrumented context test.

### 2.2 Web Console & Interactive Dashboard (`/src`)
- `src/App.tsx`: J.A.R.V.I.S.-styled OLED dashboard, Authoritative 23-Phase Governance architecture viewer, live device viewport simulation, diagnostic terminal, and hardware control matrix.
- `src/main.tsx`, `index.html`: Vite entry points.

---

## 3. Architecture Boundaries & Interface Contracts

```
+-------------------------------------------------------------------------+
|                              PRESENTATION LAYER                         |
|   Native Jetpack Compose UI (Phase 9) / Web Supervisor Console (Phase 9) |
+------------------------------------+------------------------------------+
                                     |
                                     v
+------------------------------------+------------------------------------+
|                       PRE-FLIGHT APPROVAL GATE                          |
|    Phase 13: TaskApprovalGate (Human Confirmation Before Execution)     |
+------------------------------------+------------------------------------+
                                     |
                                     v
+------------------------------------+------------------------------------+
|                          ORCHESTRATION ENGINE                           |
|   Phase 8: Multi-Step Execution Pipeline & Target Resolution            |
|   Phase 16: Autonomous Recovery & Emergency Kill-Switch Watchdog        |
+-------------------+--------------------------------+--------------------+
                    |                                |
                    v                                v
+-------------------+----------------+  +------------+--------------------+
|      ACCESSIBILITY & HARDWARE      |  |        PERSISTENCE & MEMORY         |
|   Phase 6: Accessibility Service   |  |   Phase 3: Room Database (v5)       |
|   Phase 7: Bounded Snapshot Engine |  |   Phase 15: Persistent Learning     |
|   Phase 17: Hardware Actuators     |  |   Phase 20: Memory Budget (<64MB)   |
+------------------------------------+  +---------------------------------+
```

### 3.1 Interface Contracts
1. **Command Normalization Contract:** `NaturalLanguageCommandParser.parse(prompt: String): List<TaskCommand>`. Guarantees deterministic, typed commands or explicit unsupported results.
2. **Target Resolution Contract:** `TargetResolver.resolve(selector: TargetSelector, snapshot: UiSnapshot?, currentTimeMs: Long): ResolutionResult`. Rejects stale snapshots (>4000 ms) and provides scored candidates.
3. **Independent Verification Contract:** `StepVerifier.verify(...)`. Never treats action dispatch as success; requires independent post-action hierarchy observation.
4. **Learning Lifecycle Contract:** `LearningManager.processReview(skillId, action)`. User approval does NOT automatically confer technical verification; transitions to `VERIFIED` only if `successfulExecutions >= 2` and `failedValidations == 0`.

---

## 4. Security, Isolation, and Safety Model

1. **Permission Minimization:** Only permissions required for accessibility observation, launcher querying, and authorized actuators are declared.
2. **Strict Accessibility Boundary:** Accessibility node capture is constrained:
   - Maximum traversal depth: 8 levels.
   - Maximum node count: 150 nodes.
   - Password fields and sensitive user input are masked.
3. **Physical Kill Switch:** Two presses of the hardware Volume-Up button within 650 ms instantly dispatches an emergency abort, halting all action execution and releasing hardware resources.
4. **Data Isolation:** Primary learning records, credentials, and execution logs reside exclusively in app-private SQLite storage (`/data/user/0/com.aistudio.localagent.tcin/databases/local_agent.db`). External storage writes occur only upon explicit user export via the Storage Access Framework (SAF).

---

## 5. API Compatibility Matrix

| Android Version | API Level | Package Visibility Strategy | Wi-Fi Actuator Strategy | Bluetooth Actuator Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **Android 8.1 (Target HW)** | API 27 | Standard `pm.queryIntentActivities` | `WifiManager.setWifiEnabled()` direct or Settings fallback | Standard BluetoothAdapter permissions |
| **Android 9.0** | API 28 | Standard `pm.queryIntentActivities` | Direct toggle permitted with system permissions | Direct toggle permitted |
| **Android 10.0** | API 29 | Manifest `<queries>` recommended | `Settings.Panel.ACTION_WIFI` (Direct toggle restricted) | Direct toggle restricted |
| **Android 11.0 – 13.0** | API 30–33 | `<queries>` with `ACTION_MAIN` + `CATEGORY_LAUNCHER` | `Settings.Panel.ACTION_WIFI` | `BLUETOOTH_CONNECT` runtime permission |
| **Android 14.0+** | API 34+ | `<queries>` + fine-grained component filtering | Settings Panel / ConnectivityManager | `BLUETOOTH_CONNECT` + foreground service type |

---

## 6. Resource and Memory Budget Policy

- **Target Physical Device Profile:** TECNO Camon i Click (MediaTek Helio P23, 4 GB RAM, 64 GB Storage, Android 8.1 / API 27).
- **Core Agent Process Budget:**
  - Maximum JVM Heap Allocation: **64.0 MB** (`MAX_AGENT_HEAP_MB`).
  - Active UI Node Snapshot Cache: Maximum **5 snapshots** in memory.
  - Persistent SQLite Database Budget: Maximum **50.0 MB** (`MAX_PERSISTENT_STORAGE_KB = 51200L`).
  - Background Service Wake-Locks: Zero perpetual partial wake-locks; execution relies strictly on transient foreground service triggers.
- **Rule on External Processes:** The agent strictly forbids calling `killBackgroundProcesses` on unrelated apps to artificially free RAM.
- **On-Device SLM Policy:** Model binaries (e.g., Gemma 2B or Qwen 1.5B) require ~1.2 GB to 1.8 GB RAM. On a 4 GB RAM device with Android OS taking ~2.2 GB, on-device SLM execution is classified as **OPTIONAL / CONTINGENT** upon empirical PSS/RSS profiling. Basic operation must function 100% deterministically without requiring an SLM.

---

## 7. Skill Definition and Lifecycle Model

```
   [ OBSERVED ] (Raw recorded interactions from user or agent)
         |
         v
   [ CANDIDATE ] (Normalized & synthesized procedural workflow)
         |
         v
 [ PENDING_REVIEW ] (Stored in Room SQLite learning_records; awaiting supervisor)
         |
    +----+-----------------------+
    |                            |
    | (ReviewAction.APPROVE)     | (ReviewAction.REJECT)
    v                            v
 [ APPROVED ]               [ REJECTED ]
    | (If successfulExecutions >= 2 & failedValidations == 0)
    +----------------------------+
    |                            |
    v                            v
[ VERIFIED ]             [ NEEDS_MORE_EVIDENCE ] (ReviewAction.TEST_FIRST)
    |
    v
[ AVAILABLE_FOR_REUSE ]
```

---

## 8. Authoritative 23-Phase Roadmap & Traceability Matrix

| Phase | Phase Name | Purpose & Primary Deliverables | Status |
| :---: | :--- | :--- | :---: |
| **Phase 0** | **Governance, Specifications and Reference Audit** | Project rules, specifications, repository inventory, evidence audit, and traceability matrix. | **DELIVERED (EXIT GATE)** |
| **Phase 1** | **Project Foundation and Build Integrity** | Reproducible Gradle configuration, version catalog, baseline automated test harness, and build scripts. | READY FOR GATE |
| **Phase 2** | **Core Domain Models and Contracts** | Deterministic data models (`TaskCommand`, `UiSnapshot`, `UiNode`, typed outcomes, state transitions). | READY FOR GATE |
| **Phase 3** | **Local Persistence and Execution Records** | Room SQLite schema (v5), non-destructive `MIGRATION_4_5`, restart recovery, and audit records. | IMPLEMENTED (Awaiting Gate) |
| **Phase 4** | **Command Parsing and Normalization** | Natural-language parser, deterministic regex tokenizer, command validation, ambiguity handling. | IMPLEMENTED (Awaiting Gate) |
| **Phase 5** | **Permission and Capability Management** | Runtime feature detection, Android version adaptation, and permission inspection. | IMPLEMENTED (Awaiting Gate) |
| **Phase 6** | **Accessibility Service and Lifecycle** | `LocalAgentAccessibilityService`, window transition handling, and lifecycle resource management. | IMPLEMENTED (Awaiting Gate) |
| **Phase 7** | **Observation and Snapshot Engine** | Fresh bounded node hierarchy traversal, snapshot differencing, and staleness detection. | IMPLEMENTED (Awaiting Gate) |
| **Phase 8** | **Target Resolution, App Control, Action Execution and Verification** | Dynamic launcher package discovery, scored target resolution, action dispatch, and independent verification. | IMPLEMENTED (Awaiting Gate) |
| **Phase 9** | **Console UI** | Live agent dashboard, execution timeline, diagnostics view, and real-time capability controls. | IMPLEMENTED (Awaiting Gate) |
| **Phase 10** | **Test Center** | Unit, integration, and regression reporting with clear separation of simulated vs hardware results. | READY FOR GATE |
| **Phase 11** | **Overlay and In-Context Interaction** | Optional floating supervisor HUD with permission checks and safe non-intrusive controls. | READY FOR GATE |
| **Phase 12** | **Skill Registry and Skill Contracts** | Reusable skill schemas, preconditions, postconditions, and distinct verification strategies. | READY FOR GATE |
| **Phase 13** | **Task Planning and Human Approval** | Human-in-the-loop task approval gate (`TaskApprovalGate`), plan summaries, and risk ratings. | IMPLEMENTED (Awaiting Gate) |
| **Phase 14** | **Learning Candidate Generation** | Extraction of reusable procedures and useful facts from completed, evidence-backed executions. | IMPLEMENTED (Awaiting Gate) |
| **Phase 15** | **Persistent Knowledge, Review and Retrieval** | Room-backed `LearningManager`, persistent pending queue, approval workflow, and restart survival. | IMPLEMENTED (Awaiting Gate) |
| **Phase 16** | **Recovery, Reliability and Failure Learning** | Hardware volume-up kill switch, typed failure recovery, and setting panel fallbacks. | IMPLEMENTED (Awaiting Gate) |
| **Phase 17** | **Hardware and System Capability Skills** | Front/Back/Dual Flashlight with Camera HAL watchdog, haptics, and system settings panel bridges. | IMPLEMENTED (Awaiting Gate) |
| **Phase 18** | **Optional AI and Web Research Integration** | Zero-login web scraping and external research integration without hard offline dependency. | IMPLEMENTED (Awaiting Gate) |
| **Phase 19** | **Interaction Recording and Skill Synthesis** | User interaction recorder, gesture-to-command synthesizer, and payload encoding. | IMPLEMENTED (Awaiting Gate) |
| **Phase 20** | **Resource Management and Performance** | Strict 64 MB heap policy, 50 MB storage quota enforcement, and OLED ECO low-power mode. | IMPLEMENTED (Awaiting Gate) |
| **Phase 21** | **Security, Privacy and Operational Controls** | Sensitive data masking, permission minimization, and emergency hardware disconnect. | IMPLEMENTED (Awaiting Gate) |
| **Phase 22** | **End-to-End Validation and Release Readiness** | Full regression suite, dynamic package resolution validation, and hardware release checklist. | READY FOR GATE |

---

## 9. Phase 0 Audit Summary & Exit-Gate Findings

1. **Discrepancies Resolved:** Phase numbers and names have been fully synchronized between code, test models, dashboard UI, and the authoritative roadmap.
2. **Learning Persistence Hardened:** Volatile in-memory candidates have been migrated to Room SQLite (`learning_records` table) under non-destructive `MIGRATION_4_5`, ensuring pending reviews survive process restarts.
3. **Approval vs Verification Invariant Enforced:** Domain logic ensures user approval does not automatically confer technical verification.
4. **Evidence Classification Verified:** All claims regarding physical-device performance (dual flash concurrency, OLED power savings, PSS/RSS continuous footprint) remain classified as **NOT VERIFIED** until physical hardware test logs are captured.
5. **Phase 0 Status:** **DELIVERED**. Phase 0 specifications, boundaries, and traceability are complete and submitted for owner review. Implementation of subsequent phases proceeds upon approval of this exit gate.
