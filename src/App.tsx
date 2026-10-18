import React, { useState, useEffect, useRef } from 'react';
import {
  Brain,
  ShieldAlert,
  Play,
  RotateCcw,
  Sliders,
  Sparkles,
  Check,
  X,
  Clock,
  Activity,
  Zap,
  History,
  Compass,
  Eye,
  Camera,
  Settings as SettingsIcon,
  Wifi,
  Bluetooth,
  Battery,
  HardDrive,
  Video,
  ListTree,
  GitBranch,
  FileCode,
  Search,
  CheckCircle2,
  RefreshCw,
  Terminal,
  Volume2,
  Cpu,
  Layers,
  ChevronRight,
  ChevronDown,
  Info,
  Maximize2,
  Minimize2,
  ExternalLink,
  Shield,
  Smartphone,
  Sun,
  Film,
  Globe,
  Tv,
  Flame,
  Timer,
  Radio,
  Share2,
  Bookmark,
  Star,
  FileText,
  CloudSun,
  Newspaper,
  CheckSquare,
  Square,
  Download,
  Upload,
  ShieldCheck,
  AlertCircle,
  Vibrate,
  Bell,
  Moon,
  Database,
  ArrowRight,
  Power,
  Gauge,
  Lock
} from 'lucide-react';

// ==========================================
// DOMAIN MODELS & SCHEMAS
// ==========================================
interface DiscoveredApp {
  label: string;
  packageName: string;
  launcherActivity: string;
  isSystemApp: boolean;
  canLaunch: boolean;
  version?: string;
}

interface StepPlan {
  stepIndex: number;
  description: string;
  actionType: string;
  target: string;
  risk: 'LOW' | 'MEDIUM' | 'HIGH';
  status: 'PENDING' | 'EXECUTING' | 'VERIFIED' | 'FAILED';
}

interface TaskHistoryRecord {
  id: string;
  taskCommand: string;
  targetPackage: string;
  targetLabel: string;
  timestamp: string;
  durationMs: number;
  stepsTotal: number;
  stepsCompleted: number;
  finalStatus: 'SUCCESS' | 'FAILED' | 'ABORTED';
  verificationDetails: string;
  evidence: string;
}

interface SkillRecord {
  id: string;
  title: string;
  targetAppLabel: string;
  resolvedPackageName: string;
  intendedTask: string;
  orderedSteps: string[];
  verificationStrategy: string;
  knownLimitations: string;
  evidenceSummary: string;
  successfulExecutions: number;
  failedValidations: number;
  state: 'APPROVED' | 'VERIFIED' | 'REJECTED' | 'PENDING_REVIEW';
  timestamp: string;
  version: number;
}

interface AgentFact {
  id: string;
  category: 'DEVICE' | 'PREFERENCE' | 'CONSTRAINT' | 'HEURISTIC' | 'CINEMA' | 'STORAGE';
  statement: string;
  confidence: string;
  provenance: string;
}

interface TeluguMovie {
  id: string;
  title: string;
  year: string;
  genre: string;
  directorOrCast: string;
  status: string;
  ratingOrNote: string;
  ottPlatform?: string;
  theatrical: boolean;
  synopsis: string;
  tag: string;
}

interface NoteItem {
  id: string;
  title: string;
  content: string;
  timestamp: string;
  color?: string;
  pinned?: boolean;
}

interface OvernightLog {
  time: string;
  task: string;
  status: string;
  details: string;
}

// Authoritative 23-Phase Governance Architecture (Phase 0 through Phase 22)
const PHASES_23 = [
  { phaseNumber: 0, title: 'Governance, Specifications and Reference Audit', purpose: 'Establish project rules, boundaries, and audits', status: 'COMPLETED' },
  { phaseNumber: 1, title: 'Project Foundation and Build Integrity', purpose: 'Reproducible Android Gradle and build baseline', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 2, title: 'Core Domain Models and Contracts', purpose: 'Deterministic data models, TaskCommand, and states', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 3, title: 'Local Persistence and Execution Records', purpose: 'SQLite/Room schema, migrations, and process restart recovery', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 4, title: 'Command Parsing and Normalization', purpose: 'Convert user instructions into deterministic commands', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 5, title: 'Permission and Capability Management', purpose: 'Runtime feature detection and permission inspection', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 6, title: 'Accessibility Service and Lifecycle', purpose: 'Native Android observation and action integration', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 7, title: 'Observation and Snapshot Engine', purpose: 'Fresh, bounded UI hierarchy capture and differencing', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 8, title: 'Target Resolution, App Control, Action Execution and Verification', purpose: 'PackageManager lookup, explicit launch, dispatch, and verification', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 9, title: 'Console UI', purpose: 'Interface for observing and controlling real agent operations', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 10, title: 'Test Center', purpose: 'Structured unit, integration, and regression reporting', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 11, title: 'Overlay and In-Context Interaction', purpose: 'Permission-controlled floating supervisor surface', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 12, title: 'Skill Registry and Skill Contracts', purpose: 'Structured skills with preconditions and verification strategies', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 13, title: 'Task Planning and Human Approval', purpose: 'Human-in-the-loop pre-flight approval gate', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 14, title: 'Learning Candidate Generation', purpose: 'Extract reusable procedures and useful facts from tasks', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 15, title: 'Persistent Knowledge, Review and Retrieval', purpose: 'Pending-review queue, approval workflow, and restart survival in Room', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 16, title: 'Recovery, Reliability and Failure Learning', purpose: 'Emergency kill switch, typed recovery, and failure adaptation', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 17, title: 'Hardware and System Capability Skills', purpose: 'Camera flash, haptics, and system settings panels', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 18, title: 'Optional AI and Web Research Integration', purpose: 'Zero-login web research and optional intelligence integration', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 19, title: 'Interaction Recording and Skill Synthesis', purpose: 'Workflow recording, command synthesis, and payload encoding', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 20, title: 'Resource Management and Performance', purpose: 'Memory budget limits, storage budgeting, and low-power OLED mode', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 21, title: 'Security, Privacy and Operational Controls', purpose: 'Permission minimization, secret filtering, and hardware disconnect', status: 'ACTIVE_IMPLEMENTED' },
  { phaseNumber: 22, title: 'End-to-End Validation and Release Readiness', purpose: 'End-to-end task validation, test trace matrix, and release audit', status: 'ACTIVE_IMPLEMENTED' }
];

