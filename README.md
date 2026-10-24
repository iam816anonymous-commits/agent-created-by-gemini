# LocalAgent J.A.R.V.I.S. • Autonomous Android Agent & Supervisory Dashboard

An autonomous, low-overhead mobile accessibility agent designed to operate offline-first across Android 8.0 to Android 16 (API 26–36+).

## Architecture Highlights

1. **Accessibility Service Core (`app/src/main/java/com/example/service/`)**
   - High-efficiency `LocalAgentAccessibilityService` listening to window transitions and user intents.
   - Dual-event debouncing and physical Volume-Up emergency kill switch (<650ms dual tap).
   - Zero-allocation node tree inspection and differential state comparison (`AccessibilityNodeDiffer.kt` with 64-bit FNV-1a hashing).

2. **Hierarchical Intent Routing (`com.example.core.intent.HierarchicalIntentRouter`)**
   - **Tier 1: Deterministic Fast-Path:** Core device actions (flashlight, calculator, keep notes, timer) execute in <5ms with 0 byte GC allocation.
   - **Tier 2: On-Demand Local SLM (`LlamaAndroidAdapter` & `LlamaNativeBridge`):** Interfaces with native `libllama-bridge.so` using GBNF grammar constraints for sub-1B models (SmolLM2-360M, Qwen2.5-0.5B). Enforces a strict 500MB memory ceiling.
   - **Tier 3: Online Cloud Brain (`CognitiveBrainEngine`):** Connects to the Gemini 2.5 Flash API with active `ConnectivityManager` network presence verification. User API keys are stored in the hardware-backed `AndroidKeyStore` (AES-256-GCM).
   - **Tier 4: Honest Degraded Offline:** When disconnected or unresolvable, returns an explicit degraded state. Zero fabrication policy: never hallucinates fake execution steps or fabricated online scraping.

3. **Human-in-the-Loop Safety Gate (`TaskApprovalGate.kt`)**
   - Decomposes tasks into structured AST operations rather than naive substring matching.
   - Prevents words like "shopping", "spinning", or "pinching" from falsely triggering credential modification alarms while reliably gating actual PIN, password, payment, and factory wipe actions.

4. **Hardened Local Remote Bridge (`RemoteAgentBridge.kt`)**
   - Embedded HTTP listener on port 8080 for local teleoperation.
   - Ephemeral 6-digit `SecureRandom` PIN generated per session and displayed only on the physical device screen.
   - Constant-time verification (`MessageDigest.isEqual`) preventing timing side-channels.
   - Strict 64 KB request buffer ceiling (`MAX_REQUEST_BODY_BYTES = 65536`) to protect low-RAM devices from memory exhaustion.
   - Strict localhost / loopback CORS policy (wildcard `*` dropped).

5. **Honest Web Research (`WebResearchEngine.kt`)**
   - Executes real HTTP queries with strict timeouts.
   - Explicitly separates live network scrapes from local SQLite cache. Returns honest offline failure status when network is unreachable.

---

## Getting Started

### Prerequisites
- **Android:** Android Studio Jellyfish or newer, JDK 17, Android SDK Platform 34.
- **Dashboard:** Node.js 18+, npm.

### Connecting the Dashboard to Device
1. Enable USB Debugging on your device.
2. Forward the local bridge port:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```
3. Start the React dashboard:
   ```bash
   npm run dev
   ```
4. Enter the 6-digit PIN displayed on your device screen to authorize the session.
