import React, { useState } from 'react';
import {
  TrendingUp,
  RotateCw,
  Sparkles,
  Check,
  Zap,
  Battery,
  ShieldCheck,
  Clock,
  Cpu,
  Layers,
  ArrowRight,
  Database,
  Moon,
  Sun,
  Activity,
  History,
  FileCode,
  CheckCircle2,
  AlertCircle,
  Sliders,
  Flame,
  Music,
  Gauge,
  RefreshCw,
  Radio
} from 'lucide-react';

export interface EvolutionDayData {
  dayNumber: number;
  dateString: string;
  tasksExecuted: number;
  avgExecutionLatencyMs: number;
  selfHealingSuccessCount: number;
  batteryEfficiencyPct: number;
  idleRamFootprintMb: number;
  agentProficiencyScore: number;
  improvementsApplied: string[];
  learnedFastPaths: string[];
  paretoEnergyJoules: number;
  thermalRiseDegC: number;
  harmonicScorePct: number;
}

export interface HealedSkillData {
  id: string;
  appLabel: string;
  targetPackage: string;
  originalFailureCause: string;
  healedStrategy: string;
  latencyBeforeMs: number;
  latencyAfterMs: number;
  confidence: number;
  version: string;
  daysActive: number;
}

export interface BayesianHeuristic {
  id: string;
  name: string;
  description: string;
  successCount: number;
  failureCount: number;
  observedLatencyMs: number;
  confidence: number;
  tier: 'PROVEN' | 'REINFORCING' | 'EXPLORATORY';
}

interface DayByDayEvolutionCenterProps {
  oledMode: boolean;
  onSyncVault?: (detail: string) => void;
}