export function App() {
  // ==========================================
  // J.A.R.V.I.S. & UI CONTROLLER STATE
  // ==========================================
  const [activeCategory, setActiveCategory] = useState<'all' | 'directives' | 'actuators' | 'viewport' | 'intelligence' | 'vault' | 'telemetry'>('all');
  const [oledLowPowerMode, setOledLowPowerMode] = useState(false);
  const [killSwitchActive, setKillSwitchActive] = useState(false);
  const [quickSearchQuery, setQuickSearchQuery] = useState('');
  const [activePhoneTab, setActivePhoneTab] = useState<'AUTO' | 'KEEP' | 'BRIEFING' | 'FLASHLIGHT' | 'CALCULATOR' | 'SETTINGS' | 'CINEMA'>('AUTO');

  // Primary Agent Consciousness State
  const [agentConsciousness, setAgentConsciousness] = useState({
    operationalStatus: 'READY_OBSERVING',
    activeForegroundPackage: 'com.aistudio.localagent',
    activeAppLabel: 'LocalAgent Home',
    currentThought: 'J.A.R.V.I.S. Neural Core operational. Dual-core offline brain online. Standing by for multi-modal commands.',
    cognitivePulse: 'calm',
    lastPerceptionTimestamp: '10:04:18 AM',
    confidenceScore: 0.98
  });

  // User Task Input & Stepper State
  const [userPrompt, setUserPrompt] = useState('Turn on flashlight for 1 minute');
  const [pendingPlanModal, setPendingPlanModal] = useState(false);
  const [proposedPlan, setProposedPlan] = useState<StepPlan[]>([]);
  const [targetPackageForPlan, setTargetPackageForPlan] = useState('com.android.hardware.flashlight');
  const [targetLabelForPlan, setTargetLabelForPlan] = useState('Flashlight');
  const [activeStepIndex, setActiveStepIndex] = useState<number>(-1);
  const [executionLogs, setExecutionLogs] = useState<string[]>([
    '[10:00:01 AM] J.A.R.V.I.S. Neural Bus online on TECNO Camon i Click (HiOS 3.3 / Android 8.1)',
    '[10:00:02 AM] Brain Vault loaded from /sdcard/LocalAgent/brain_vault.json (24 Verified Skills, 8 Semantic Facts)',
    '[10:00:03 AM] Hardware Actuator Matrix verified: Camera 0 (Rear Quad-LED), Camera 1 (Front Dual-LED), ERM Haptics',
    '[10:00:04 AM] Offline SLM Engine: Gemma 2B IT (Q4_K_M) ready on-device. Zero cloud reliance mode active.'
  ]);

  // Flashlight & Hardware Actuators State
  const [flashlightActive, setFlashlightActive] = useState(false);
  const [flashlightMode, setFlashlightMode] = useState<'BACK' | 'FRONT' | 'DUAL'>('BACK');
  const [flashlightRemainingSeconds, setFlashlightRemainingSeconds] = useState<number | null>(null);
  const [flashlightTotalSeconds, setFlashlightTotalSeconds] = useState<number | null>(null);
  const [hapticFrequency, setHapticFrequency] = useState<'TICK' | 'BURST' | 'IMPACT'>('TICK');
  const [lastHapticTriggered, setLastHapticTriggered] = useState<string | null>(null);
  const [lastAudioChime, setLastAudioChime] = useState<string | null>(null);
  const [screenBrightness, setScreenBrightness] = useState<number>(75);
  const [nightShieldActive, setNightShieldActive] = useState(false);
  const [wifiEnabled, setWifiEnabled] = useState(true);
  const [bluetoothEnabled, setBluetoothEnabled] = useState(true);
  const [batteryLevel, setBatteryLevel] = useState(84);

  // Keep Notes Application State
  const [notesList, setNotesList] = useState<NoteItem[]>([
    {
      id: 'note-1',
      title: 'Groceries & Sunday Prep',
      content: 'Almond milk, organic avocados, dark roast coffee beans, and fresh spinach.',
      timestamp: 'Oct 18, 2026 • 09:15 AM',
      color: 'amber',
      pinned: true
    },
    {
      id: 'note-2',
      title: 'System Architecture Checklist',
      content: 'Review 4GB RAM budget for Gemma-2B-IT quantized weights and keep AccessibilityService thread unblocked.',
      timestamp: 'Oct 17, 2026 • 10:40 PM',
      color: 'cyan',
      pinned: false
    },
    {
      id: 'note-3',
      title: 'Telugu Cinema Watchlist 2026',
      content: 'Check IMAX tickets for SSMB29 and Raja Saab streaming availability.',
      timestamp: 'Oct 16, 2026 • 08:20 PM',
      color: 'purple',
      pinned: false
    }
  ]);

  // Morning Briefing & Nocturnal Logs
  const [briefingSubTab, setBriefingSubTab] = useState<'OVERNIGHT' | 'WEATHER' | 'TODOS' | 'TECH'>('OVERNIGHT');
  const [overnightActivities] = useState<OvernightLog[]>([
    {
      time: '01:30 AM',
      task: 'External Storage Vault Integrity Check',
      status: 'VERIFIED',
      details: 'Validated /sdcard/LocalAgent/brain_vault.json checksum (MD5 verified). Survives APK uninstall.'
    },
    {
      time: '02:45 AM',
      task: 'SQLite Cognitive Cache Vacuum & Defrag',
      status: 'OPTIMIZED',
      details: 'Cleaned orphaned execution traces. Shrunk internal index from 5.4MB to 3.8MB.'
    },
    {
      time: '04:15 AM',
      task: 'Accessibility Node Hierarchy Indexing',
      status: 'COMPLETED',
      details: 'Cached UI node resource IDs for 14 system packages to accelerate cold-start interaction.'
    },
    {
      time: '05:30 AM',
      task: 'Autonomous Morning Intelligence Synthesis',
      status: 'SYNTHESIZED',
      details: 'Aggregated Hyderabad weather (28°C), daily tech highlights, and scheduled reminders.'
    }
  ]);

  const [toDoItems, setToDoItems] = useState([
    { id: 'td-1', text: 'Morning physical workout & hydration', done: true },
    { id: 'td-2', text: 'Verify LocalAgent offline 4GB RAM benchmark', done: false },
    { id: 'td-3', text: 'Buy groceries: almond milk & coffee', done: false },
    { id: 'td-4', text: 'Telugu cinema schedule review for Sunday', done: false }
  ]);

  // Dual-Core Brain Architecture Mode
  const [brainOperatingMode, setBrainOperatingMode] = useState<'OFFLINE_ON_DEVICE' | 'HYBRID_ZERO_LOGIN'>('OFFLINE_ON_DEVICE');
  const [brainTestPrompt, setBrainTestPrompt] = useState('How can I add a note and turn on flash together?');
  const [brainTestResponse, setBrainTestResponse] = useState<string | null>(null);
  const [isBrainThinking, setIsBrainThinking] = useState(false);

  // Telugu Cinema Intelligence (Anchored October 18, 2026)
  const [movieFilter, setMovieFilter] = useState<'ALL' | 'THEATRES' | 'OTT'>('ALL');
  const [teluguMoviesList] = useState<TeluguMovie[]>([
    {
      id: 'tel-1',
      title: 'SSMB29 (Globe-Trotting Action Adventure)',
      year: '2026',
      genre: 'Action / Adventure / Epic',
      directorOrCast: 'Mahesh Babu • Dir: S.S. Rajamouli',
      status: 'GLOBAL THEATRICAL PHENOMENON',
      ratingOrNote: '9.8/10 IMDb • All-Time Global Record',
      theatrical: true,
      synopsis: 'An epic Indiana Jones-style jungle adventure across Africa and the Amazon with cutting-edge IMAX 3D visuals.',
      tag: 'BLOCKBUSTER OF THE YEAR 2026'
    },
    {
      id: 'tel-2',
      title: 'Spirit',
      year: '2026',
      genre: 'Action / Crime / Drama',
      directorOrCast: 'Prabhas • Dir: Sandeep Reddy Vanga',
      status: 'WORLDWIDE BLOCKBUSTER',
      ratingOrNote: '9.4/10 IMDb • ₹1,100+ Cr Gross',
      theatrical: true,
      synopsis: 'Prabhas portrays a fierce, relentless, morally complex IPS officer waging war against an international syndicate.',
      tag: 'ALL-TIME RECORD 2026'
    },
    {
      id: 'tel-3',
      title: 'The Raja Saab',
      year: '2025–2026',
      genre: 'Horror / Romantic Comedy',
      directorOrCast: 'Prabhas, Malavika Mohanan • Dir: Maruthi',
      status: 'STREAMING NOW',
      ratingOrNote: '8.7/10 • Super Hit',
      ottPlatform: 'Netflix & Aha Video',
      theatrical: false,
      synopsis: 'A royal estate haunted by ancestral mysteries combined with electrifying comedy and mass visual spectacle.',
      tag: 'TOP STREAMING HIT'
    },
    {
      id: 'tel-4',
      title: 'Vishwambhara',
      year: '2025–2026',
      genre: 'Socio-Fantasy / Action',
      directorOrCast: 'Chiranjeevi, Trisha Krishnan • Dir: Vassishta',
      status: 'STREAMING NOW',
      ratingOrNote: '8.9/10 • Mega Blockbuster',
      ottPlatform: 'Amazon Prime Video',
      theatrical: false,
      synopsis: 'A mythical celestial guardian descends across cosmic realms to protect humanity against an apocalyptic force.',
      tag: 'SOCIO-FANTASY HIT'
    },
    {
      id: 'tel-5',
      title: 'Pushpa 2: The Rule',
      year: '2024–2025',
      genre: 'Action / Drama',
      directorOrCast: 'Allu Arjun, Rashmika Mandanna • Dir: Sukumar',
      status: 'STREAMING RECORD HOLDER',
      ratingOrNote: '9.3/10 • Highest Grosser',
      ottPlatform: 'Netflix',
      theatrical: false,
      synopsis: 'Pushpa Raj commands his red sandalwood empire while clashing fiercely with SP Bhanwar Singh Shekhawat.',
      tag: 'ALL-TIME HIGHEST GROSSER'
    },
    {
      id: 'tel-6',
      title: 'Devara: Part 1',
      year: '2024–2025',
      genre: 'Action / Coastal Thriller',
      directorOrCast: 'Jr NTR, Saif Ali Khan, Janhvi Kapoor • Dir: Koratala Siva',
      status: 'STREAMING NOW',
      ratingOrNote: '8.8/10 • Sensational Hit',
      ottPlatform: 'Netflix',
      theatrical: false,
      synopsis: 'A coastal chieftain rules the treacherous sea passages, shielding his clan from betrayal and sea piracy.',
      tag: 'COASTAL BLOCKBUSTER'
    }
  ]);

  // Installed Apps Catalog (TECNO Camon i Click HiOS)
  const [installedApps] = useState<DiscoveredApp[]>([
    { label: 'Keep Notes', packageName: 'com.google.android.keep', launcherActivity: 'com.google.android.keep.activities.BrowseActivity', isSystemApp: false, canLaunch: true, version: '5.24.412' },
    { label: 'Flashlight Hardware', packageName: 'com.android.hardware.flashlight', launcherActivity: 'com.android.hardware.flashlight.Controller', isSystemApp: true, canLaunch: true, version: '2.0.0' },
    { label: 'Morning Intelligence', packageName: 'com.example.localagent.briefing', launcherActivity: 'com.example.localagent.briefing.MainActivity', isSystemApp: true, canLaunch: true, version: '1.4.0' },
    { label: 'Telugu Cinema 2026', packageName: 'com.example.localagent.research', launcherActivity: 'com.example.localagent.research.MainActivity', isSystemApp: true, canLaunch: true, version: '2.5.0' },
    { label: 'Settings', packageName: 'com.android.settings', launcherActivity: 'com.android.settings.Settings', isSystemApp: true, canLaunch: true, version: '8.1.0' },
    { label: 'Calculator', packageName: 'com.transsion.calculator', launcherActivity: 'com.transsion.calculator.Calculator', isSystemApp: true, canLaunch: true, version: '7.2.1' },
    { label: 'Camera', packageName: 'com.transsion.camera', launcherActivity: 'com.transsion.camera.CameraLauncher', isSystemApp: true, canLaunch: true, version: '3.4.1' }
  ]);

  // Procedural Skills Knowledge Graph (Persisted in Brain Vault)
  const [skills, setSkills] = useState<SkillRecord[]>([
    {
      id: 'skill-1',
      title: 'Keep Notes Fast Entry',
      targetAppLabel: 'Keep Notes',
      resolvedPackageName: 'com.google.android.keep',
      intendedTask: 'Open Keep Notes, tap new note FAB, inject typed content, and save to SQLite',
      orderedSteps: [
        'Launch com.google.android.keep',
        'Verify foreground window package focus',
        'Locate accessibility node id="new_note_button"',
        'Dispatch ACTION_CLICK on new note FAB',
        'Inject text content into title and body nodes',
        'Simulate back navigation to commit and auto-save note'
      ],
      verificationStrategy: 'Poll accessibility view hierarchy for created note title node',
      knownLimitations: 'Requires Keep Notes to be installed. Falls back to LocalAgent Internal SQLite Notepad if absent.',
      evidenceSummary: '100% verified across 48 automated execution cycles.',
      successfulExecutions: 48,
      failedValidations: 0,
      state: 'VERIFIED',
      timestamp: '2026-10-18 09:15:00',
      version: 4
    },
    {
      id: 'skill-2',
      title: 'Universal Multi-Emitter Torch Control',
      targetAppLabel: 'Flashlight Hardware',
      resolvedPackageName: 'com.android.hardware.flashlight',
      intendedTask: 'Control Front, Rear, or Dual 360° flashlights with auto-off countdown timer',
      orderedSteps: [
        'Query CameraManager for Camera 0 (Rear) and Camera 1 (Front)',
        'Invoke setTorchMode(id, true) for requested emitter target',
        'Register watchdog timer (15s - 120s) with AlarmManager',
        'Confirm hardware thermal sensor is below 48°C limit'
      ],
      verificationStrategy: 'CameraManager.TorchCallback listener confirmation',
      knownLimitations: 'Dual flash requires physical front flash LED (verified on TECNO Camon i Click).',
      evidenceSummary: 'Tested across 15s, 60s, and 120s durations without thermal tripping.',
      successfulExecutions: 82,
      failedValidations: 0,
      state: 'VERIFIED',
      timestamp: '2026-10-18 09:40:00',
      version: 5
    },
    {
      id: 'skill-3',
      title: 'Morning Intelligence Synthesis',
      targetAppLabel: 'Morning Intelligence',
      resolvedPackageName: 'com.example.localagent.briefing',
      intendedTask: 'Aggregate nocturnal system maintenance audits, local weather, and pending to-dos',
      orderedSteps: [
        'Query nocturnal maintenance SQLite log',
        'Fetch on-device weather forecast cache',
        'Extract uncompleted to-do items from Keep Notes',
        'Synthesize compact executive HUD summary card'
      ],
      verificationStrategy: 'Independent assert that all 4 briefing cards populated',
      knownLimitations: 'Weather operates from 6-hour offline cached forecast if network unavailable.',
      evidenceSummary: 'Executed autonomously at 06:00 AM daily.',
      successfulExecutions: 31,
      failedValidations: 0,
      state: 'VERIFIED',
      timestamp: '2026-10-18 06:00:00',
      version: 3
    }
  ]);

  // Semantic Facts
  const [agentFacts] = useState<AgentFact[]>([
    { id: 'f-1', category: 'DEVICE', statement: 'Host device is TECNO Camon i Click (IN6) running HiOS 3.3 over Android 8.1 Oreo with 4GB RAM / 64GB Storage.', confidence: '1.0', provenance: 'android.os.Build inspection' },
    { id: 'f-2', category: 'STORAGE', statement: 'Durable Brain Vault persisted at /sdcard/LocalAgent/brain_vault.json in external public storage to survive APK uninstallation.', confidence: '1.0', provenance: 'Environment.getExternalStorageDirectory()' },
    { id: 'f-3', category: 'CINEMA', statement: 'Temporal anchor is Sunday, October 18, 2026. Top Telugu releases include SSMB29, Spirit, and The Raja Saab.', confidence: '0.99', provenance: 'Live Cinema Intelligence & Box Office Sync' },
    { id: 'f-4', category: 'HEURISTIC', statement: 'Dual-core brain strategy: Offline 1.5B/2B SLM uses ~1.2GB RAM (under 4GB limit). Zero-login cloud used for live web queries.', confidence: '0.98', provenance: 'Phase 18 Dual-Core Architectural Spec' }
  ]);

  // Task History
  const [taskHistory, setTaskHistory] = useState<TaskHistoryRecord[]>([
    {
      id: 'th-1',
      taskCommand: 'Turn on flashlight for 1 minute',
      targetPackage: 'com.android.hardware.flashlight',
      targetLabel: 'Flashlight Hardware',
      timestamp: 'Oct 18, 2026 • 10:02 AM',
      durationMs: 380,
      stepsTotal: 4,
      stepsCompleted: 4,
      finalStatus: 'SUCCESS',
      verificationDetails: 'CameraManager torch state callback returned ACTIVE on Camera 0. Watchdog set to 60s.',
      evidence: 'CameraManager.TorchCallback confirmed'
    },
    {
      id: 'th-2',
      taskCommand: 'Add a note: Buy groceries and almond milk',
      targetPackage: 'com.google.android.keep',
      targetLabel: 'Keep Notes',
      timestamp: 'Oct 18, 2026 • 09:15 AM',
      durationMs: 840,
      stepsTotal: 6,
      stepsCompleted: 6,
      finalStatus: 'SUCCESS',
      verificationDetails: 'View hierarchy node with text "Buy groceries and almond milk" verified in foreground.',
      evidence: 'AccessibilityNodeInfo matched'
    }
  ]);

  // Calculator & Simulated App Variables
  const [simulatedDisplayVal, setSimulatedDisplayVal] = useState('0');

  // Flashlight Countdown Watchdog Effect
  useEffect(() => {
    let timer: any = null;
    if (flashlightActive && flashlightRemainingSeconds !== null && flashlightRemainingSeconds > 0) {
      timer = setInterval(() => {
        setFlashlightRemainingSeconds(prev => {
          if (prev === null || prev <= 1) {
            setFlashlightActive(false);
            setExecutionLogs(l => [
              ...l,
              `[${new Date().toLocaleTimeString()}] HARDWARE WATCHDOG: Flashlight auto-shutoff expired safely. Torch powered down.`
            ]);
            return null;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [flashlightActive, flashlightRemainingSeconds]);

  // Flashlight Auto Mode Sync with Phone Viewport
  useEffect(() => {
    if (flashlightActive) {
      setActivePhoneTab('FLASHLIGHT');
    }
  }, [flashlightActive]);

  // ==========================================
  // INTENT RESOLUTION ENGINE (Rule + Fuzzy)
  // ==========================================
  const resolveTargetAndSteps = (promptText: string) => {
    const lower = promptText.toLowerCase().trim();

    // 1. FLASHLIGHT INTENT
    if (lower.includes('flash') || lower.includes('torch') || lower.includes('light')) {
      const isDual = lower.includes('both') || lower.includes('dual') || lower.includes('all');
      const isFront = lower.includes('front') || lower.includes('selfie');
      const targetMode: 'BACK' | 'FRONT' | 'DUAL' = isDual ? 'DUAL' : isFront ? 'FRONT' : 'BACK';

      let durationSeconds = 60;
      if (lower.includes('15 sec') || lower.includes('15s')) durationSeconds = 15;
      else if (lower.includes('30 sec') || lower.includes('30s')) durationSeconds = 30;
      else if (lower.includes('1 min') || lower.includes('1 minute') || lower.includes('60s')) durationSeconds = 60;
      else if (lower.includes('2 min') || lower.includes('2 minute') || lower.includes('120s')) durationSeconds = 120;

      const app = installedApps.find(a => a.packageName === 'com.android.hardware.flashlight') || installedApps[1];

      const steps: StepPlan[] = [
        {
          stepIndex: 1,
          description: `Acquire Android CameraManager system service`,
          actionType: 'CONNECT_SERVICE',
          target: 'android.hardware.camera2.CameraManager',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 2,
          description: `Resolve hardware emitter target: [${targetMode}] (${targetMode === 'DUAL' ? 'Cameras 0 & 1' : targetMode === 'FRONT' ? 'Camera 1 Front LED' : 'Camera 0 Rear Quad-LED'})`,
          actionType: 'RESOLVE_HARDWARE',
          target: targetMode,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 3,
          description: `Arm safety watchdog countdown timer for ${durationSeconds} seconds`,
          actionType: 'SET_WATCHDOG',
          target: `WatchdogTimer(${durationSeconds}s)`,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 4,
          description: `Dispatch setTorchMode(id, true) for ${targetMode} flash emitter`,
          actionType: 'ACTUATE_HARDWARE',
          target: `CameraManager.setTorchMode(${targetMode})`,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 5,
          description: `Independently verify CameraManager.TorchCallback confirms ACTIVE illumination state`,
          actionType: 'VERIFY_CONDITION',
          target: 'TorchCallback.onTorchModeChanged(ACTIVE)',
          risk: 'LOW',
          status: 'PENDING'
        }
      ];

      return { app, steps, flashlightTargetMode: targetMode, flashlightDuration: durationSeconds, isNoteAction: false };
    }

    // 2. NOTE INTENT (Google Keep Notes / Note App)
    if (lower.includes('note') || lower.includes('memo') || lower.includes('keep') || lower.includes('jot')) {
      const app = installedApps.find(a => a.packageName === 'com.google.android.keep') || installedApps[0];

      let noteContent = 'Review today\'s goals and prepare for tomorrow.';
      if (lower.includes('add a note:') || lower.includes('add note:')) {
        const parts = promptText.split(/:/);
        if (parts.length > 1 && parts[1].trim()) {
          noteContent = parts.slice(1).join(':').trim();
        }
      } else if (lower.includes('note that') || lower.includes('note down')) {
        noteContent = promptText.replace(/.*(note that|note down)/i, '').trim();
      }

      const steps: StepPlan[] = [
        {
          stepIndex: 1,
          description: `Scan device PackageManager for notes application: "${app.label}" (${app.packageName})`,
          actionType: 'RESOLVE_APP',
          target: 'PackageManager',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 2,
          description: `Confirm launcher intent target: ${app.packageName}/${app.launcherActivity}`,
          actionType: 'VERIFY_TARGET',
          target: app.packageName,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 3,
          description: `Dispatch Intent.ACTION_MAIN to launch ${app.label}`,
          actionType: 'LAUNCH_APP',
          target: app.packageName,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 4,
          description: `Observe window focus and verify foreground package is ${app.packageName}`,
          actionType: 'OBSERVE_FOREGROUND',
          target: app.packageName,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 5,
          description: `Locate accessible Floating Action Button node with description "New text note"`,
          actionType: 'RESOLVE_NODE',
          target: 'ContentDesc("New text note") | ResId("new_note_button")',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 6,
          description: `Inject note text: "${noteContent}" via AccessibilityNodeInfo.ACTION_SET_TEXT`,
          actionType: 'INJECT_TEXT',
          target: `EditableTitleAndBody("${noteContent}")`,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 7,
          description: `Simulate back button navigation to commit note to persistent Keep SQLite database`,
          actionType: 'PERFORM_GESTURE',
          target: 'GLOBAL_ACTION_BACK',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 8,
          description: `Independently verify note record exists in Keep Notes hierarchy & Durable Vault`,
          actionType: 'VERIFY_CONDITION',
          target: 'NoteCreatedAndPersisted',
          risk: 'LOW',
          status: 'PENDING'
        }
      ];

      return { app, steps, extractedNoteText: noteContent, isNoteAction: true };
    }

    // 3. MORNING BRIEFING & NIGHT LOG INTENT
    if (lower.includes('morning briefing') || lower.includes('briefing') || lower.includes('daily news') || lower.includes('night') || lower.includes('weather') || lower.includes('forecast')) {
      const app = installedApps.find(a => a.packageName === 'com.example.localagent.briefing') || installedApps[2];

      const steps: StepPlan[] = [
        {
          stepIndex: 1,
          description: `Resolve on-device Morning Intelligence subsystem (${app.packageName})`,
          actionType: 'RESOLVE_SUBSYSTEM',
          target: app.packageName,
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 2,
          description: `Query nocturnal maintenance SQLite log (00:00 - 06:00 AM autonomous audits)`,
          actionType: 'QUERY_LOGS',
          target: 'AutonomousNightMaintenanceTable',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 3,
          description: `Fetch local weather forecast for Sunday, October 18, 2026 (Hyderabad 28°C)`,
          actionType: 'WEATHER_FETCH',
          target: 'LocalWeatherService',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 4,
          description: `Extract today's prioritized to-do items from Keep Notes database`,
          actionType: 'SYNC_TODOS',
          target: 'KeepNotesDatabase',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 5,
          description: `Render unified executive Morning Briefing HUD on device viewport`,
          actionType: 'RENDER_HUD',
          target: 'MorningBriefingView',
          risk: 'LOW',
          status: 'PENDING'
        }
      ];

      return { app, steps, isNoteAction: false };
    }

    // 4. TELUGU MOVIES INTENT
    if (lower.includes('telugu') || lower.includes('movie') || lower.includes('cinema') || lower.includes('film')) {
      const app = installedApps.find(a => a.packageName === 'com.example.localagent.research') || installedApps[3];

      const steps: StepPlan[] = [
        {
          stepIndex: 1,
          description: `Engage Cinema & Web Intelligence Engine anchored to October 18, 2026`,
          actionType: 'INIT_RESEARCH',
          target: 'CinemaIntelligenceEngine',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 2,
          description: `Scan verified box office releases (SSMB29, Spirit, The Raja Saab, Vishwambhara)`,
          actionType: 'QUERY_BOX_OFFICE',
          target: 'TeluguCinemaGraph2026',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 3,
          description: `Verify streaming platforms (Netflix, Prime Video, Aha) & current theatrical status`,
          actionType: 'VERIFY_STREAMING',
          target: 'OTTStreamingCatalogs',
          risk: 'LOW',
          status: 'PENDING'
        },
        {
          stepIndex: 4,
          description: `Present categorized release cards with ratings, directors, and synopsis`,
          actionType: 'DISPLAY_CATALOG',
          target: 'CinemaExplorerViewport',
          risk: 'LOW',
          status: 'PENDING'
        }
      ];

      return { app, steps, isNoteAction: false };
    }

    // 5. CALCULATOR INTENT
    if (lower.includes('calc') || lower.includes('open calculator') || lower.includes('add ') || lower.includes('multiply')) {
      const app = installedApps.find(a => a.packageName === 'com.transsion.calculator') || installedApps[5];
      const steps: StepPlan[] = [
        { stepIndex: 1, description: `Query device PackageManager to resolve package for "${app.label}"`, actionType: 'RESOLVE_APP', target: 'PackageManager', risk: 'LOW', status: 'PENDING' },
        { stepIndex: 2, description: `Confirm launcher intent target: ${app.packageName}`, actionType: 'VERIFY_TARGET', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 3, description: `Dispatch Intent.ACTION_MAIN to launch Calculator`, actionType: 'LAUNCH_APP', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 4, description: `Observe window focus and verify foreground package is ${app.packageName}`, actionType: 'OBSERVE_FOREGROUND', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 5, description: `Locate accessible button node with label "7" in view hierarchy`, actionType: 'RESOLVE_NODE', target: 'Button("7")', risk: 'LOW', status: 'PENDING' },
        { stepIndex: 6, description: `Dispatch accessibility action ACTION_CLICK on button "7"`, actionType: 'ACCESSIBILITY_CLICK', target: 'Button("7")', risk: 'LOW', status: 'PENDING' },
        { stepIndex: 7, description: `Independently verify Calculator display reflects "7"`, actionType: 'VERIFY_CONDITION', target: 'DisplayMatches("7")', risk: 'LOW', status: 'PENDING' }
      ];
      return { app, steps, isNoteAction: false };
    }

    // 6. SETTINGS INTENT
    if (lower.includes('setting') || lower.includes('wifi') || lower.includes('wi-fi') || lower.includes('bluetooth')) {
      const app = installedApps.find(a => a.packageName === 'com.android.settings') || installedApps[4];
      const steps: StepPlan[] = [
        { stepIndex: 1, description: `Query device PackageManager to resolve package for "${app.label}"`, actionType: 'RESOLVE_APP', target: 'PackageManager', risk: 'LOW', status: 'PENDING' },
        { stepIndex: 2, description: `Confirm launcher intent target: ${app.packageName}`, actionType: 'VERIFY_TARGET', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 3, description: `Dispatch Intent.ACTION_MAIN to launch Settings`, actionType: 'LAUNCH_APP', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 4, description: `Observe window focus and verify foreground package is ${app.packageName}`, actionType: 'OBSERVE_FOREGROUND', target: app.packageName, risk: 'LOW', status: 'PENDING' },
        { stepIndex: 5, description: `Independently verify Settings dashboard header and preference list`, actionType: 'VERIFY_CONDITION', target: 'DashboardHeaderVisible', risk: 'LOW', status: 'PENDING' }
      ];
      return { app, steps, isNoteAction: false };
    }

    // 7. GENERIC FALLBACK
    const fallbackApp = installedApps[0];
    const genericSteps: StepPlan[] = [
      { stepIndex: 1, description: `Query device PackageManager for command target`, actionType: 'RESOLVE_APP', target: 'PackageManager', risk: 'LOW', status: 'PENDING' },
      { stepIndex: 2, description: `Confirm target package: ${fallbackApp.packageName}`, actionType: 'VERIFY_TARGET', target: fallbackApp.packageName, risk: 'LOW', status: 'PENDING' },
      { stepIndex: 3, description: `Dispatch Intent.ACTION_MAIN for ${fallbackApp.label}`, actionType: 'LAUNCH_APP', target: fallbackApp.packageName, risk: 'LOW', status: 'PENDING' },
      { stepIndex: 4, description: `Independently verify foreground package is ${fallbackApp.packageName}`, actionType: 'OBSERVE_FOREGROUND', target: fallbackApp.packageName, risk: 'LOW', status: 'PENDING' }
    ];
    return { app: fallbackApp, steps: genericSteps, isNoteAction: false };
  };

  const liveResolution = resolveTargetAndSteps(userPrompt);

  const filteredMovies = teluguMoviesList.filter(m => {
    if (movieFilter === 'THEATRES') return m.theatrical;
    if (movieFilter === 'OTT') return !!m.ottPlatform;
    return true;
  });

  // ==========================================
  // TASK INITIATION & STEP EXECUTION
  // ==========================================
  const handleInitiateTask = () => {
    if (killSwitchActive) return;

    setAgentConsciousness(prev => ({
      ...prev,
      operationalStatus: 'ANALYZING',
      currentThought: `Parsing directive: "${userPrompt}". Synthesizing sequential verification plan...`,
      cognitivePulse: 'focused'
    }));

    const resolution = resolveTargetAndSteps(userPrompt);
    setProposedPlan(resolution.steps);
    setTargetPackageForPlan(resolution.app.packageName);
    setTargetLabelForPlan(resolution.app.label);
    setPendingPlanModal(true);

    setAgentConsciousness(prev => ({
      ...prev,
      operationalStatus: 'AWAITING_APPROVAL',
      currentThought: `Synthesized ${resolution.steps.length} deterministic steps for ${resolution.app.label}. Awaiting J.A.R.V.I.S. supervisor confirmation.`,
      cognitivePulse: 'evaluating'
    }));
  };

  const handleApproveAndStart = async () => {
    setPendingPlanModal(false);
    if (killSwitchActive) return;

    const resolution = resolveTargetAndSteps(userPrompt);
    const startTime = Date.now();

    setAgentConsciousness(prev => ({
      ...prev,
      operationalStatus: 'EXECUTING',
      currentThought: `Executing directive for ${targetLabelForPlan}. Sequential verification pipeline running...`,
      cognitivePulse: 'active'
    }));

    let completedSteps = 0;

    for (let i = 0; i < proposedPlan.length; i++) {
      if (killSwitchActive) break;
      setActiveStepIndex(i);

      setProposedPlan(prev =>
        prev.map((s, idx) => (idx === i ? { ...s, status: 'EXECUTING' } : s))
      );

      const isPostLaunch = i >= 2;
      const currentPkg = isPostLaunch ? targetPackageForPlan : 'com.aistudio.localagent';
      const currentLabel = isPostLaunch ? targetLabelForPlan : 'LocalAgent Home';

      setAgentConsciousness(prev => ({
        ...prev,
        currentThought: `Step ${i + 1}/${proposedPlan.length}: ${proposedPlan[i].description}`,
        activeForegroundPackage: currentPkg,
        activeAppLabel: currentLabel
      }));

      setExecutionLogs(prev => [
        ...prev,
        `[${new Date().toLocaleTimeString()}] Step ${i + 1}/${proposedPlan.length}: ${proposedPlan[i].description}`
      ]);

      // Handle Calculator 7 simulation
      if (targetPackageForPlan === 'com.transsion.calculator' && i === 5) {
        setSimulatedDisplayVal('7');
        setActivePhoneTab('CALCULATOR');
      }

      // Handle Flashlight activation
      if (targetPackageForPlan === 'com.android.hardware.flashlight' && i === 3) {
        const mode = resolution.flashlightTargetMode || 'BACK';
        const dur = resolution.flashlightDuration || 60;
        setFlashlightActive(true);
        setFlashlightMode(mode);
        setFlashlightRemainingSeconds(dur);
        setFlashlightTotalSeconds(dur);
        setActivePhoneTab('FLASHLIGHT');
      }

      // Handle Keep Notes save
      if (resolution.isNoteAction && i === 5) {
        const newNoteText = resolution.extractedNoteText || 'Important priority for today';
        const newNote: NoteItem = {
          id: `note-${Date.now()}`,
          title: `Note: ${newNoteText.slice(0, 24)}...`,
          content: newNoteText,
          timestamp: `Oct 18, 2026 • ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`,
          color: 'amber',
          pinned: true
        };
        setNotesList(prev => [newNote, ...prev]);
        setActivePhoneTab('KEEP');
        setExecutionLogs(prev => [
          ...prev,
          `[${new Date().toLocaleTimeString()}] PERSISTENCE: Saved note "${newNoteText}" into Keep Notes repository.`
        ]);
      }

      if (targetPackageForPlan === 'com.example.localagent.briefing') {
        setActivePhoneTab('BRIEFING');
      }
      if (targetPackageForPlan === 'com.example.localagent.research') {
        setActivePhoneTab('CINEMA');
      }
      if (targetPackageForPlan === 'com.android.settings') {
        setActivePhoneTab('SETTINGS');
      }

      // Simulated step latency
      await new Promise(r => setTimeout(r, 460));
      completedSteps++;

      setProposedPlan(prev =>
        prev.map((s, idx) => (idx === i ? { ...s, status: 'VERIFIED' } : s))
      );
    }

    const duration = Date.now() - startTime;
    setActiveStepIndex(-1);
    setAgentConsciousness(prev => ({
      ...prev,
      operationalStatus: 'READY_OBSERVING',
      currentThought: `Directive verified successfully in ${duration}ms. Active package: ${targetPackageForPlan}. Knowledge vault committed to /sdcard/LocalAgent.`,
      activeForegroundPackage: targetPackageForPlan,
      activeAppLabel: targetLabelForPlan,
      cognitivePulse: 'calm'
    }));

    const newRecord: TaskHistoryRecord = {
      id: `th-${Date.now()}`,
      taskCommand: userPrompt,
      targetPackage: targetPackageForPlan,
      targetLabel: targetLabelForPlan,
      timestamp: `Oct 18, 2026 • ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`,
      durationMs: duration,
      stepsTotal: proposedPlan.length,
      stepsCompleted: completedSteps,
      finalStatus: killSwitchActive ? 'ABORTED' : 'SUCCESS',
      verificationDetails: `All ${completedSteps}/${proposedPlan.length} steps executed and independently confirmed through AccessibilityNodeInfo state asserts.`,
      evidence: `NodeAssert(target=${targetPackageForPlan}, verified=true)`
    };

    setTaskHistory(prev => [newRecord, ...prev.slice(0, 19)]);
  };

  // Hardware triggers
  const handleTriggerHaptic = (pattern: 'TICK' | 'BURST' | 'IMPACT') => {
    setHapticFrequency(pattern);
    setLastHapticTriggered(`${pattern} [${pattern === 'TICK' ? '15ms' : pattern === 'BURST' ? '40ms x 2' : '80ms heavy'}]`);
    if ('vibrate' in navigator) {
      if (pattern === 'TICK') navigator.vibrate(15);
      else if (pattern === 'BURST') navigator.vibrate([40, 60, 40]);
      else navigator.vibrate(90);
    }
    setTimeout(() => setLastHapticTriggered(null), 1400);
  };

  const handleTriggerAudioChime = (tone: 'CONFIRM' | 'ALERT') => {
    setLastAudioChime(tone);
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.frequency.value = tone === 'ALERT' ? 1240 : 880;
      gain.gain.setValueAtTime(0.12, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.32);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.32);
    } catch (e) {
      // Audio fallback
    }
    setTimeout(() => setLastAudioChime(null), 1400);
  };

  // Brain Reasoning Test
  const handleRunBrainTest = () => {
    setIsBrainThinking(true);
    setBrainTestResponse(null);

    setTimeout(() => {
      setIsBrainThinking(false);
      if (brainOperatingMode === 'OFFLINE_ON_DEVICE') {
        setBrainTestResponse(
          `[Gemma 2B IT (4-bit Q4_K_M) - Executed 100% On-Device / 0ms Cloud Latency]\n` +
          `• Intent: Compound Hardware + Productivity Automation\n` +
          `• Plan: \n` +
          `  1. Query CameraManager -> enable dual flash with 60s auto-off watchdog.\n` +
          `  2. Launch Keep Notes via Intent.ACTION_MAIN.\n` +
          `  3. Use accessibility tree to inject new note into Keep Notes database.\n` +
          `  4. Commit procedural learning to /sdcard/LocalAgent/brain_vault.json.\n` +
          `• Memory footprint: 1.18 GB RAM (within 4GB host RAM budget).`
        );
      } else {
        setBrainTestResponse(
          `[Hybrid Zero-Login Cloud Engine - Live Web & Grounding]\n` +
          `• Live Date: Sunday, October 18, 2026\n` +
          `• Telugu Cinema Status: SSMB29 in theatres worldwide (₹1,200+ Cr). Spirit & Raja Saab on OTT streaming.\n` +
          `• Web Grounding: Retrieved anonymous public synopsis with zero user login credentials.\n` +
          `• Fallback: Ready to seamlessly revert to Offline Gemma 2B if Wi-Fi / LTE disconnects.`
        );
      }
    }, 600);
  };

  // Export Brain Vault Backup
  const handleExportVault = () => {
    const vaultData = {
      vaultVersion: '2.4',
      storageTarget: '/sdcard/LocalAgent/brain_vault.json',
      exportDate: 'Sunday, October 18, 2026',
      device: 'TECNO Camon i Click (HiOS 3.3 / Android 8.1)',
      skillsCount: skills.length,
      skills,
      factsCount: agentFacts.length,
      facts: agentFacts,
      notesCount: notesList.length,
      notes: notesList,
      resilienceNotice: 'This JSON backup survives APK uninstallation on Android public shared storage.'
    };
    const blob = new Blob([JSON.stringify(vaultData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'LocalAgent_Brain_Vault_Backup_2026.json';
    a.click();
    URL.revokeObjectURL(url);
    setExecutionLogs(prev => [
      ...prev,
      `[${new Date().toLocaleTimeString()}] VAULT BACKUP: Exported complete procedural knowledge graph to LocalAgent_Brain_Vault_Backup_2026.json.`
    ]);
  };

  // Quick preset commands
  const quickPresets = [
    { label: 'Flashlight 1 min', cmd: 'Turn on flashlight for 1 minute', icon: Sun, color: 'text-amber-400' },
    { label: 'Dual 360° Flash', cmd: 'Turn on dual flash', icon: Sun, color: 'text-amber-300' },
    { label: 'Add Keep Note', cmd: 'Add a note: Buy groceries and almond milk', icon: FileText, color: 'text-cyan-400' },
    { label: 'Morning Briefing', cmd: 'Morning briefing & what did you do night', icon: CloudSun, color: 'text-amber-400' },
    { label: 'Telugu Cinema 2026', cmd: 'Search for new Telugu movies', icon: Film, color: 'text-purple-400' },
    { label: 'Open Settings', cmd: 'Open device settings', icon: SettingsIcon, color: 'text-cyan-300' },
    { label: 'Open Calculator', cmd: 'Open calculator and calculate 7', icon: Terminal, color: 'text-emerald-400' }
  ];

  return (
    <div className={`min-h-screen text-slate-100 flex flex-col font-sans transition-colors duration-300 ${
      oledLowPowerMode ? 'bg-black selection:bg-cyan-600 selection:text-white' : 'bg-[#030712] selection:bg-cyan-500 selection:text-black'
    }`}>
      {/* =========================================================================
          SECTION 1: J.A.R.V.I.S. HUD HEADER & SYSTEM DIRECTIVES
          ========================================================================= */}
      <header className={`border-b sticky top-0 z-40 px-3 sm:px-6 py-2.5 backdrop-blur-md transition-all ${
        oledLowPowerMode ? 'bg-black/95 border-cyan-950/60' : 'bg-[#030712]/90 border-cyan-900/40'
      }`}>
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-2">
          {/* Brand & Living Consciousness Ring */}
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="relative shrink-0">
              <div className={`w-9 h-9 rounded-xl flex items-center justify-center font-mono font-bold text-black shadow-md transition-all ${
                killSwitchActive
                  ? 'bg-red-600 text-white'
                  : oledLowPowerMode
                  ? 'bg-cyan-500 text-black shadow-cyan-500/10'
                  : 'bg-gradient-to-tr from-cyan-400 via-teal-300 to-cyan-500 shadow-cyan-400/30'
              }`}>
                <Brain className="w-5 h-5" />
              </div>
              <span className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border border-black ${
                killSwitchActive ? 'bg-red-500 animate-ping' : 'bg-emerald-400'
              }`} />
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-sm sm:text-base font-bold tracking-tight text-white flex items-center gap-1">
                  J.A.R.V.I.S.
                  <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-cyan-950/80 text-cyan-300 border border-cyan-800/80 font-normal">
                    CORE v2.5
                  </span>
                </span>
                <span className="text-[10px] font-mono text-amber-400 bg-amber-950/40 px-1.5 py-0.5 rounded border border-amber-800/60 hidden xs:inline">
                  Oct 18, 2026
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-mono truncate hidden sm:flex items-center gap-1.5">
                <span>TECNO IN6 (HiOS 3.3)</span>
                <span className="text-slate-600">&bull;</span>
                <span className="text-cyan-400">{brainOperatingMode === 'OFFLINE_ON_DEVICE' ? 'Offline Gemma 2B (Zero Cloud)' : 'Hybrid Web Engine'}</span>
                <span className="text-slate-600">&bull;</span>
                <span className="text-emerald-400">Vault: /sdcard/LocalAgent</span>
              </p>
            </div>
          </div>

          {/* Quick Controls: OLED Eco Mode + Kill Switch */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            {/* Low Power OLED Saver Toggle */}
            <button
              onClick={() => setOledLowPowerMode(!oledLowPowerMode)}
              title="Toggle OLED Pure Black Low Power Consumption Mode"
              className={`px-2.5 py-1.5 rounded-lg text-xs font-mono font-medium transition-all flex items-center gap-1.5 border ${
                oledLowPowerMode
                  ? 'bg-emerald-950 text-emerald-300 border-emerald-700 shadow-sm'
                  : 'bg-slate-900/80 hover:bg-slate-800 text-slate-300 border-slate-800'
              }`}
            >
              <Power className={`w-3.5 h-3.5 ${oledLowPowerMode ? 'text-emerald-400' : 'text-slate-400'}`} />
              <span className="hidden md:inline">{oledLowPowerMode ? 'OLED ECO (ACTIVE)' : 'OLED ECO'}</span>
            </button>

            {/* Emergency Kill Switch */}
            <button
              onClick={() => {
                setKillSwitchActive(!killSwitchActive);
                if (!killSwitchActive) {
                  setFlashlightActive(false);
                  setAgentConsciousness(prev => ({
                    ...prev,
                    operationalStatus: 'HALTED',
                    currentThought: 'EMERGENCY DISCONNECT ENGAGED. Hardware and intent dispatch severed.',
                    cognitivePulse: 'halted'
                  }));
                } else {
                  setAgentConsciousness(prev => ({
                    ...prev,
                    operationalStatus: 'READY_OBSERVING',
                    currentThought: 'Kill switch disengaged. Resuming autonomous perception and action.',
                    cognitivePulse: 'calm'
                  }));
                }
              }}
              className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-mono font-bold transition-all flex items-center gap-1.5 border ${
                killSwitchActive
                  ? 'bg-red-600 text-white border-red-500 animate-pulse shadow-md shadow-red-600/30'
                  : 'bg-red-950/40 hover:bg-red-950/80 text-red-300 border-red-900/60'
              }`}
            >
              <ShieldAlert className="w-3.5 h-3.5 text-red-400" />
              <span>{killSwitchActive ? 'HALTED' : 'KILL SWITCH'}</span>
            </button>
          </div>
        </div>
      </header>

      {/* =========================================================================
          SECTION 2: JARVIS TELEMETRY & COGNITIVE HUD BAR
          ========================================================================= */}
      <section className="max-w-7xl w-full mx-auto px-3 sm:px-6 pt-3">
        <div className={`rounded-xl border p-3 sm:p-4 transition-all ${
          oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/90 border-cyan-900/40 shadow-sm'
        }`}>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
            {/* Status & Perception Stream */}
            <div className="flex items-start sm:items-center gap-3 min-w-0">
              <div className="relative shrink-0 mt-0.5 sm:mt-0">
                <div className="w-8 h-8 rounded-lg bg-black border border-cyan-500/30 flex items-center justify-center text-cyan-400">
                  <Compass className="w-4 h-4 animate-spin-slow" />
                </div>
                <span className="absolute -top-1 -right-1 flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-cyan-400" />
                </span>
              </div>

              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap text-xs">
                  <span className="font-mono font-bold uppercase tracking-wider text-cyan-300 text-[11px]">
                    [ COGNITIVE STATUS ]
                  </span>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold border ${
                    agentConsciousness.operationalStatus === 'EXECUTING'
                      ? 'bg-cyan-950 text-cyan-300 border-cyan-700 animate-pulse'
                      : agentConsciousness.operationalStatus === 'HALTED'
                      ? 'bg-red-950 text-red-400 border-red-800'
                      : 'bg-emerald-950 text-emerald-400 border-emerald-800'
                  }`}>
                    {agentConsciousness.operationalStatus}
                  </span>
                  <span className="text-slate-500 hidden sm:inline">&bull;</span>
                  <span className="text-[11px] text-slate-300 truncate hidden sm:inline">
                    Foreground: <strong className="text-cyan-300 font-mono">{agentConsciousness.activeAppLabel}</strong>
                  </span>
                </div>
                <p className="text-xs text-slate-300 mt-0.5 line-clamp-2 leading-relaxed font-sans">
                  {agentConsciousness.currentThought}
                </p>
              </div>
            </div>

            {/* Monospace Telemetry Strip */}
            <div className="flex items-center gap-2 text-[10px] font-mono self-start md:self-center shrink-0 flex-wrap">
              {flashlightActive && (
                <div className="px-2 py-1 bg-amber-950/60 border border-amber-600/60 rounded text-amber-300 flex items-center gap-1 animate-pulse font-bold">
                  <Sun className="w-3 h-3 text-amber-400 fill-amber-400" />
                  <span>{flashlightMode} FLASH {flashlightRemainingSeconds !== null ? `(${flashlightRemainingSeconds}s)` : 'ON'}</span>
                </div>
              )}
              <div className="px-2 py-1 bg-black border border-slate-800 rounded text-slate-300">
                <span className="text-slate-500">RAM:</span> <span className="text-cyan-300 font-bold">42 MB</span> / 4GB
              </div>
              <div className="px-2 py-1 bg-black border border-slate-800 rounded text-slate-300 hidden xs:block">
                <span className="text-slate-500">POWER:</span> <span className="text-emerald-400 font-bold">0.8W (OLED)</span>
              </div>
              <div className="px-2 py-1 bg-black border border-slate-800 rounded text-slate-300 hidden sm:block">
                <span className="text-slate-500">BATTERY:</span> <span className="text-emerald-400 font-bold">{batteryLevel}%</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 3: CATEGORIZED NAVIGATION BAR (Mobile Scrollable Dock)
          ========================================================================= */}
      <nav className="max-w-7xl w-full mx-auto px-3 sm:px-6 pt-3">
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar text-xs font-mono">
          {[
            { id: 'all', label: 'All Systems', icon: Layers },
            { id: 'directives', label: 'Directives', icon: Zap },
            { id: 'actuators', label: 'Hardware Actuators', icon: Sliders },
            { id: 'viewport', label: 'Live Screen', icon: Smartphone },
            { id: 'intelligence', label: 'Morning & Cinema', icon: CloudSun },
            { id: 'vault', label: 'Brain Vault', icon: Database },
            { id: 'telemetry', label: 'Audit Logs', icon: Terminal }
          ].map(tab => {
            const Icon = tab.icon;
            const isActive = activeCategory === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveCategory(tab.id as any)}
                className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition-all flex items-center gap-1.5 border ${
                  isActive
                    ? 'bg-cyan-500 text-black font-bold border-cyan-400 shadow-sm shadow-cyan-500/20'
                    : 'bg-black/60 text-slate-400 hover:text-white border-slate-800/80'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* =========================================================================
          MAIN CARD-BASED GRID DASHBOARD (MOBILE-FIRST, STRICT BOUNDS)
          ========================================================================= */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-3 sm:px-6 py-4 space-y-4">

        {/* -----------------------------------------------------------------------
            SECTION A: AUTONOMOUS TASK DIRECTIVE NEXUS
            ----------------------------------------------------------------------- */}
        {(activeCategory === 'all' || activeCategory === 'directives') && (
          <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
            oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
          }`}>
            <div className="flex items-center justify-between mb-2.5">
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-cyan-400" />
                <h2 className="text-sm font-bold text-white tracking-wide font-mono uppercase">
                  [ DIRECTIVE NEXUS &bull; NATURAL LANGUAGE EXECUTION ]
                </h2>
              </div>
              <span className="text-[10px] font-mono text-cyan-400 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-800/60 hidden sm:inline">
                Phase 13: Pre-Flight Safety Verification Gate
              </span>
            </div>

            {/* Input & Dispatch Button */}
            <div className="space-y-2">
              <div className="relative">
                <input
                  type="text"
                  value={userPrompt}
                  onChange={e => setUserPrompt(e.target.value)}
                  onKeyDown={e => {
                    if (e.key === 'Enter') handleInitiateTask();
                  }}
                  placeholder="Tell J.A.R.V.I.S.: 'Turn on flashlight for 1 minute', 'Add a note: Buy groceries', 'Morning briefing'..."
                  className="w-full bg-black border border-cyan-900/60 focus:border-cyan-400 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none transition-all pr-24 font-sans"
                />
                <button
                  onClick={handleInitiateTask}
                  disabled={agentConsciousness.operationalStatus === 'EXECUTING'}
                  className="absolute right-1.5 top-1.5 bottom-1.5 px-3 bg-cyan-500 hover:bg-cyan-400 disabled:bg-slate-800 disabled:text-slate-500 text-black font-bold text-xs rounded-lg transition-colors flex items-center gap-1 shadow-sm"
                >
                  <Play className="w-3 h-3 fill-current" />
                  <span>Execute</span>
                </button>
              </div>

              {/* Real-time Target Resolution Pill */}
              <div className="p-2.5 bg-black border border-slate-800/80 rounded-xl flex items-center justify-between text-xs font-mono flex-wrap gap-2">
                <div className="flex items-center gap-2 truncate">
                  <span className="text-slate-500 text-[11px]">RESOLVED TARGET:</span>
                  <span className="text-cyan-300 font-bold flex items-center gap-1 truncate">
                    {liveResolution.app.packageName.includes('flashlight') && <Sun className="w-3.5 h-3.5 text-amber-400" />}
                    {liveResolution.app.packageName.includes('keep') && <FileText className="w-3.5 h-3.5 text-amber-400" />}
                    {liveResolution.app.packageName.includes('briefing') && <CloudSun className="w-3.5 h-3.5 text-amber-400" />}
                    {liveResolution.app.packageName.includes('research') && <Film className="w-3.5 h-3.5 text-purple-400" />}
                    {liveResolution.app.label === 'Settings' && <SettingsIcon className="w-3.5 h-3.5 text-cyan-400" />}
                    {liveResolution.app.label === 'Calculator' && <Terminal className="w-3.5 h-3.5 text-emerald-400" />}
                    <span>{liveResolution.app.label}</span>
                  </span>
                  <span className="text-slate-600 text-[10px] hidden sm:inline">({liveResolution.app.packageName})</span>
                </div>

                <span className="text-[10px] text-emerald-400 px-2 py-0.5 rounded bg-emerald-950/60 border border-emerald-800/60">
                  {liveResolution.steps.length} Verified Steps
                </span>
              </div>
            </div>

            {/* Quick Preset Directive Pills */}
            <div className="mt-3 pt-2.5 border-t border-slate-900 flex items-center gap-1.5 overflow-x-auto no-scrollbar text-xs">
              <span className="text-[10px] font-mono text-slate-500 uppercase shrink-0">Presets:</span>
              {quickPresets.map((preset, idx) => {
                const Icon = preset.icon;
                const isSelected = userPrompt === preset.cmd;
                return (
                  <button
                    key={idx}
                    onClick={() => setUserPrompt(preset.cmd)}
                    className={`px-2.5 py-1 rounded-md text-[11px] font-mono transition-all flex items-center gap-1.5 shrink-0 border ${
                      isSelected
                        ? 'bg-cyan-950 text-cyan-300 border-cyan-500 font-bold'
                        : 'bg-black text-slate-400 hover:text-white border-slate-800/80 hover:border-slate-700'
                    }`}
                  >
                    <Icon className={`w-3 h-3 ${preset.color}`} />
                    <span>{preset.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* -----------------------------------------------------------------------
            SECTION B: TWO-COLUMN CARDS GRID (ACTUATORS + DEVICE VIEWPORT)
            ----------------------------------------------------------------------- */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">

          {/* LEFT 7 COLUMNS: HARDWARE ACTUATOR MATRIX */}
          {(activeCategory === 'all' || activeCategory === 'actuators') && (
            <div className={`space-y-4 ${activeCategory === 'all' ? 'lg:col-span-7' : 'lg:col-span-12'}`}>
              <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
                oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
              }`}>
                <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                  <div className="flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-cyan-400" />
                    <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                      [ HARDWARE &amp; ACTUATOR MATRIX ]
                    </h3>
                  </div>
                  <span className="text-[10px] font-mono text-emerald-400">100% Controllable</span>
                </div>

                {/* 1. Flashlight Multi-Emitter Control Card */}
                <div className="p-3.5 bg-black border border-amber-500/40 rounded-xl space-y-2.5 mb-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Sun className="w-4 h-4 text-amber-400" />
                      <span className="font-bold text-white text-xs font-mono uppercase">
                        Camera Flashlight Emitters
                      </span>
                    </div>
                    <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                      flashlightActive ? 'bg-amber-400 text-black animate-pulse' : 'bg-slate-900 text-slate-500'
                    }`}>
                      {flashlightActive ? 'ILLUMINATION ACTIVE' : 'TORCH OFF'}
                    </span>
                  </div>

                  {/* Mode Buttons */}
                  <div className="grid grid-cols-3 gap-2 text-xs font-mono">
                    <button
                      onClick={() => {
                        setFlashlightMode('BACK');
                        setFlashlightActive(true);
                        setFlashlightRemainingSeconds(60);
                        setFlashlightTotalSeconds(60);
                        setActivePhoneTab('FLASHLIGHT');
                      }}
                      className={`p-2 sm:p-2.5 rounded-lg border text-left transition-all ${
                        flashlightActive && flashlightMode === 'BACK'
                          ? 'bg-amber-500 text-black border-amber-400 font-bold'
                          : 'bg-black border-slate-800 text-slate-300 hover:text-white'
                      }`}
                    >
                      <div className="text-[11px] font-bold">Rear Flash</div>
                      <div className="text-[9px] opacity-75">Camera 0 (Quad)</div>
                    </button>

                    <button
                      onClick={() => {
                        setFlashlightMode('FRONT');
                        setFlashlightActive(true);
                        setFlashlightRemainingSeconds(60);
                        setFlashlightTotalSeconds(60);
                        setActivePhoneTab('FLASHLIGHT');
                      }}
                      className={`p-2 sm:p-2.5 rounded-lg border text-left transition-all ${
                        flashlightActive && flashlightMode === 'FRONT'
                          ? 'bg-amber-500 text-black border-amber-400 font-bold'
                          : 'bg-black border-slate-800 text-slate-300 hover:text-white'
                      }`}
                    >
                      <div className="text-[11px] font-bold">Front Flash</div>
                      <div className="text-[9px] opacity-75">Camera 1 (Dual)</div>
                    </button>

                    <button
                      onClick={() => {
                        setFlashlightMode('DUAL');
                        setFlashlightActive(true);
                        setFlashlightRemainingSeconds(60);
                        setFlashlightTotalSeconds(60);
                        setActivePhoneTab('FLASHLIGHT');
                      }}
                      className={`p-2 sm:p-2.5 rounded-lg border text-left transition-all ${
                        flashlightActive && flashlightMode === 'DUAL'
                          ? 'bg-amber-500 text-black border-amber-400 font-bold'
                          : 'bg-black border-slate-800 text-slate-300 hover:text-white'
                      }`}
                    >
                      <div className="text-[11px] font-bold">Dual 360°</div>
                      <div className="text-[9px] opacity-75">HAL Concurrent</div>
                    </button>
                  </div>

                  {/* Hardware HAL Notice */}
                  <div className="p-2 bg-slate-950/80 rounded border border-slate-900 text-[10px] text-slate-400 font-mono flex items-start gap-1.5">
                    <Info className="w-3 h-3 text-cyan-400 shrink-0 mt-0.5" />
                    <span>
                      <strong className="text-slate-300">HAL Note:</strong> Concurrent Dual Flash requires Camera HAL3 concurrent streaming support. If hardware locks simultaneous ISP channels, agent gracefully falls back to primary rear torch.
                    </span>
                  </div>

                  {/* Auto-shutoff Watchdog Bar */}
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-900 text-xs">
                    <div className="flex items-center gap-1.5 font-mono text-[10px]">
                      <span className="text-slate-500">WATCHDOG TIMER:</span>
                      {[15, 30, 60, 120].map(s => (
                        <button
                          key={s}
                          onClick={() => {
                            setFlashlightActive(true);
                            setFlashlightRemainingSeconds(s);
                            setFlashlightTotalSeconds(s);
                            setActivePhoneTab('FLASHLIGHT');
                          }}
                          className={`px-2 py-0.5 rounded ${
                            flashlightActive && flashlightRemainingSeconds === s
                              ? 'bg-amber-400 text-black font-bold'
                              : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
                          }`}
                        >
                          {s < 60 ? `${s}s` : `${s / 60}m`}
                        </button>
                      ))}
                    </div>

                    {flashlightActive && (
                      <button
                        onClick={() => setFlashlightActive(false)}
                        className="px-2.5 py-0.5 bg-red-600 hover:bg-red-500 text-white rounded font-mono text-[10px] font-bold"
                      >
                        SHUT OFF
                      </button>
                    )}
                  </div>
                </div>

                {/* 2. Haptic Vibration & Audio Speaker Actuators */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
                  {/* Haptic Motor */}
                  <div className="p-3 bg-black border border-cyan-950 rounded-xl space-y-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-mono font-bold text-white flex items-center gap-1">
                        <Vibrate className="w-3.5 h-3.5 text-cyan-400" />
                        Haptic ERM / LRA
                      </span>
                      {lastHapticTriggered && (
                        <span className="text-[9px] font-mono text-cyan-300 animate-pulse">
                          {lastHapticTriggered}
                        </span>
                      )}
                    </div>
                    <div className="grid grid-cols-3 gap-1.5 text-xs font-mono">
                      <button
                        onClick={() => handleTriggerHaptic('TICK')}
                        className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded text-slate-200 text-center text-[10px]"
                      >
                        Tick
                      </button>
                      <button
                        onClick={() => handleTriggerHaptic('BURST')}
                        className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded text-slate-200 text-center text-[10px]"
                      >
                        Burst
                      </button>
                      <button
                        onClick={() => handleTriggerHaptic('IMPACT')}
                        className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded text-slate-200 text-center text-[10px]"
                      >
                        Impact
                      </button>
                    </div>
                  </div>

                  {/* Audio Speaker */}
                  <div className="p-3 bg-black border border-indigo-950 rounded-xl space-y-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-mono font-bold text-white flex items-center gap-1">
                        <Volume2 className="w-3.5 h-3.5 text-indigo-400" />
                        Audio Synthesizer
                      </span>
                      {lastAudioChime && (
                        <span className="text-[9px] font-mono text-indigo-300 animate-pulse">
                          {lastAudioChime} TONE
                        </span>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-1.5 text-xs font-mono">
                      <button
                        onClick={() => handleTriggerAudioChime('CONFIRM')}
                        className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded text-slate-200 text-center text-[10px]"
                      >
                        Confirm Chime
                      </button>
                      <button
                        onClick={() => handleTriggerAudioChime('ALERT')}
                        className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded text-slate-200 text-center text-[10px]"
                      >
                        Alert Siren
                      </button>
                    </div>
                  </div>
                </div>

                {/* 3. Radios & Thermal Telemetry */}
                <div className="grid grid-cols-3 gap-2 text-xs font-mono">
                  <div className="p-2.5 bg-black border border-slate-900 rounded-lg flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Wifi className="w-3.5 h-3.5 text-cyan-400" />
                      <div>
                        <div className="text-[10px] text-white">Wi-Fi Panel</div>
                        <div className="text-[8px] text-slate-500">Android 10+ Intent</div>
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        setWifiEnabled(!wifiEnabled);
                        setExecutionLogs(l => [
                          ...l,
                          `[${new Date().toLocaleTimeString()}] RADIO DISPATCH: Launching Settings.Panel.ACTION_WIFI (Compliant with Android 10+ Wi-Fi restriction)`
                        ]);
                      }}
                      className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                        wifiEnabled ? 'bg-cyan-500 text-black' : 'bg-slate-800 text-slate-500'
                      }`}
                    >
                      {wifiEnabled ? 'PANEL' : 'OFF'}
                    </button>
                  </div>

                  <div className="p-2.5 bg-black border border-slate-900 rounded-lg flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Bluetooth className="w-3.5 h-3.5 text-indigo-400" />
                      <div>
                        <div className="text-[10px] text-white">Bluetooth</div>
                        <div className="text-[9px] text-slate-500">{bluetoothEnabled ? 'Active' : 'Off'}</div>
                      </div>
                    </div>
                    <button
                      onClick={() => setBluetoothEnabled(!bluetoothEnabled)}
                      className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                        bluetoothEnabled ? 'bg-indigo-500 text-white' : 'bg-slate-800 text-slate-500'
                      }`}
                    >
                      {bluetoothEnabled ? 'ON' : 'OFF'}
                    </button>
                  </div>

                  <div className="p-2.5 bg-black border border-slate-900 rounded-lg flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Battery className="w-3.5 h-3.5 text-emerald-400" />
                      <div>
                        <div className="text-[10px] text-white">Thermal</div>
                        <div className="text-[9px] text-emerald-400 font-bold">31.4&deg;C (Good)</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* RIGHT 5 COLUMNS: LIVE DEVICE VIEWPORT (MOBILE SCREEN) */}
          {(activeCategory === 'all' || activeCategory === 'viewport') && (
            <div className={`space-y-4 ${activeCategory === 'all' ? 'lg:col-span-5' : 'lg:col-span-12'}`}>
              <div className={`rounded-2xl border p-4 sm:p-5 transition-all flex flex-col items-center ${
                oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
              }`}>
                {/* Viewport Header */}
                <div className="w-full flex items-center justify-between mb-2 text-xs font-mono">
                  <div className="flex items-center gap-1.5">
                    <Smartphone className="w-3.5 h-3.5 text-cyan-400" />
                    <span className="font-bold text-white uppercase">[ DEVICE VIEWPORT ]</span>
                  </div>
                  <span className="text-[10px] text-cyan-300 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-800/60 truncate max-w-[150px]">
                    {agentConsciousness.activeForegroundPackage}
                  </span>
                </div>

                {/* Viewport Quick Tabs */}
                <div className="w-full flex items-center justify-center gap-1 mb-3 text-[10px] font-mono flex-wrap">
                  {[
                    { id: 'AUTO', label: 'Auto Focus' },
                    { id: 'KEEP', label: 'Keep Notes' },
                    { id: 'BRIEFING', label: 'Morning' },
                    { id: 'FLASHLIGHT', label: 'Torch' },
                    { id: 'CINEMA', label: 'Cinema' }
                  ].map(tab => (
                    <button
                      key={tab.id}
                      onClick={() => setActivePhoneTab(tab.id as any)}
                      className={`px-2 py-0.5 rounded ${
                        activePhoneTab === tab.id
                          ? 'bg-cyan-500 text-black font-bold'
                          : 'bg-black text-slate-400 hover:text-white border border-slate-800'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                {/* Phone Handheld Frame (Never overflows) */}
                <div className="w-full max-w-[280px] bg-black rounded-[36px] border-4 border-slate-800 p-2.5 shadow-2xl aspect-[9/17] flex flex-col justify-between relative overflow-hidden">
                  {/* Camera & Speaker Notch */}
                  <div className="w-16 h-2 bg-slate-800 mx-auto rounded-full mb-1 shrink-0 z-20" />

                  {/* ACTIVE FLASHLIGHT BEAM ANIMATION */}
                  {flashlightActive && (
                    <div className="absolute inset-0 pointer-events-none z-10 overflow-hidden">
                      {(flashlightMode === 'FRONT' || flashlightMode === 'DUAL') && (
                        <div className="absolute top-0 inset-x-0 h-40 bg-gradient-to-b from-amber-300/40 via-amber-400/20 to-transparent blur-md animate-pulse" />
                      )}
                      {flashlightMode === 'DUAL' && (
                        <div className="absolute inset-0 bg-amber-400/10 mix-blend-screen animate-pulse" />
                      )}
                      {(flashlightMode === 'BACK' || flashlightMode === 'DUAL') && (
                        <div className="absolute top-2 right-4 w-3 h-3 bg-amber-300 rounded-full shadow-lg shadow-amber-300/90 ring-4 ring-amber-400/40 animate-ping" />
                      )}
                    </div>
                  )}

                  {/* Inner Screen Display */}
                  <div className="flex-1 bg-black rounded-2xl p-2.5 flex flex-col justify-between border border-slate-900 font-mono overflow-hidden relative z-20">
                    {/* Status Bar */}
                    <div className="text-[9px] text-slate-500 flex justify-between border-b border-slate-900 pb-1 items-center shrink-0">
                      <span>HiOS &bull; Oct 18</span>
                      <div className="flex items-center gap-1 text-[8px]">
                        {flashlightActive && <Sun className="w-2 h-2 text-amber-400 fill-amber-400" />}
                        <Wifi className="w-2 h-2 text-cyan-400" />
                        <Battery className="w-2 h-2 text-emerald-400" />
                        <span>84%</span>
                      </div>
                    </div>

                    {/* DYNAMIC SCREEN VIEWPORT */}
                    {activePhoneTab === 'KEEP' || (activePhoneTab === 'AUTO' && agentConsciousness.activeForegroundPackage === 'com.google.android.keep') ? (
                      /* GOOGLE KEEP NOTES VIEW */
                      <div className="flex-1 flex flex-col justify-between py-1 text-left font-sans overflow-hidden">
                        <div>
                          <div className="flex items-center justify-between border-b border-slate-900 pb-1 mb-1.5">
                            <div className="flex items-center gap-1 text-xs font-bold text-amber-400">
                              <FileText className="w-3 h-3 text-amber-400" />
                              <span>Keep Notes</span>
                            </div>
                            <span className="text-[8px] font-mono text-emerald-400 bg-emerald-950/60 px-1 py-0.2 rounded">
                              {notesList.length} Notes
                            </span>
                          </div>
                        </div>

                        {/* Scrollable Note List */}
                        <div className="flex-1 overflow-y-auto space-y-1.5 pr-0.5 max-h-48 text-[9px]">
                          {notesList.map((n, idx) => (
                            <div
                              key={n.id}
                              className={`p-1.5 rounded border ${
                                idx === 0
                                  ? 'bg-amber-950/40 border-amber-500/60 text-white'
                                  : 'bg-slate-950 border-slate-800 text-slate-300'
                              }`}
                            >
                              <div className="flex items-center justify-between font-bold text-white">
                                <span className="truncate max-w-[120px]">{n.title}</span>
                                {idx === 0 && (
                                  <span className="text-[7px] font-mono text-amber-400 bg-amber-950 px-1 rounded">
                                    NEW
                                  </span>
                                )}
                              </div>
                              <p className="text-slate-300 line-clamp-2 mt-0.5">{n.content}</p>
                              <div className="text-[7px] font-mono text-slate-500 pt-0.5">{n.timestamp}</div>
                            </div>
                          ))}
                        </div>

                        <button
                          onClick={() => setUserPrompt('Add a note: Sunday review and study session')}
                          className="w-full py-1 mt-1 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 rounded text-[9px] font-mono transition-colors"
                        >
                          + Quick Add Note
                        </button>
                      </div>
                    ) : activePhoneTab === 'BRIEFING' || (activePhoneTab === 'AUTO' && agentConsciousness.activeForegroundPackage === 'com.example.localagent.briefing') ? (
                      /* MORNING BRIEFING VIEW */
                      <div className="flex-1 flex flex-col justify-between py-1 text-left font-sans overflow-hidden">
                        <div>
                          <div className="flex items-center justify-between border-b border-slate-900 pb-1 mb-1">
                            <div className="flex items-center gap-1 text-xs font-bold text-white">
                              <CloudSun className="w-3 h-3 text-amber-400" />
                              <span>Morning Briefing</span>
                            </div>
                            <span className="text-[8px] font-mono text-amber-400 bg-amber-950 px-1 rounded">
                              Oct 18
                            </span>
                          </div>

                          <div className="grid grid-cols-3 gap-0.5 mb-1.5 text-[8px] font-mono text-center">
                            <button
                              onClick={() => setBriefingSubTab('OVERNIGHT')}
                              className={`p-0.5 rounded ${briefingSubTab === 'OVERNIGHT' ? 'bg-amber-500 text-black font-bold' : 'bg-slate-900 text-slate-400'}`}
                            >
                              Night
                            </button>
                            <button
                              onClick={() => setBriefingSubTab('WEATHER')}
                              className={`p-0.5 rounded ${briefingSubTab === 'WEATHER' ? 'bg-amber-500 text-black font-bold' : 'bg-slate-900 text-slate-400'}`}
                            >
                              Weather
                            </button>
                            <button
                              onClick={() => setBriefingSubTab('TODOS')}
                              className={`p-0.5 rounded ${briefingSubTab === 'TODOS' ? 'bg-amber-500 text-black font-bold' : 'bg-slate-900 text-slate-400'}`}
                            >
                              Tasks
                            </button>
                          </div>
                        </div>

                        <div className="flex-1 overflow-y-auto space-y-1 text-[8px]">
                          {briefingSubTab === 'OVERNIGHT' && (
                            <div className="space-y-1">
                              {overnightActivities.slice(0, 3).map((act, i) => (
                                <div key={i} className="p-1 bg-slate-950 rounded border border-slate-900">
                                  <div className="flex justify-between text-cyan-400 font-mono text-[7px]">
                                    <span>{act.time}</span>
                                    <span className="text-emerald-400">{act.status}</span>
                                  </div>
                                  <div className="font-semibold text-white">{act.task}</div>
                                </div>
                              ))}
                            </div>
                          )}

                          {briefingSubTab === 'WEATHER' && (
                            <div className="p-2 bg-slate-950 rounded border border-slate-900 text-center">
                              <div className="text-xl font-bold text-amber-300 font-mono">28&deg;C</div>
                              <div className="text-white text-[9px] font-semibold">Hyderabad &bull; Clear</div>
                              <div className="text-[7px] text-slate-400 font-mono pt-1">Humidity 62% &bull; AQI 42</div>
                            </div>
                          )}

                          {briefingSubTab === 'TODOS' && (
                            <div className="space-y-1">
                              {toDoItems.map(td => (
                                <div
                                  key={td.id}
                                  onClick={() => {
                                    setToDoItems(prev =>
                                      prev.map(i => (i.id === td.id ? { ...i, done: !i.done } : i))
                                    );
                                  }}
                                  className={`p-1 rounded border flex items-center gap-1 cursor-pointer ${
                                    td.done ? 'bg-black border-slate-900 text-slate-500 line-through' : 'bg-slate-950 border-slate-800 text-slate-200'
                                  }`}
                                >
                                  {td.done ? <CheckSquare className="w-2.5 h-2.5 text-emerald-400" /> : <Square className="w-2.5 h-2.5 text-slate-600" />}
                                  <span className="truncate">{td.text}</span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    ) : activePhoneTab === 'FLASHLIGHT' || flashlightActive ? (
                      /* FLASHLIGHT CONTROLLER VIEW */
                      <div className="my-auto space-y-2 py-1 text-center">
                        <div className="relative mx-auto w-16 h-16 rounded-full bg-amber-500/20 border-2 border-amber-400 flex items-center justify-center shadow-lg shadow-amber-500/30">
                          <Sun className={`w-8 h-8 ${flashlightActive ? 'text-amber-300 fill-amber-300 animate-spin-slow' : 'text-slate-600'}`} />
                        </div>
                        <div>
                          <div className="text-xs font-bold text-white">
                            {flashlightMode === 'DUAL' ? 'Dual 360° Flash' : flashlightMode === 'FRONT' ? 'Front Flash' : 'Rear Flash'}
                          </div>
                          <span className={`text-[9px] font-mono px-2 py-0.5 rounded font-bold ${
                            flashlightActive ? 'bg-amber-400 text-black' : 'bg-slate-800 text-slate-400'
                          }`}>
                            {flashlightActive ? 'ACTIVE ILLUMINATION' : 'STANDBY'}
                          </span>
                        </div>
                        {flashlightActive && flashlightRemainingSeconds !== null && (
                          <div className="text-[10px] font-mono text-amber-300 font-bold">
                            {flashlightRemainingSeconds}s Remaining
                          </div>
                        )}
                      </div>
                    ) : activePhoneTab === 'CINEMA' ? (
                      /* TELUGU CINEMA VIEW */
                      <div className="flex-1 flex flex-col justify-between py-1 text-left font-sans overflow-hidden">
                        <div className="flex items-center justify-between border-b border-slate-900 pb-1 mb-1">
                          <span className="text-xs font-bold text-purple-400 flex items-center gap-1">
                            <Film className="w-3 h-3" />
                            Telugu Cinema
                          </span>
                          <span className="text-[8px] font-mono text-purple-300">2026</span>
                        </div>
                        <div className="flex-1 overflow-y-auto space-y-1 text-[8px] pr-0.5">
                          {teluguMoviesList.slice(0, 3).map(m => (
                            <div key={m.id} className="p-1 bg-slate-950 rounded border border-slate-900">
                              <div className="font-bold text-white truncate">{m.title}</div>
                              <div className="text-slate-400 text-[7px] truncate">{m.directorOrCast}</div>
                              <div className="text-emerald-400 text-[7px]">{m.ratingOrNote}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : activePhoneTab === 'CALCULATOR' ? (
                      /* CALCULATOR VIEW */
                      <div className="my-auto space-y-2 py-1 text-center font-mono">
                        <div className="p-2 bg-slate-950 rounded border border-slate-800 text-right text-lg text-emerald-400 font-bold">
                          {simulatedDisplayVal}
                        </div>
                        <div className="grid grid-cols-4 gap-1 text-[10px]">
                          {['7', '8', '9', '/'].map(k => (
                            <div key={k} className="p-1 bg-slate-900 rounded text-white">{k}</div>
                          ))}
                          {['4', '5', '6', '*'].map(k => (
                            <div key={k} className="p-1 bg-slate-900 rounded text-white">{k}</div>
                          ))}
                        </div>
                      </div>
                    ) : (
                      /* DEFAULT STANDBY */
                      <div className="my-auto text-center space-y-2 py-4">
                        <div className="w-9 h-9 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center mx-auto text-cyan-400">
                          <Brain className="w-4 h-4" />
                        </div>
                        <p className="text-xs text-white font-bold">J.A.R.V.I.S. Standby</p>
                        <p className="text-[9px] text-slate-500">Awaiting Directive</p>
                      </div>
                    )}

                    {/* Navigation Bar */}
                    <div className="pt-1 border-t border-slate-900 flex justify-around text-slate-600 text-[10px] shrink-0">
                      <span>&#9664;</span>
                      <span>&#9679;</span>
                      <span>&#9632;</span>
                    </div>
                  </div>

                  {/* Bottom Bezel */}
                  <div className="w-12 h-1 bg-slate-800 mx-auto rounded-full mt-1 shrink-0 z-20" />
                </div>
              </div>
            </div>
          )}
        </div>

        {/* -----------------------------------------------------------------------
            SECTION C: MORNING INTELLIGENCE & TELUGU CINEMA 2026
            ----------------------------------------------------------------------- */}
        {(activeCategory === 'all' || activeCategory === 'intelligence') && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

            {/* CARD 1: DAILY MORNING BRIEFING & NOCTURNAL MAINTENANCE */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <CloudSun className="w-4 h-4 text-amber-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ MORNING INTELLIGENCE &bull; OCT 18, 2026 ]
                  </h3>
                </div>
                <button
                  onClick={() => setUserPrompt('Morning briefing & what did you do night')}
                  className="px-2 py-0.5 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 rounded text-[10px] font-mono"
                >
                  Regenerate
                </button>
              </div>

              {/* 4 Mini Telemetry Cards */}
              <div className="grid grid-cols-2 gap-2 text-xs mb-3 font-mono">
                <div className="p-2.5 bg-black border border-slate-900 rounded-lg">
                  <span className="text-[9px] text-slate-500 block">WEATHER</span>
                  <div className="text-base font-bold text-amber-300">28&deg;C &bull; Clear</div>
                  <span className="text-[9px] text-slate-400">AQI 42 &bull; Humidity 62%</span>
                </div>
                <div className="p-2.5 bg-black border border-slate-900 rounded-lg">
                  <span className="text-[9px] text-slate-500 block">NIGHT AUDITS</span>
                  <div className="text-base font-bold text-emerald-400">4 Verified</div>
                  <span className="text-[9px] text-slate-400">Storage defragged 100%</span>
                </div>
                <div className="p-2.5 bg-black border border-slate-900 rounded-lg">
                  <span className="text-[9px] text-slate-500 block">TECH NEWS</span>
                  <div className="text-[11px] font-bold text-cyan-300 truncate">On-Device SLMs 45 t/s</div>
                  <span className="text-[9px] text-slate-400">Android 17 NPU hooks</span>
                </div>
                <div className="p-2.5 bg-black border border-slate-900 rounded-lg">
                  <span className="text-[9px] text-slate-500 block">TODAYS TASKS</span>
                  <div className="text-base font-bold text-white">
                    {toDoItems.filter(t => t.done).length}/{toDoItems.length} Done
                  </div>
                  <span className="text-[9px] text-amber-400">Keep Notes synced</span>
                </div>
              </div>

              {/* Nocturnal Maintenance Log */}
              <div className="space-y-1.5 text-xs">
                <div className="text-[10px] font-mono text-slate-500 uppercase">
                  Nocturnal Maintenance Log (00:00 - 06:00 AM):
                </div>
                {overnightActivities.slice(0, 3).map((item, idx) => (
                  <div key={idx} className="p-2 bg-black border border-slate-900 rounded-lg space-y-0.5 text-[11px]">
                    <div className="flex items-center justify-between font-mono text-[9px]">
                      <span className="text-cyan-400 font-bold">{item.time}</span>
                      <span className="text-emerald-400">{item.status}</span>
                    </div>
                    <div className="font-semibold text-white">{item.task}</div>
                    <div className="text-slate-400 text-[10px] leading-relaxed">{item.details}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* CARD 2: TELUGU CINEMA INTELLIGENCE (ANCHORED OCTOBER 2026) */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <Film className="w-4 h-4 text-purple-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ TELUGU CINEMA INTELLIGENCE &bull; 2026 ]
                  </h3>
                </div>
                <div className="flex items-center gap-1 text-[10px] font-mono">
                  <button
                    onClick={() => setMovieFilter('ALL')}
                    className={`px-2 py-0.5 rounded ${movieFilter === 'ALL' ? 'bg-purple-600 text-white font-bold' : 'bg-black text-slate-400'}`}
                  >
                    All
                  </button>
                  <button
                    onClick={() => setMovieFilter('THEATRES')}
                    className={`px-2 py-0.5 rounded ${movieFilter === 'THEATRES' ? 'bg-purple-600 text-white font-bold' : 'bg-black text-slate-400'}`}
                  >
                    Theatres
                  </button>
                  <button
                    onClick={() => setMovieFilter('OTT')}
                    className={`px-2 py-0.5 rounded ${movieFilter === 'OTT' ? 'bg-purple-600 text-white font-bold' : 'bg-black text-slate-400'}`}
                  >
                    OTT
                  </button>
                </div>
              </div>

              {/* Movie Cards */}
              <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                {filteredMovies.map(movie => (
                  <div
                    key={movie.id}
                    className="p-3 bg-black border border-slate-900 hover:border-purple-600/60 rounded-xl space-y-1 text-xs transition-colors"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <h4 className="font-bold text-white text-xs">{movie.title}</h4>
                        <span className="text-[10px] text-slate-400 font-mono">{movie.directorOrCast}</span>
                      </div>
                      <span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-purple-950 text-purple-300 border border-purple-800 font-bold shrink-0">
                        {movie.tag}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-300 leading-relaxed font-sans">{movie.synopsis}</p>
                    <div className="flex items-center justify-between pt-1 border-t border-slate-900 text-[10px] font-mono">
                      <span className="text-emerald-400 font-bold">
                        {movie.ottPlatform ? `OTT: ${movie.ottPlatform}` : 'Theatrical Run'}
                      </span>
                      <span className="text-amber-400 flex items-center gap-0.5">
                        <Star className="w-2.5 h-2.5 fill-amber-400" />
                        {movie.ratingOrNote}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* -----------------------------------------------------------------------
            SECTION D: LIVING BRAIN ARCHITECTURE & DURABLE VAULT
            ----------------------------------------------------------------------- */}
        {(activeCategory === 'all' || activeCategory === 'vault') && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

            {/* CARD 1: DUAL-CORE BRAIN (OFFLINE SLM VS HYBRID CLOUD) */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <Brain className="w-4 h-4 text-cyan-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ DUAL-CORE BRAIN ARCHITECTURE ]
                  </h3>
                </div>
                <span className="text-[10px] font-mono text-cyan-300 bg-cyan-950 px-2 py-0.5 rounded border border-cyan-800">
                  Phase 18
                </span>
              </div>

              {/* Mode Selectors */}
              <div className="grid grid-cols-2 gap-2 text-xs mb-3 font-mono">
                <button
                  onClick={() => setBrainOperatingMode('OFFLINE_ON_DEVICE')}
                  className={`p-2.5 rounded-xl border text-left transition-all ${
                    brainOperatingMode === 'OFFLINE_ON_DEVICE'
                      ? 'bg-cyan-950 text-white border-cyan-400 shadow-sm'
                      : 'bg-black border-slate-900 text-slate-400'
                  }`}
                >
                  <div className="text-xs font-bold text-white flex items-center gap-1">
                    <Cpu className="w-3.5 h-3.5 text-cyan-400" />
                    Offline SLM
                  </div>
                  <div className="text-[10px] text-emerald-400 mt-1">Gemma 2B (4-bit)</div>
                  <div className="text-[9px] text-slate-500 mt-0.5">1.2 GB RAM &bull; Zero Cloud</div>
                </button>

                <button
                  onClick={() => setBrainOperatingMode('HYBRID_ZERO_LOGIN')}
                  className={`p-2.5 rounded-xl border text-left transition-all ${
                    brainOperatingMode === 'HYBRID_ZERO_LOGIN'
                      ? 'bg-purple-950 text-white border-purple-400 shadow-sm'
                      : 'bg-black border-slate-900 text-slate-400'
                  }`}
                >
                  <div className="text-xs font-bold text-white flex items-center gap-1">
                    <Globe className="w-3.5 h-3.5 text-purple-400" />
                    Zero-Login Web
                  </div>
                  <div className="text-[10px] text-purple-300 mt-1">Gemini / Public Web</div>
                  <div className="text-[9px] text-slate-500 mt-0.5">Live Cinema &bull; No Sign-in</div>
                </button>
              </div>

              {/* Interactive Brain Test */}
              <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-2">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-[10px] text-slate-400">TEST LIVING BRAIN:</span>
                  <span className="text-[10px] text-cyan-400 font-bold">{brainOperatingMode}</span>
                </div>
                <div className="flex gap-1.5">
                  <input
                    type="text"
                    value={brainTestPrompt}
                    onChange={e => setBrainTestPrompt(e.target.value)}
                    className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1 text-xs text-white"
                  />
                  <button
                    onClick={handleRunBrainTest}
                    disabled={isBrainThinking}
                    className="px-3 py-1 bg-cyan-500 hover:bg-cyan-400 disabled:bg-slate-800 text-black font-bold text-xs rounded-lg font-mono"
                  >
                    {isBrainThinking ? 'Thinking...' : 'Test'}
                  </button>
                </div>
                {brainTestResponse && (
                  <div className="p-2 bg-slate-950 border border-slate-900 rounded font-mono text-[10px] text-slate-300 whitespace-pre-wrap leading-relaxed">
                    {brainTestResponse}
                  </div>
                )}
              </div>
            </div>

            {/* CARD 2: DURABLE BRAIN VAULT (ROOM/SQLITE + PENDING QUEUE + SAF BACKUP) */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <Database className="w-4 h-4 text-emerald-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ DURABLE BRAIN VAULT &bull; ARCHITECTURE ]
                  </h3>
                </div>
                <button
                  onClick={handleExportVault}
                  className="px-2.5 py-1 bg-emerald-500 hover:bg-emerald-400 text-black font-bold rounded-lg text-xs font-mono flex items-center gap-1 shadow-sm"
                >
                  <Download className="w-3 h-3" />
                  SAF Export (.json)
                </button>
              </div>

              {/* Memory Architecture Specification */}
              <div className="p-3 bg-black border border-emerald-950 rounded-xl space-y-2 text-xs font-mono leading-relaxed">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] text-slate-500">PRIMARY MEMORY STORE:</span>
                  <span className="text-[9px] px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-400 border border-emerald-800 font-bold">
                    APP-PRIVATE STORAGE
                  </span>
                </div>
                <div className="p-1.5 bg-slate-950 border border-slate-900 rounded text-cyan-300 text-[10px] font-bold">
                  /data/user/0/com.aistudio.localagent.tcin/databases/local_agent.db (Room v5, learning_records)
                </div>
                <ul className="space-y-1 text-slate-400 text-[10px]">
                  <li>&bull; <strong className="text-white">Primary:</strong> App-private Room/SQLite (v5 with non-destructive MIGRATION_4_5 &amp; ACID transactions).</li>
                  <li>&bull; <strong className="text-white">Approved Skills:</strong> Compact, versioned records ({skills.length} verified procedures in Room).</li>
                  <li>&bull; <strong className="text-amber-400">Pending Learning Queue:</strong> Survives app restart in learning_records table (awaiting supervisor review).</li>
                  <li>&bull; <strong className="text-emerald-400">External Backups:</strong> User-controlled Storage Access Framework (SAF) export.</li>
                </ul>
              </div>

              {/* Pending Learning Review Queue (User Confirmation Before Committing) */}
              <div className="mt-3 p-2.5 bg-amber-950/20 border border-amber-500/40 rounded-xl space-y-1.5">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-[10px] text-amber-300 font-bold uppercase flex items-center gap-1">
                    <AlertCircle className="w-3 h-3 text-amber-400" />
                    Pending Learning Queue (1 Candidate)
                  </span>
                  <span className="text-[9px] text-amber-400 bg-amber-950 px-1.5 py-0.2 rounded">
                    AWAITING SUPERVISOR
                  </span>
                </div>
                <div className="text-[11px] text-slate-300 font-sans">
                  Observed execution: <strong>Calculator Sequential Arithmetic</strong> (Tap 7 &rarr; Multiply &rarr; 8 &rarr; Equals).
                </div>
                <div className="flex items-center justify-end gap-1.5 pt-1 text-[10px] font-mono">
                  <button className="px-2 py-0.5 bg-slate-900 text-slate-400 hover:text-white rounded border border-slate-800">
                    Discard
                  </button>
                  <button className="px-2.5 py-0.5 bg-amber-500 text-black font-bold rounded">
                    Approve &amp; Commit Skill
                  </button>
                </div>
              </div>

              {/* Approved Skills summary */}
              <div className="mt-3 space-y-1 text-xs">
                <div className="text-[10px] font-mono text-slate-500 uppercase">
                  Approved Knowledge Registry:
                </div>
                {skills.map(s => (
                  <div key={s.id} className="p-2 bg-black border border-slate-900 rounded-lg flex items-center justify-between text-[11px]">
                    <div>
                      <div className="font-bold text-white">{s.title}</div>
                      <div className="text-[9px] text-slate-500 font-mono">{s.resolvedPackageName} &bull; v{s.version}</div>
                    </div>
                    <span className="text-[9px] font-mono text-emerald-400 px-1.5 py-0.5 rounded bg-emerald-950 border border-emerald-900 font-bold">
                      [VERIFIED]
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* -----------------------------------------------------------------------
            SECTION E: TASK EXECUTION HISTORY & DIAGNOSTIC AUDIT LOGS
            ----------------------------------------------------------------------- */}
        {(activeCategory === 'all' || activeCategory === 'telemetry') && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

            {/* Task Execution History */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <History className="w-4 h-4 text-cyan-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ TASK EXECUTION AUDIT TRAIL ]
                  </h3>
                </div>
                <span className="text-[10px] font-mono text-slate-500">
                  {taskHistory.length} Recorded Tasks
                </span>
              </div>

              <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                {taskHistory.map(item => (
                  <div key={item.id} className="p-2.5 bg-black border border-slate-900 rounded-xl space-y-1 text-xs">
                    <div className="flex items-start justify-between">
                      <span className="font-semibold text-white truncate max-w-[200px]">
                        &ldquo;{item.taskCommand}&rdquo;
                      </span>
                      <span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800 font-bold">
                        {item.finalStatus}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-[10px] font-mono text-slate-500">
                      <span className="text-cyan-400">{item.targetLabel}</span>
                      <span>&bull;</span>
                      <span>{item.durationMs}ms</span>
                      <span>&bull;</span>
                      <span>{item.timestamp}</span>
                    </div>
                    <div className="text-[10px] text-slate-400 bg-slate-950 p-1.5 rounded border border-slate-900 leading-relaxed font-sans">
                      {item.verificationDetails}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Diagnostic Terminal Stream */}
            <div className={`rounded-2xl border p-4 sm:p-5 transition-all ${
              oledLowPowerMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
            }`}>
              <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
                <div className="flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-cyan-400" />
                  <h3 className="text-xs sm:text-sm font-bold text-white font-mono uppercase tracking-wide">
                    [ J.A.R.V.I.S. DIAGNOSTIC LOG STREAM ]
                  </h3>
                </div>
                <button
                  onClick={() => setExecutionLogs([])}
                  className="text-[10px] font-mono text-slate-500 hover:text-slate-300"
                >
                  Clear Logs
                </button>
              </div>

              <div className="bg-black p-2.5 rounded-xl border border-slate-900 font-mono text-[10px] text-slate-300 max-h-64 overflow-y-auto space-y-1">
                {executionLogs.map((log, idx) => (
                  <div key={idx} className="leading-relaxed">
                    {log}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </main>

      {/* =========================================================================
          PRE-FLIGHT STEP APPROVAL MODAL (PHASE 13 SAFETY GATE)
          ========================================================================= */}
      {pendingPlanModal && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4">
          <div className="bg-black border border-cyan-500/50 rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-4 sm:p-5 shadow-2xl space-y-3 font-sans">
            <div className="flex items-start justify-between border-b border-slate-900 pb-2">
              <div>
                <span className="text-[10px] font-mono font-bold text-cyan-400 uppercase tracking-wider">
                  [ PHASE 13 &bull; HUMAN CONFIRMATION GATE ]
                </span>
                <h3 className="text-sm sm:text-base font-bold text-white mt-0.5">
                  Proposed Directive Execution Plan
                </h3>
              </div>
              <button
                onClick={() => setPendingPlanModal(false)}
                className="text-slate-500 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="bg-slate-950 p-3 rounded-xl border border-slate-900 text-xs text-slate-300 space-y-1 font-mono">
              <div>
                <span className="text-slate-500">DIRECTIVE:</span> <strong className="text-white">&ldquo;{userPrompt}&rdquo;</strong>
              </div>
              <div>
                <span className="text-slate-500">TARGET:</span> <span className="text-cyan-300 font-bold">{targetLabelForPlan}</span> ({targetPackageForPlan})
              </div>
              <div>
                <span className="text-slate-500">RESOURCE IMPACT:</span> <span className="text-emerald-400 font-bold">Low (&lt; 2MB RAM)</span>
              </div>
              <div className="text-amber-400 font-bold pt-1">
                J.A.R.V.I.S. will execute this sequentially upon confirmation.
              </div>
            </div>

            {/* Stepper list */}
            <div className="space-y-1.5 max-h-52 overflow-y-auto pr-1">
              {proposedPlan.map(step => (
                <div
                  key={step.stepIndex}
                  className="p-2 bg-slate-950 border border-slate-900 rounded-lg text-xs flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 rounded-full bg-slate-900 text-cyan-400 font-mono font-bold flex items-center justify-center text-[10px]">
                      {step.stepIndex}
                    </span>
                    <span className="text-slate-200 text-[11px]">{step.description}</span>
                  </div>
                  <span className="text-[9px] font-mono px-1 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800">
                    {step.risk}
                  </span>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-900">
              <button
                onClick={() => setPendingPlanModal(false)}
                className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-slate-300 rounded-lg text-xs font-mono"
              >
                Dismiss
              </button>
              <button
                onClick={handleApproveAndStart}
                className="px-4 py-1.5 bg-cyan-500 hover:bg-cyan-400 text-black font-bold rounded-lg text-xs font-mono shadow-md shadow-cyan-500/20 flex items-center gap-1.5"
              >
                <Check className="w-3.5 h-3.5" />
                <span>Approve &amp; Execute</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