export const DayByDayEvolutionCenter: React.FC<DayByDayEvolutionCenterProps> = ({
  oledMode,
  onSyncVault
}) => {
  type CenterTab = 'timeline' | 'governor' | 'pareto';
  const [activeCenterTab, setActiveCenterTab] = useState<CenterTab>('timeline');

  const [currentDay, setCurrentDay] = useState<number>(5);
  const [isEvolving, setIsEvolving] = useState<boolean>(false);
  const [evolutionStepText, setEvolutionStepText] = useState<string | null>(null);
  const [selectedDayNumber, setSelectedDayNumber] = useState<number>(5);

  // Helio P23 Hardware Governor & Tuner State
  type GovernorMode = 'EFFICIENCY_DAEMON' | 'DYNAMIC_BURST_DISPATCH' | 'THERMAL_EMERGENCY_COOL' | 'AC_CHARGING_HIGH_PERF';
  const [governorMode, setGovernorMode] = useState<GovernorMode>('DYNAMIC_BURST_DISPATCH');
  const [isCompactingMemory, setIsCompactingMemory] = useState<boolean>(false);
  const [compactionToast, setCompactionToast] = useState<string | null>(null);
  const [currentHeapMb, setCurrentHeapMb] = useState<number>(39.8);
  const [socTempDegC, setSocTempDegC] = useState<number>(37.2);
  const [flashCoolDownMs, setFlashCoolDownMs] = useState<number>(120);
  const [uiActionIntervalMs, setUiActionIntervalMs] = useState<number>(35);
  const [pcmBufferSizeFrames, setPcmBufferSizeFrames] = useState<number>(1024);

  const [history, setHistory] = useState<EvolutionDayData[]>([
    {
      dayNumber: 1,
      dateString: 'Day 1: Cold Start & Device Topology',
      tasksExecuted: 14,
      avgExecutionLatencyMs: 1450,
      selfHealingSuccessCount: 2,
      batteryEfficiencyPct: 62.0,
      idleRamFootprintMb: 44.5,
      agentProficiencyScore: 72.0,
      improvementsApplied: [
        'Initial accessibility hierarchy mapping across 18 pre-installed apps',
        'Cached package metadata & launcher activities in local Room DB (v6)',
        'Established MediaTek Helio P23 CorePilot thermal baseline (<42°C)'
      ],
      learnedFastPaths: ['Basic Intent.ACTION_MAIN launching'],
      paretoEnergyJoules: 1.45,
      thermalRiseDegC: 2.4,
      harmonicScorePct: 65.0
    },
    {
      dayNumber: 2,
      dateString: 'Day 2: Accessibility Heuristic Tuning',
      tasksExecuted: 28,
      avgExecutionLatencyMs: 1180,
      selfHealingSuccessCount: 5,
      batteryEfficiencyPct: 71.5,
      idleRamFootprintMb: 43.1,
      agentProficiencyScore: 79.5,
      improvementsApplied: [
        'Auto-healed Google Keep note creation: Replaced text selector with contentDescription',
        'Implemented 64-bit FNV-1a hashing for O(1) screen mutation verification',
        'Cut background idle polling interval when screen is dark from 5s to 30s'
      ],
      learnedFastPaths: ['Google Keep direct text intent injection'],
      paretoEnergyJoules: 1.15,
      thermalRiseDegC: 2.0,
      harmonicScorePct: 74.0
    },
    {
      dayNumber: 3,
      dateString: 'Day 3: Memory Defragmentation & Power Capping',
      tasksExecuted: 36,
      avgExecutionLatencyMs: 920,
      selfHealingSuccessCount: 8,
      batteryEfficiencyPct: 81.0,
      idleRamFootprintMb: 41.8,
      agentProficiencyScore: 86.0,
      improvementsApplied: [
        'Synthesized Calculator fast-macro: Keystroke interval reduced to 40ms',
        'Learned ambient lux profile: Automatically suppresses Rear Quad-LED in dark rooms (<20 lx)',
        'Executed Room FTS5 index VACUUM, reclaiming 4.2 MB external flash storage'
      ],
      learnedFastPaths: ['Calculator zero-click evaluation', 'Keep direct URI note append'],
      paretoEnergyJoules: 0.88,
      thermalRiseDegC: 1.6,
      harmonicScorePct: 82.0
    },
    {
      dayNumber: 4,
      dateString: 'Day 4: Cross-App Memory & Procedural Audio',
      tasksExecuted: 42,
      avgExecutionLatencyMs: 780,
      selfHealingSuccessCount: 11,
      batteryEfficiencyPct: 88.5,
      idleRamFootprintMb: 40.4,
      agentProficiencyScore: 91.5,
      improvementsApplied: [
        'Cross-App Memory Stack: Automatic SMS OTP extraction without foregrounding Messaging app',
        'Procedural 44.1 kHz PCM audio generation eliminates bulky sound assets (saved 12MB storage)',
        'Auto-recovery for unexpected system permission dialogs with automated Allow click'
      ],
      learnedFastPaths: ['SMS OTP -> Bank verification direct pipeline', 'WhatsApp voice quick dispatch'],
      paretoEnergyJoules: 0.62,
      thermalRiseDegC: 1.2,
      harmonicScorePct: 90.0
    },
    {
      dayNumber: 5,
      dateString: 'Day 5: Production Peak & Lockstep Synesthesia',
      tasksExecuted: 58,
      avgExecutionLatencyMs: 640,
      selfHealingSuccessCount: 15,
      batteryEfficiencyPct: 94.2,
      idleRamFootprintMb: 39.8,
      agentProficiencyScore: 97.4,
      improvementsApplied: [
        'Lockstep Synesthesia: Synchronized vibrations and dual-flash strobes with song beats (<3.2ms jitter)',
        'Zero-leak mmap model lifecycle enforces strict 39.8MB idle memory (48MB ceiling safe)',
        'Helio P23 big.LITTLE core pinning: Dedicated high-efficiency Cortex-A53 cluster for background daemon'
      ],
      learnedFastPaths: [
        'Autonomous Cinema graph queries',
        'One-touch flashlight emergency Morse SOS beacon',
        'Continuous nocturnal SQLite defragmentation'
      ],
      paretoEnergyJoules: 0.42,
      thermalRiseDegC: 0.8,
      harmonicScorePct: 96.5
    }
  ]);

  const [healedSkills] = useState<HealedSkillData[]>([
    {
      id: 'hs-1',
      appLabel: 'Google Keep',
      targetPackage: 'com.google.android.keep',
      originalFailureCause: "App update relocated 'New note' FAB from bottom-right to center with altered viewId",
      healedStrategy: "Fallback to semantic contentDescription 'New text note' + relative coordinate ratio tap (0.50, 0.94)",
      latencyBeforeMs: 2850,
      latencyAfterMs: 420,
      confidence: 0.99,
      version: 'v2.3',
      daysActive: 4
    },
    {
      id: 'hs-2',
      appLabel: 'Calculator',
      targetPackage: 'com.transsion.calculator',
      originalFailureCause: 'System accessibility font scaling caused exact text label matching to fail on operator buttons',
      healedStrategy: 'Switched to accessible viewId matching combined with bounding-box ratio validation',
      latencyBeforeMs: 1920,
      latencyAfterMs: 310,
      confidence: 0.98,
      version: 'v2.1',
      daysActive: 3
    },
    {
      id: 'hs-3',
      appLabel: 'Settings',
      targetPackage: 'com.android.settings',
      originalFailureCause: "Wi-Fi toggle relocated behind sub-menu 'Network & Internet' in Android 8.1 Oreo",
      healedStrategy: 'Direct explicit Intent dispatch Intent(Settings.ACTION_WIFI_SETTINGS) bypassing 3 intermediate UI clicks',
      latencyBeforeMs: 3400,
      latencyAfterMs: 280,
      confidence: 1.0,
      version: 'v3.0',
      daysActive: 5
    },
    {
      id: 'hs-4',
      appLabel: 'Camera Flash Torch',
      targetPackage: 'android.hardware.camera2',
      originalFailureCause: 'Consecutive quad-LED firings generated thermal ramp approaching MediaTek driver threshold (42°C)',
      healedStrategy: 'Applied 150ms cool-down guard intervals and shifted non-critical pulses to Front Dual-LED',
      latencyBeforeMs: 480,
      latencyAfterMs: 85,
      confidence: 0.99,
      version: 'v2.0',
      daysActive: 2
    }
  ]);

  const [bayesianHeuristics, setBayesianHeuristics] = useState<BayesianHeuristic[]>([
    {
      id: 'intent_deep_link',
      name: 'Explicit Intent Deep-Link',
      description: 'Bypasses multi-step screen navigation using direct explicit Intent URIs',
      successCount: 142,
      failureCount: 1,
      observedLatencyMs: 45,
      confidence: 0.986,
      tier: 'PROVEN'
    },
    {
      id: 'content_desc_ratio',
      name: 'ContentDescription + Ratio',
      description: 'Matches accessibility semantics with viewport normalized bounding box ratios',
      successCount: 98,
      failureCount: 2,
      observedLatencyMs: 110,
      confidence: 0.971,
      tier: 'PROVEN'
    },
    {
      id: 'resource_id_fast_slice',
      name: 'Resource-ID Fast Slice',
      description: 'Direct viewId lookup scoped strictly to visible on-screen layout subtrees',
      successCount: 76,
      failureCount: 5,
      observedLatencyMs: 65,
      confidence: 0.928,
      tier: 'REINFORCING'
    },
    {
      id: 'haptic_transient_sync',
      name: 'Lockstep Haptic Beat Sync',
      description: 'Sub-3ms tactile impulse triggered concurrently with bass drop transients',
      successCount: 188,
      failureCount: 1,
      observedLatencyMs: 2,
      confidence: 0.989,
      tier: 'PROVEN'
    },
    {
      id: 'flash_pwm_governor',
      name: 'Quad-LED PWM Governor',
      description: 'Persistence-of-vision micro-pulses protecting Helio P23 torch drivers',
      successCount: 210,
      failureCount: 0,
      observedLatencyMs: 15,
      confidence: 0.995,
      tier: 'PROVEN'
    }
  ]);

  const latestDay = history[history.length - 1];
  const selectedDay = history.find(d => d.dayNumber === selectedDayNumber) || latestDay;

  // Trigger simulated overnight evolution pass
  const handleTriggerOvernightPass = () => {
    if (isEvolving) return;
    setIsEvolving(true);

    const steps = [
      'Phase 1/6: Mining daily SQLite interaction traces & identifying UI friction points...',
      'Phase 2/6: Auto-repairing degraded accessibility selectors & updating Bayesian weights...',
      'Phase 3/6: Synthesizing atomic fast-path macros (bypassing multi-step screen traversals)...',
      'Phase 4/6: Executing SQLite FTS5 index VACUUM & pruning stale logs (reclaiming storage)...',
      'Phase 5/6: Recalibrating ambient lux & thermal thresholds for MediaTek Helio P23...',
      'Phase 6/6: Atomically syncing updated skills to /sdcard/LocalAgent/brain_vault.json...'
    ];

    let currentStep = 0;
    const interval = setInterval(() => {
      if (currentStep < steps.length) {
        setEvolutionStepText(steps[currentStep]);
        currentStep++;
      } else {
        clearInterval(interval);
        // Advance day
        const nextDayNum = currentDay + 1;
        const newRecord: EvolutionDayData = {
          dayNumber: nextDayNum,
          dateString: `Day ${nextDayNum}: Autonomous Lifelong Refinement`,
          tasksExecuted: latestDay.tasksExecuted + 14,
          avgExecutionLatencyMs: Math.max(310, Math.round(latestDay.avgExecutionLatencyMs * 0.91)),
          selfHealingSuccessCount: latestDay.selfHealingSuccessCount + 3,
          batteryEfficiencyPct: Math.min(99.0, Number((latestDay.batteryEfficiencyPct + 1.1).toFixed(1))),
          idleRamFootprintMb: Math.max(37.5, Number((latestDay.idleRamFootprintMb - 0.3).toFixed(1))),
          agentProficiencyScore: Math.min(99.6, Number((latestDay.agentProficiencyScore + 0.6).toFixed(1))),
          improvementsApplied: [
            `Overnight FTS5 SQLite index vacuum pruned transient logs, reclaiming 3.1 MB storage`,
            `Synthesized direct Intent fast-path for background app switching (cutting latency by 9%)`,
            `Recalibrated ambient lux threshold curve to match user's room illumination habits`,
            `Reinforced Bayesian confidence scores across all registered accessibility skills`
          ],
          learnedFastPaths: [...latestDay.learnedFastPaths, `Day ${nextDayNum} Synthesized Intent Macro`],
          paretoEnergyJoules: Math.max(0.24, Number((latestDay.paretoEnergyJoules * 0.88).toFixed(2))),
          thermalRiseDegC: Math.max(0.4, Number((latestDay.thermalRiseDegC * 0.85).toFixed(1))),
          harmonicScorePct: Math.min(99.2, Number((latestDay.harmonicScorePct + 0.8).toFixed(1)))
        };

        setHistory(prev => [...prev, newRecord]);
        setCurrentDay(nextDayNum);
        setSelectedDayNumber(nextDayNum);
        setCurrentHeapMb(newRecord.idleRamFootprintMb);
        setSocTempDegC(Math.max(35.8, Number((socTempDegC - 0.2).toFixed(1))));
        setUiActionIntervalMs(Math.max(20, Math.round(uiActionIntervalMs * 0.92)));
        setIsEvolving(false);
        setEvolutionStepText(null);

        // Update heuristics confidence
        setBayesianHeuristics(prev =>
          prev.map(h => ({
            ...h,
            successCount: h.successCount + 12,
            confidence: Number(((h.successCount + 13) / (h.successCount + h.failureCount + 14)).toFixed(3))
          }))
        );

        if (onSyncVault) {
          onSyncVault(`Overnight Evolution Complete: Advanced to Day ${nextDayNum} (Proficiency: ${newRecord.agentProficiencyScore}%, Latency: ${newRecord.avgExecutionLatencyMs}ms)`);
        }
      }
    }, 550);
  };

  // Trigger Proactive Memory Compaction (Simulating System.gc() & buffer eviction)
  const handleProactiveCompaction = () => {
    if (isCompactingMemory) return;
    setIsCompactingMemory(true);
    setCompactionToast('Executing ART Low-Memory Proactive Compaction & buffer eviction...');

    setTimeout(() => {
      const reclaimed = (Math.random() * 2.5 + 1.2).toFixed(1);
      const newHeap = Math.max(36.8, Number((currentHeapMb - parseFloat(reclaimed)).toFixed(1)));
      setCurrentHeapMb(newHeap);
      setIsCompactingMemory(false);
      setCompactionToast(`[PASS] Reclaimed ${reclaimed} MB. Heap at ${newHeap} MB (<48.0 MB safe).`);
      setTimeout(() => setCompactionToast(null), 3000);
      if (onSyncVault) {
        onSyncVault(`Memory Compaction: Reclaimed ${reclaimed} MB heap (Current: ${newHeap} MB)`);
      }
    }, 700);
  };

  return (
    <div
      className={`rounded-2xl border p-4 sm:p-5 transition-all space-y-5 ${
        oledMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
      }`}
    >
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-900 pb-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-gradient-to-br from-emerald-950 to-cyan-950 border border-emerald-800 text-emerald-400">
            <TrendingUp className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-white tracking-wide font-mono uppercase">
                [ DAY-BY-DAY CONTINUOUS EVOLUTION &bull; LIFELONG ADAPTATION ENGINE ]
              </h2>
              <span className="px-2 py-0.5 rounded bg-emerald-950 border border-emerald-700 text-emerald-300 text-[10px] font-mono font-bold">
                Day {currentDay} Active
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500 font-mono mt-0.5">
              <span>Helio P23 Constraints Verified</span>
              <span aria-hidden="true">&bull;</span>
              <span>Self-Healing UI Resilience</span>
              <span aria-hidden="true">&bull;</span>
              <span>Nocturnal VACUUM Optimization</span>
              <span aria-hidden="true">&bull;</span>
              <span className="text-cyan-400 font-semibold">100% Offline-First</span>
            </div>
          </div>
        </div>

        {/* Action Button: Trigger Overnight Evolution */}
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button
            onClick={handleTriggerOvernightPass}
            disabled={isEvolving}
            className="w-full sm:w-auto min-h-[44px] px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 active:from-emerald-600 active:to-cyan-600 text-black font-bold text-xs rounded-xl font-mono flex items-center justify-center gap-2 transition-all shadow-md shadow-emerald-950/40 disabled:opacity-50"
          >
            <RotateCw className={`w-4 h-4 shrink-0 ${isEvolving ? 'animate-spin' : ''}`} />
            <span>{isEvolving ? 'Evolving Overnight...' : 'Trigger Overnight Evolution Pass'}</span>
          </button>
        </div>
      </div>

      {/* Evolution Step Progress Banner (When running) */}
      {isEvolving && evolutionStepText && (
        <div className="p-3 bg-gradient-to-r from-emerald-950/90 to-cyan-950/90 border border-emerald-500/60 rounded-xl text-xs font-mono text-emerald-300 flex items-center gap-2 animate-pulse">
          <Sparkles className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{evolutionStepText}</span>
        </div>
      )}

      {/* Sub-tab Navigation */}
      <div className="flex items-center gap-2 border-b border-slate-900 pb-2 text-xs font-mono overflow-x-auto no-scrollbar">
        <button
          onClick={() => setActiveCenterTab('timeline')}
          className={`min-h-[44px] px-3.5 py-2 rounded-xl border flex items-center gap-1.5 transition-all whitespace-nowrap ${
            activeCenterTab === 'timeline'
              ? 'bg-emerald-950 text-emerald-300 border-emerald-600 font-bold shadow-sm'
              : 'bg-black text-slate-400 hover:text-white border-slate-900'
          }`}
        >
          <History className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
          <span>
            <span className="sm:hidden">Timeline</span>
            <span className="hidden sm:inline">Lifelong Evolution Timeline</span>
          </span>
        </button>

        <button
          onClick={() => setActiveCenterTab('governor')}
          className={`min-h-[44px] px-3.5 py-2 rounded-xl border flex items-center gap-1.5 transition-all whitespace-nowrap ${
            activeCenterTab === 'governor'
              ? 'bg-cyan-950 text-cyan-300 border-cyan-600 font-bold shadow-sm'
              : 'bg-black text-slate-400 hover:text-white border-slate-900'
          }`}
        >
          <Cpu className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
          <span>
            <span className="sm:hidden">Governor</span>
            <span className="hidden sm:inline">Helio P23 Governor &amp; Autotuner</span>
          </span>
        </button>

        <button
          onClick={() => setActiveCenterTab('pareto')}
          className={`min-h-[44px] px-3.5 py-2 rounded-xl border flex items-center gap-1.5 transition-all whitespace-nowrap ${
            activeCenterTab === 'pareto'
              ? 'bg-pink-950 text-pink-300 border-pink-600 font-bold shadow-sm'
              : 'bg-black text-slate-400 hover:text-white border-slate-900'
          }`}
        >
          <Gauge className="w-3.5 h-3.5 text-pink-400 shrink-0" />
          <span>
            <span className="sm:hidden">Pareto</span>
            <span className="hidden sm:inline">Pareto Multi-Objective Frontier</span>
          </span>
        </button>
      </div>

      {/* =========================================================================
          KEY LIFELONG ADAPTATION METRICS (DAY 1 VS CURRENT DAY)
          ========================================================================= */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {/* KPI 1: Execution Latency */}
        <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-1.5">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-amber-400" />
              <span>Task Latency</span>
            </span>
            <span className="text-emerald-400 font-bold text-[10px]">
              -{(((1450 - latestDay.avgExecutionLatencyMs) / 1450) * 100).toFixed(1)}%
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-lg font-bold text-white font-mono">{latestDay.avgExecutionLatencyMs}ms</span>
            <span className="text-xs text-slate-500 font-mono line-through">1,450ms</span>
          </div>
          <p className="text-[10px] text-slate-500 font-sans">Learned direct fast-paths &amp; macros</p>
        </div>

        {/* KPI 2: Self-Healing Success */}
        <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-1.5">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-cyan-400" />
              <span>Self-Healed Skills</span>
            </span>
            <span className="text-cyan-400 font-bold text-[10px]">98.6%</span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-lg font-bold text-white font-mono">{latestDay.selfHealingSuccessCount} Fixes</span>
            <span className="text-xs text-slate-500 font-mono">0 App Crashes</span>
          </div>
          <p className="text-[10px] text-slate-500 font-sans">Adapts to layout &amp; font scale shifts</p>
        </div>

        {/* KPI 3: Idle RAM Footprint */}
        <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-1.5">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5 text-emerald-400" />
              <span>Idle RAM Footprint</span>
            </span>
            <span className="text-emerald-400 font-bold text-[10px]">&lt;48MB OK</span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-lg font-bold text-white font-mono">{currentHeapMb} MB</span>
            <span className="text-xs text-slate-500 font-mono">/ 48.0 MB</span>
          </div>
          <p className="text-[10px] text-slate-500 font-sans">Zero memory leak via mmap eviction</p>
        </div>

        {/* KPI 4: Overall Proficiency */}
        <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-1.5">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-pink-400" />
              <span>Proficiency Index</span>
            </span>
            <span className="text-pink-400 font-bold text-[10px]">+{((latestDay.agentProficiencyScore - 72.0)).toFixed(1)}%</span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-lg font-bold text-white font-mono">{latestDay.agentProficiencyScore}%</span>
            <span className="text-xs text-slate-500 font-mono">Day 1: 72%</span>
          </div>
          <p className="text-[10px] text-slate-500 font-sans">Bayesian reinforced decision graph</p>
        </div>
      </div>

      {/* =========================================================================
          TAB 1: LIFELONG EVOLUTION TIMELINE & SELF-HEALED SKILLS
          ========================================================================= */}
      {activeCenterTab === 'timeline' && (
        <div className="space-y-4">
          {/* Day Selector Timeline & Details */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-slate-400 font-bold uppercase flex items-center gap-2">
                <History className="w-3.5 h-3.5 text-amber-400" />
                <span>DAY-BY-DAY EVOLUTION TIMELINE &bull; SELECT TO INSPECT:</span>
              </span>
              <span className="text-slate-500 text-[10px]">
                {history.length} Continuous Days Recorded
              </span>
            </div>

            {/* Timeline Day Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar text-xs font-mono">
              {history.map(day => {
                const isSelected = day.dayNumber === selectedDayNumber;
                const isLatest = day.dayNumber === currentDay;
                return (
                  <button
                    key={day.dayNumber}
                    onClick={() => setSelectedDayNumber(day.dayNumber)}
                    className={`px-3.5 py-2 rounded-xl border text-left transition-all shrink-0 flex flex-col gap-0.5 ${
                      isSelected
                        ? 'bg-emerald-950/90 text-emerald-300 border-emerald-500 shadow-sm'
                        : 'bg-slate-950 text-slate-400 hover:text-white border-slate-900'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 font-bold">
                      <span>Day {day.dayNumber}</span>
                      {isLatest && (
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                      )}
                    </div>
                    <div className="text-[9px] text-slate-500 flex items-center gap-2">
                      <span>{day.avgExecutionLatencyMs}ms</span>
                      <span>&bull;</span>
                      <span className="text-emerald-400 font-semibold">{day.agentProficiencyScore}%</span>
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Selected Day Breakdown Card */}
            <div className="p-3.5 bg-slate-950 border border-slate-900 rounded-lg space-y-3 font-mono text-xs">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 border-b border-slate-900 pb-2">
                <div>
                  <span className="text-white font-bold text-sm">{selectedDay.dateString}</span>
                  <p className="text-[10px] text-slate-400 font-sans mt-0.5">
                    {selectedDay.tasksExecuted} Tasks Executed &bull; {selectedDay.selfHealingSuccessCount} Self-Heals Verified
                  </p>
                </div>
                <div className="flex items-center gap-3 text-[10px]">
                  <span>Latency: <strong className="text-amber-400">{selectedDay.avgExecutionLatencyMs}ms</strong></span>
                  <span>RAM: <strong className="text-emerald-400">{selectedDay.idleRamFootprintMb}MB</strong></span>
                  <span>Battery: <strong className="text-cyan-400">{selectedDay.batteryEfficiencyPct}%</strong></span>
                </div>
              </div>

              <div className="space-y-1.5">
                <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">
                  Autonomous Improvements Applied Overnight:
                </span>
                <div className="space-y-1">
                  {selectedDay.improvementsApplied.map((imp, idx) => (
                    <div key={idx} className="flex items-start gap-2 text-slate-300 text-[11px]">
                      <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                      <span>{imp}</span>
                    </div>
                  ))}
                </div>
              </div>

              {selectedDay.learnedFastPaths && selectedDay.learnedFastPaths.length > 0 && (
                <div className="pt-2 border-t border-slate-900/80 space-y-1">
                  <span className="text-[10px] text-slate-500 uppercase">Learned Fast-Paths:</span>
                  <div className="flex flex-wrap gap-1.5">
                    {selectedDay.learnedFastPaths.map((fp, i) => (
                      <span
                        key={i}
                        className="px-2 py-0.5 bg-black border border-slate-800 text-cyan-300 rounded text-[10px]"
                      >
                        {fp}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Self-Healed Skills & Resilience Registry */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-slate-400 font-bold uppercase flex items-center gap-2">
                <ShieldCheck className="w-3.5 h-3.5 text-cyan-400" />
                <span>SELF-HEALED SKILLS REGISTRY &bull; UI MUTATION ADAPTATION:</span>
              </span>
              <span className="text-slate-500 text-[10px]">
                {healedSkills.length} Auto-Healed Skills Active
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-900 text-[10px] text-slate-500 uppercase">
                    <th className="py-2 px-2.5">App / Target</th>
                    <th className="py-2 px-2.5">Observed Failure / UI Mutation</th>
                    <th className="py-2 px-2.5">Autonomous Healed Strategy</th>
                    <th className="py-2 px-2.5">Latency Delta</th>
                    <th className="py-2 px-2.5">Confidence</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-900/60 text-[11px]">
                  {healedSkills.map(skill => (
                    <tr key={skill.id} className="hover:bg-slate-950/60 transition-colors">
                      <td className="py-2.5 px-2.5">
                        <div className="font-bold text-white">{skill.appLabel}</div>
                        <div className="text-[9px] text-slate-500 truncate max-w-[140px]">
                          {skill.targetPackage}
                        </div>
                      </td>
                      <td className="py-2.5 px-2.5 text-rose-300/90 text-[10px] max-w-[220px]">
                        {skill.originalFailureCause}
                      </td>
                      <td className="py-2.5 px-2.5 text-emerald-300 text-[10px] max-w-[240px]">
                        {skill.healedStrategy}
                      </td>
                      <td className="py-2.5 px-2.5 whitespace-nowrap">
                        <span className="text-rose-400 line-through text-[10px]">{skill.latencyBeforeMs}ms</span>
                        <span className="text-slate-500 mx-1">&rarr;</span>
                        <span className="text-emerald-400 font-bold">{skill.latencyAfterMs}ms</span>
                      </td>
                      <td className="py-2.5 px-2.5 whitespace-nowrap">
                        <span className="px-2 py-0.5 rounded bg-emerald-950 border border-emerald-800 text-emerald-300 font-bold text-[10px]">
                          {(skill.confidence * 100).toFixed(0)}% ({skill.version})
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 2: HELIO P23 GOVERNOR & HARDWARE AUTOTUNER
          ========================================================================= */}
      {activeCenterTab === 'governor' && (
        <div className="space-y-4 font-mono text-xs">
          {/* Top Status & Controls */}
          <div className="p-4 bg-black border border-cyan-950 rounded-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-900 pb-3">
              <div className="flex items-center gap-2">
                <Cpu className="w-4 h-4 text-cyan-400" />
                <span className="font-bold text-white uppercase">
                  [ MEDIATEK HELIO P23 (MT6763T) COREPILOT CLUSTER GOVERNOR ]
                </span>
              </div>

              {/* Memory Compaction Button */}
              <button
                onClick={handleProactiveCompaction}
                disabled={isCompactingMemory}
                className="px-3 py-1.5 rounded-lg bg-emerald-950 border border-emerald-700 text-emerald-300 hover:bg-emerald-900 text-xs font-bold flex items-center gap-1.5 transition-all shadow-xs disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isCompactingMemory ? 'animate-spin' : ''}`} />
                <span>{isCompactingMemory ? 'Compacting Heap...' : 'Run Proactive Memory Compactor'}</span>
              </button>
            </div>

            {/* Compaction Toast Banner */}
            {compactionToast && (
              <div className="p-2.5 rounded-lg bg-cyan-950/80 border border-cyan-600/70 text-cyan-300 text-xs flex items-center gap-2">
                <Sparkles className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                <span>{compactionToast}</span>
              </div>
            )}

            {/* Live Governor Profile Selectors */}
            <div className="space-y-2">
              <span className="text-[10px] text-slate-500 uppercase font-bold">Select Active Cluster Governor:</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2">
                {[
                  {
                    id: 'EFFICIENCY_DAEMON',
                    title: 'Efficiency Daemon',
                    cores: 'Cores 0-3 @ 0.8-1.5 GHz',
                    desc: 'Silent background observer (~180mW)',
                    badge: 'POWERSAVE'
                  },
                  {
                    id: 'DYNAMIC_BURST_DISPATCH',
                    title: 'Dynamic Burst Dispatch',
                    cores: 'Cores 0-3 + Cores 4-7 Escalation',
                    desc: 'Sub-50ms dispatch for active UI tasks',
                    badge: 'OPTIMAL'
                  },
                  {
                    id: 'THERMAL_EMERGENCY_COOL',
                    title: 'Thermal Guard Cool',
                    cores: 'Duty-cycled Cores 0-1',
                    desc: 'Cool-down back-off if SoC > 41.5°C',
                    badge: 'SAFE'
                  },
                  {
                    id: 'AC_CHARGING_HIGH_PERF',
                    title: 'AC Nocturnal Consolidation',
                    cores: 'Full 8 Cores Unlocked',
                    desc: 'High-throughput SQLite defrag during charging',
                    badge: 'NOCTURNAL'
                  }
                ].map(gov => {
                  const isSelected = governorMode === gov.id;
                  return (
                    <button
                      key={gov.id}
                      onClick={() => {
                        setGovernorMode(gov.id as GovernorMode);
                        if (onSyncVault) onSyncVault(`Governor switched to ${gov.id}`);
                      }}
                      className={`p-3 rounded-xl border text-left transition-all space-y-1 ${
                        isSelected
                          ? 'bg-cyan-950/90 text-cyan-300 border-cyan-500 shadow-sm'
                          : 'bg-slate-950 text-slate-400 hover:text-white border-slate-900'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[11px] font-bold">
                        <span>{gov.title}</span>
                        <span className={`text-[9px] px-1.5 py-0.2 rounded border ${
                          isSelected ? 'bg-cyan-900 border-cyan-600 text-cyan-200' : 'bg-black border-slate-800 text-slate-500'
                        }`}>
                          {gov.badge}
                        </span>
                      </div>
                      <div className="text-[10px] text-amber-300/80">{gov.cores}</div>
                      <p className="text-[10px] text-slate-400 font-sans">{gov.desc}</p>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Live Hardware Telemetry Gauges */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-900">
              {/* Gauge 1: Thermal Status */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-1.5">
                <div className="flex items-center justify-between text-slate-400 text-[11px]">
                  <span className="flex items-center gap-1.5">
                    <Flame className="w-3.5 h-3.5 text-rose-400" />
                    <span>SoC Temperature</span>
                  </span>
                  <span className="text-emerald-400 font-bold text-[10px]">
                    +{(42.0 - socTempDegC).toFixed(1)}°C Safe Margin
                  </span>
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-bold text-white">{socTempDegC}°C</span>
                  <span className="text-slate-500 text-[10px]">Trip: 42.0°C</span>
                </div>
                <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-emerald-500 via-amber-500 to-rose-500 rounded-full"
                    style={{ width: `${Math.min(100, (socTempDegC / 45.0) * 100)}%` }}
                  />
                </div>
              </div>

              {/* Gauge 2: Strict 48MB RAM Heap */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-1.5">
                <div className="flex items-center justify-between text-slate-400 text-[11px]">
                  <span className="flex items-center gap-1.5">
                    <Database className="w-3.5 h-3.5 text-emerald-400" />
                    <span>Heap Allocator</span>
                  </span>
                  <span className="text-emerald-400 font-bold text-[10px]">
                    +{(48.0 - currentHeapMb).toFixed(1)}MB Headroom
                  </span>
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-bold text-white">{currentHeapMb} MB</span>
                  <span className="text-slate-500 text-[10px]">Limit: 48.0 MB</span>
                </div>
                <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                  <div
                    className="h-full bg-emerald-400 rounded-full"
                    style={{ width: `${(currentHeapMb / 48.0) * 100}%` }}
                  />
                </div>
              </div>

              {/* Gauge 3: Dynamic Audio PCM Buffer */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-1.5">
                <div className="flex items-center justify-between text-slate-400 text-[11px]">
                  <span className="flex items-center gap-1.5">
                    <Music className="w-3.5 h-3.5 text-cyan-400" />
                    <span>PCM Audio Buffer</span>
                  </span>
                  <span className="text-cyan-400 font-bold text-[10px]">23.2ms Latency</span>
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-bold text-white">{pcmBufferSizeFrames} Frames</span>
                  <span className="text-slate-500 text-[10px]">44.1 kHz PCM</span>
                </div>
                <div className="text-[10px] text-slate-400 font-sans">
                  Zero memory leak with static native AudioTrack allocation
                </div>
              </div>
            </div>

            {/* Dynamic Autotuned Parameters Matrix */}
            <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-2">
              <span className="text-[10px] text-slate-400 uppercase font-bold">
                Evolved Autotuned Hardware Parameters (Day {currentDay}):
              </span>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                <div className="p-2 rounded bg-black border border-slate-900">
                  <span className="text-slate-500 text-[10px]">UI Action Gap:</span>
                  <div className="text-amber-400 font-bold">{uiActionIntervalMs}ms</div>
                </div>
                <div className="p-2 rounded bg-black border border-slate-900">
                  <span className="text-slate-500 text-[10px]">Flash Cool-down:</span>
                  <div className="text-cyan-400 font-bold">{flashCoolDownMs}ms</div>
                </div>
                <div className="p-2 rounded bg-black border border-slate-900">
                  <span className="text-slate-500 text-[10px]">Ambient Dark Lux:</span>
                  <div className="text-emerald-400 font-bold">&lt; 20 lx</div>
                </div>
                <div className="p-2 rounded bg-black border border-slate-900">
                  <span className="text-slate-500 text-[10px]">GC Micro-Pause:</span>
                  <div className="text-pink-400 font-bold">0.0ms (0 Alloc)</div>
                </div>
              </div>
            </div>
          </div>

          {/* Bayesian Heuristics Table */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-slate-400 font-bold uppercase flex items-center gap-2">
                <Activity className="w-3.5 h-3.5 text-cyan-400" />
                <span>BAYESIAN HEURISTIC WEIGHTS &bull; CONTINUAL EVIDENCE ACCUMULATION:</span>
              </span>
              <span className="text-emerald-400 text-[10px] font-bold">Beta Distribution Posterior Active</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-900 text-[10px] text-slate-500 uppercase">
                    <th className="py-2 px-2.5">Heuristic</th>
                    <th className="py-2 px-2.5">Function</th>
                    <th className="py-2 px-2.5">Success / Fail</th>
                    <th className="py-2 px-2.5">Latency</th>
                    <th className="py-2 px-2.5">Confidence</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-900/60 text-[11px]">
                  {bayesianHeuristics.map(heur => (
                    <tr key={heur.id} className="hover:bg-slate-950/60 transition-colors">
                      <td className="py-2.5 px-2.5 font-bold text-white">
                        {heur.name}
                      </td>
                      <td className="py-2.5 px-2.5 text-slate-400 text-[10px] max-w-[280px]">
                        {heur.description}
                      </td>
                      <td className="py-2.5 px-2.5 text-slate-300">
                        <span className="text-emerald-400 font-bold">{heur.successCount}</span>
                        <span className="text-slate-600 mx-1">/</span>
                        <span className="text-rose-400">{heur.failureCount}</span>
                      </td>
                      <td className="py-2.5 px-2.5 text-amber-400 font-bold">
                        {heur.observedLatencyMs}ms
                      </td>
                      <td className="py-2.5 px-2.5">
                        <span className="px-2 py-0.5 rounded bg-cyan-950 border border-cyan-800 text-cyan-300 font-bold text-[10px]">
                          {(heur.confidence * 100).toFixed(1)}% ({heur.tier})
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 3: PARETO MULTI-OBJECTIVE FRONTIER
          ========================================================================= */}
      {activeCenterTab === 'pareto' && (
        <div className="space-y-4 font-mono text-xs">
          <div className="p-4 bg-black border border-pink-950 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-3">
              <div className="flex items-center gap-2">
                <Gauge className="w-4 h-4 text-pink-400" />
                <span className="font-bold text-white uppercase">
                  [ PARETO MULTI-OBJECTIVE FITNESS FRONTIER &bull; DAY-BY-DAY GAINS ]
                </span>
              </div>
              <span className="text-[10px] text-pink-400 bg-pink-950 px-2 py-0.5 rounded border border-pink-800">
                Non-Dominated Solutions Only
              </span>
            </div>

            <p className="text-slate-400 font-sans text-xs">
              Every day the agent evolves its actuator choreographies and task dispatch paths to dominate previous generations along all five objective axes:
            </p>

            {/* Pareto Comparison Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-900 text-[10px] text-slate-500 uppercase">
                    <th className="py-2 px-2.5">Generation</th>
                    <th className="py-2 px-2.5">Energy (Joules)</th>
                    <th className="py-2 px-2.5">Task Latency</th>
                    <th className="py-2 px-2.5">Thermal Rise</th>
                    <th className="py-2 px-2.5">Harmonic Concordance</th>
                    <th className="py-2 px-2.5">Sync Jitter</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-900/60 text-[11px]">
                  {history.map(day => (
                    <tr key={day.dayNumber} className="hover:bg-slate-950/60 transition-colors">
                      <td className="py-2.5 px-2.5 font-bold text-white">
                        Day {day.dayNumber}
                      </td>
                      <td className="py-2.5 px-2.5">
                        <span className="text-emerald-400 font-bold">{day.paretoEnergyJoules} J</span>
                        <span className="text-[10px] text-slate-500 ml-1">
                          (-{(((1.45 - day.paretoEnergyJoules) / 1.45) * 100).toFixed(0)}%)
                        </span>
                      </td>
                      <td className="py-2.5 px-2.5 text-amber-400 font-bold">
                        {day.avgExecutionLatencyMs}ms
                      </td>
                      <td className="py-2.5 px-2.5 text-cyan-400">
                        +{day.thermalRiseDegC}°C
                      </td>
                      <td className="py-2.5 px-2.5 text-pink-400 font-bold">
                        {day.harmonicScorePct}%
                      </td>
                      <td className="py-2.5 px-2.5 text-emerald-300">
                        &lt; {(3.2 * (6 - Math.min(day.dayNumber, 5))).toFixed(1)}ms
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Multi-Objective Visual Comparison Summary */}
            <div className="p-3.5 bg-slate-950 border border-slate-900 rounded-lg space-y-2 pt-3">
              <div className="flex items-center justify-between text-[11px]">
                <span className="text-white font-bold">Day 1 Baseline vs Current Day {currentDay} Optimization:</span>
                <span className="text-emerald-400 font-bold">+71.0% Overall Energy Efficiency</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[10px] text-slate-400 font-sans">
                <div className="flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>Energy per Choreography: 1.45J down to <strong>{latestDay.paretoEnergyJoules}J</strong></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>Audio/Light Jitter: 12.5ms down to <strong>&lt; 2.1ms</strong> lockstep sync</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>Thermal SoC Rise: +2.4°C down to <strong>+{latestDay.thermalRiseDegC}°C</strong></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>Harmonic Resonance: 65% up to <strong>{latestDay.harmonicScorePct}%</strong></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
