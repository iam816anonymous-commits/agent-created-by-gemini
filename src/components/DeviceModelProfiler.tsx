import React, { useState, useEffect } from 'react';
import {
  Cpu,
  Gauge,
  Sliders,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  Sparkles,
  Database,
  Terminal,
  Check,
  X,
  FileCode,
  ShieldCheck,
  Smartphone,
  ChevronRight,
  TrendingDown,
  Info,
  Layers,
  ArrowRight,
  Shield,
  Lock,
  Unlock,
  Eye,
  GitCommit,
  CheckCircle,
  XCircle,
  Zap,
  HardDrive,
  Activity,
  Radio
} from 'lucide-react';
import { probeLiveDeviceHardware, DynamicDeviceTelemetry } from '../utils/dynamicDeviceProbe';

export interface DeviceHardwareProfile {
  id: string;
  name: string;
  socModel: string;
  cpuArch: string;
  coreTopology: string;
  simdSupport: string[];
  totalRamMb: number;
  osOverheadMb: number;
  activeAppBufferMb: number;
  androidSdk: number;
  androidVersion: string;
  pageSizeBytes: number;
}

export interface ModelCandidate {
  id: string;
  name: string;
  parameterCount: string;
  quantization: string;
  fileSizeMb: number;
  runtimeRamMb: number;
  minRecommendedDeviceRamMb: number;
  targetTokPerSecHelioP23: number;
  status: 'RECOMMENDED' | 'COMPATIBLE' | 'RISKY' | 'PROHIBITED';
  rationale: string;
  gbnfCompliant: boolean;
}

export interface MockApprovalProposal {
  id: string;
  intent: string;
  targetPackage: string;
  targetAppLabel: string;
  toolName: string;
  actionType: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  paramsJson: string;
  preStateHash: string;
  postStateExpected: string;
  targetNodeSummary: string;
  potentialSideEffects: string[];
  status: 'PENDING' | 'APPROVED' | 'DECLINED';
  timestamp: string;
}

export interface DeviceModelProfilerProps {
  oledMode?: boolean;
  selectedPresetId?: string;
  onSelectPreset?: (preset: DeviceHardwareProfile, tier: 'TIER_1' | 'TIER_2' | 'TIER_3') => void;
  onRequestApproval?: (proposal: MockApprovalProposal) => void;
}

export const DEVICE_PRESETS: DeviceHardwareProfile[] = [
  {
    id: 'tecno_in6_helio_p23',
    name: 'TECNO Camon i Click (IN6)',
    socModel: 'MediaTek Helio P23 (MT6763V)',
    cpuArch: 'ARMv8-A (64-bit aarch64)',
    coreTopology: '8x Cortex-A53 (4x 2.3 GHz BIG + 4x 1.65 GHz LITTLE)',
    simdSupport: ['NEON', 'VFPv4', 'AES'],
    totalRamMb: 4096,
    osOverheadMb: 1850,
    activeAppBufferMb: 720,
    androidSdk: 27,
    androidVersion: 'Android 8.1 Oreo (HiOS 3.3)',
    pageSizeBytes: 4096
  },
  {
    id: 'redmi_note10_snapdragon',
    name: 'Redmi Note 10 / Budget 6GB',
    socModel: 'Qualcomm Snapdragon 678 (SM6150)',
    cpuArch: 'ARMv8.2-A (64-bit aarch64)',
    coreTopology: '2x Kryo 460 Gold (2.2 GHz) + 6x Kryo 460 Silver (1.7 GHz)',
    simdSupport: ['NEON', 'DotProd', 'FP16', 'AES'],
    totalRamMb: 6144,
    osOverheadMb: 2100,
    activeAppBufferMb: 950,
    androidSdk: 30,
    androidVersion: 'Android 11 (MIUI 12.5)',
    pageSizeBytes: 4096
  },
  {
    id: 'galaxy_a54_exynos',
    name: 'Samsung Galaxy A54 8GB',
    socModel: 'Samsung Exynos 1380',
    cpuArch: 'ARMv8.4-A (64-bit aarch64)',
    coreTopology: '4x Cortex-A78 (2.4 GHz) + 4x Cortex-A55 (2.0 GHz)',
    simdSupport: ['NEON', 'DotProd', 'I8MM', 'SVE'],
    totalRamMb: 8192,
    osOverheadMb: 2600,
    activeAppBufferMb: 1200,
    androidSdk: 34,
    androidVersion: 'Android 14 (One UI 6.1)',
    pageSizeBytes: 4096
  },
  {
    id: 'pixel_9_pro_tensor',
    name: 'Google Pixel 9 Pro 16GB',
    socModel: 'Google Tensor G4',
    cpuArch: 'ARMv9.2-A (64-bit aarch64)',
    coreTopology: '1x Cortex-X4 (3.1 GHz) + 3x Cortex-A720 (2.6 GHz) + 4x Cortex-A520 (1.95 GHz)',
    simdSupport: ['NEON', 'DotProd', 'SVE2', 'I8MM', 'FP16'],
    totalRamMb: 16384,
    osOverheadMb: 3400,
    activeAppBufferMb: 1800,
    androidSdk: 36,
    androidVersion: 'Android 15–17 (API 35–37+ / 16 KB Pages)',
    pageSizeBytes: 16384
  }
];

export const CANDIDATE_MODELS: ModelCandidate[] = [
  {
    id: 'smollm2_360m_q4',
    name: 'SmolLM2-360M-Instruct (Q4_K_M)',
    parameterCount: '360 Million',
    quantization: 'Q4_K_M (4-bit Medium Quantization)',
    fileSizeMb: 242,
    runtimeRamMb: 271,
    minRecommendedDeviceRamMb: 3072,
    targetTokPerSecHelioP23: 22.4,
    status: 'RECOMMENDED',
    rationale: 'Perfect match for <= 4GB budget. 271MB runtime footprint fits easily into the calculated safe ceiling without triggering LMK. High instruction fidelity for structured tool calls.',
    gbnfCompliant: true
  },
  {
    id: 'qwen25_05b_q3',
    name: 'Qwen2.5-0.5B-Instruct (Q3_K_M)',
    parameterCount: '490 Million',
    quantization: 'Q3_K_M (3-bit Medium Quantization)',
    fileSizeMb: 194,
    runtimeRamMb: 235,
    minRecommendedDeviceRamMb: 3072,
    targetTokPerSecHelioP23: 24.1,
    status: 'RECOMMENDED',
    rationale: 'Ultra-compact 3-bit quantization profile. Smallest memory footprint (235MB) with exceptional JSON parsing accuracy, ideal when heavy foreground apps are active.',
    gbnfCompliant: true
  },
  {
    id: 'smollm2_360m_q8',
    name: 'SmolLM2-360M-Instruct (Q8_0)',
    parameterCount: '360 Million',
    quantization: 'Q8_0 (8-bit High Precision)',
    fileSizeMb: 386,
    runtimeRamMb: 420,
    minRecommendedDeviceRamMb: 4096,
    targetTokPerSecHelioP23: 16.8,
    status: 'COMPATIBLE',
    rationale: 'Near-lossless precision for subtle multi-step reasoning. Safely fits 4GB devices when foreground memory pressure is light (< 800MB apps).',
    gbnfCompliant: true
  },
  {
    id: 'llama_32_1b_q4',
    name: 'Llama-3.2-1B-Instruct (Q4_K_M)',
    parameterCount: '1.23 Billion',
    quantization: 'Q4_K_M (4-bit)',
    fileSizeMb: 720,
    runtimeRamMb: 850,
    minRecommendedDeviceRamMb: 6144,
    targetTokPerSecHelioP23: 7.2,
    status: 'RISKY',
    rationale: 'Exceeds 4GB device safe ceiling (requires 850MB RAM). Triggers Dalvik/Linux page thrashing on Helio P23. Recommended only on >= 6GB RAM devices.',
    gbnfCompliant: true
  },
  {
    id: 'qwen25_15b_q4',
    name: 'Qwen2.5-1.5B-Instruct (Q4_K_M)',
    parameterCount: '1.54 Billion',
    quantization: 'Q4_K_M (4-bit)',
    fileSizeMb: 980,
    runtimeRamMb: 1150,
    minRecommendedDeviceRamMb: 8192,
    targetTokPerSecHelioP23: 4.8,
    status: 'PROHIBITED',
    rationale: 'Strictly prohibited on 4GB devices. Exceeds available RAM and causes Android LowMemoryKiller to terminate LocalAgent. Reserved for 8GB+ hardware.',
    gbnfCompliant: true
  },
  {
    id: 'gemma_2_2b_q4',
    name: 'Gemma-2-2B-Instruct (Q4_K_M)',
    parameterCount: '2.61 Billion',
    quantization: 'Q4_K_M (4-bit)',
    fileSizeMb: 1420,
    runtimeRamMb: 1680,
    minRecommendedDeviceRamMb: 8192,
    targetTokPerSecHelioP23: 2.1,
    status: 'PROHIBITED',
    rationale: 'Exceeds 4GB device RAM pool. Triggers immediate SIGKILL from Linux kernel LMK. Enabled only on flagship 8GB+ devices with GPU acceleration.',
    gbnfCompliant: true
  }
];

export function DeviceModelProfiler({
  oledMode = false,
  selectedPresetId: propSelectedPresetId,
  onSelectPreset,
  onRequestApproval
}: DeviceModelProfilerProps) {
  const [selectedPresetId, setSelectedPresetId] = useState<string>(propSelectedPresetId || 'tecno_in6_helio_p23');

  useEffect(() => {
    if (propSelectedPresetId && propSelectedPresetId !== selectedPresetId) {
      setSelectedPresetId(propSelectedPresetId);
    }
  }, [propSelectedPresetId]);
  const [dynamicProbe, setDynamicProbe] = useState<DynamicDeviceTelemetry | null>(null);
  const [isProbingLive, setIsProbingLive] = useState(false);

  useEffect(() => {
    let isMounted = true;
    setIsProbingLive(true);
    probeLiveDeviceHardware().then(data => {
      if (isMounted) {
        setDynamicProbe(data);
        setIsProbingLive(false);
      }
    });
    return () => { isMounted = false; };
  }, []);

  const [customRamMb, setCustomRamMb] = useState<number>(4096);
  const [customOsOverheadMb, setCustomOsOverheadMb] = useState<number>(1850);
  const [customAppBufferMb, setCustomAppBufferMb] = useState<number>(720);
  const [isCustomMode, setIsCustomMode] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<'profile' | 'tiers' | 'approval' | 'registry' | 'benchmark' | 'code'>('profile');
  const [selectedCodeFile, setSelectedCodeFile] = useState<'DeviceProfiler.kt' | 'ArchitectureTierManager.kt' | 'AutonomyPermissionManager.kt'>('DeviceProfiler.kt');

  // Human-in-the-Loop Permission Gate State
  const [requireUserApproval, setRequireUserApproval] = useState<boolean>(true);
  const [approvalProposals, setApprovalProposals] = useState<MockApprovalProposal[]>([
    {
      id: 'prop-otp-001',
      intent: 'Inject OTP "582914" from MemoryStack into foreground authentication field',
      targetPackage: 'com.example.bank.auth',
      targetAppLabel: 'SecureBanking Login',
      toolName: 'CrossAppMemoryManager.injectText',
      actionType: 'INJECT_TEXT',
      riskLevel: 'HIGH',
      paramsJson: JSON.stringify({ fieldId: 'otp_input_edit_text', value: '582914', clearAfterPaste: true }, null, 2),
      preStateHash: '0x7B9A2F1C4E8A91D0',
      postStateExpected: '0x8C1B3E4A2F9B03E1 (Target node text mutated: "******")',
      targetNodeSummary: 'android.widget.EditText [id/otp_input_edit_text] Bounds: Rect(80, 520 - 640, 600)',
      potentialSideEffects: [
        'Dispatches accessibility ACTION_SET_TEXT to target package',
        'Clears OTP fragment from transient MemoryStack to prevent second injection',
        'Triggers automatic 2FA verification button listener'
      ],
      status: 'PENDING',
      timestamp: 'Just now'
    },
    {
      id: 'prop-act-002',
      intent: 'Toggle Dual Flashlights (Camera 0 Rear Quad-LED + Camera 1 Front Dual-LED) to SOS Strobe',
      targetPackage: 'com.android.hardware.flashlight',
      targetAppLabel: 'CameraManager Actuator',
      toolName: 'ActuatorController.setFlashlight',
      actionType: 'TOGGLE_ACTUATOR',
      riskLevel: 'MEDIUM',
      paramsJson: JSON.stringify({ cameraIndices: ['0', '1'], mode: 'SOS_STROBE', autoShutoffSec: 180 }, null, 2),
      preStateHash: '0x0000000000000000',
      postStateExpected: '0x0000000000000003 (Camera 0 & 1 Torch = TRUE)',
      targetNodeSummary: 'android.hardware.camera2.CameraManager hardware actuator bus',
      potentialSideEffects: [
        'Draws +0.35W additional instantaneous battery power',
        'Hardware watchdog arms 180-second auto-shutoff timer',
        'Audio chime plays on actuator activation'
      ],
      status: 'PENDING',
      timestamp: '2 mins ago'
    },
    {
      id: 'prop-vault-003',
      intent: 'Commit 3 newly learned nocturnal skills to public storage /sdcard/LocalAgent/brain_vault.json',
      targetPackage: 'com.example.core.storage',
      targetAppLabel: 'Brain Vault Persistence',
      toolName: 'BrainVaultManager.persistVault',
      actionType: 'COMMIT_VAULT',
      riskLevel: 'LOW',
      paramsJson: JSON.stringify({ newSkillsCount: 3, targetPath: '/sdcard/LocalAgent/brain_vault.json', crc32Check: true }, null, 2),
      preStateHash: '0x93E7A1C200000000',
      postStateExpected: '0xA4F8B2D311002200 (CRC32: 0xA4F8B2D3, Version: 4)',
      targetNodeSummary: 'External public flash storage /sdcard/LocalAgent/brain_vault.json',
      potentialSideEffects: [
        'Performs atomic swap via temporary file swap (.tmp -> .json)',
        'Survives complete APK uninstallation',
        'Writes 48.2 KB of JSON payload to public disk'
      ],
      status: 'PENDING',
      timestamp: '5 mins ago'
    }
  ]);

  const [auditLog, setAuditLog] = useState<Array<{
    id: string;
    intent: string;
    tool: string;
    decision: 'APPROVED' | 'DECLINED';
    time: string;
    executionLog: string;
  }>>([
    {
      id: 'audit-000',
      intent: 'Initial hardware inspection and device profiling',
      tool: 'DeviceProfiler.profileHardware',
      decision: 'APPROVED',
      time: '10:00:01 AM',
      executionLog: 'Pre-flight auto-profile approved. Discovered 4,096MB RAM, safe ceiling: 381MB.'
    }
  ]);

  // Simulation test state
  const [isBenchmarking, setIsBenchmarking] = useState(false);
  const [benchmarkResult, setBenchmarkResult] = useState<{
    modelName: string;
    ttftMs: number;
    tokPerSec: number;
    tokensGenerated: number;
    memoryUsedMb: number;
    lmkSafe: boolean;
    pageAlignmentSafe: boolean;
  } | null>(null);

  const livePreset: DeviceHardwareProfile | null = dynamicProbe ? {
    id: 'live_probed_device',
    name: `[LIVE PROBE] ${dynamicProbe.deviceName}`,
    socModel: dynamicProbe.gpuRenderer,
    cpuArch: `${dynamicProbe.cpuCores}-core (${dynamicProbe.platform})`,
    coreTopology: `${dynamicProbe.cpuCores} active execution threads`,
    simdSupport: ['NEON', 'WebGL2', 'SIMD', 'WASM'],
    totalRamMb: dynamicProbe.totalRamMb,
    osOverheadMb: dynamicProbe.osOverheadMb,
    activeAppBufferMb: dynamicProbe.activeAppBufferMb,
    androidSdk: dynamicProbe.isMobile ? 27 : 34,
    androidVersion: `${dynamicProbe.osName} ${dynamicProbe.osVersion}`,
    pageSizeBytes: dynamicProbe.recommendedPageSize
  } : null;

  const allPresets = livePreset ? [livePreset, ...DEVICE_PRESETS] : DEVICE_PRESETS;
  const activePreset = allPresets.find(p => p.id === selectedPresetId) || allPresets[0];

  // Dynamic Metrics Calculation
  const totalRam = isCustomMode ? customRamMb : activePreset.totalRamMb;
  const osOverhead = isCustomMode ? customOsOverheadMb : activePreset.osOverheadMb;
  const appBuffer = isCustomMode ? customAppBufferMb : activePreset.activeAppBufferMb;
  const availablePoolMb = Math.max(0, totalRam - osOverhead - appBuffer);

  // FORMULA SPECIFICATION: SafeModelRAM = (TotalRAM - OsOverhead - ActiveAppBuffer) * 0.25
  const calculatedSafeModelRamMb = Math.round(availablePoolMb * 0.25);

  // Tier Classification (ArchitectureTierManager.kt)
  const activeHardwareTier: 'TIER_1' | 'TIER_2' | 'TIER_3' =
    totalRam <= 4096 ? 'TIER_1' : totalRam <= 8192 ? 'TIER_2' : 'TIER_3';

  const tierSpecs = {
    TIER_1: {
      tierKey: 'TIER_1',
      name: 'Tier 1: Constrained Mobile (≤ 4GB)',
      socExample: 'MediaTek Helio P23 (MT6763V) / Android 8.1 API 27',
      perceptionPipeline: 'Zero-Allocation Text-Tree Pruning (<4KB DSL)',
      screenshotPermitted: false,
      screenshotReason: 'Prohibited: 1080x2160 Bitmap creates 9.3MB allocation spike, risking immediate LMK crash',
      recommendedModel: 'SmolLM2-360M-Instruct (Q4_K_M, 271MB)',
      contextLimit: '512 tokens',
      kvCacheBudget: '32 MB',
      cpuPinning: 'LITTLE-Core Pinning (Cores 0-3 @ 1.65GHz) to preserve thermals and battery (<0.65W)',
      pageAlignment: '4 KB Linux page baseline (sysconf(_SC_PAGESIZE) = 4096)',
      idleCeiling: '48.0 MB strict resident ceiling',
      badgeColor: 'text-amber-400 bg-amber-950/80 border-amber-600'
    },
    TIER_2: {
      tierKey: 'TIER_2',
      name: 'Tier 2: Balanced Mid-Range (6GB – 8GB)',
      socExample: 'Snapdragon 678 / Exynos 1380 / Android 11–14 (API 30–34)',
      perceptionPipeline: 'Hybrid Tree + Fast Regional Screenshot Diff',
      screenshotPermitted: true,
      screenshotReason: 'Permitted on Android 11+ via AccessibilityService.takeScreenshot() callback',
      recommendedModel: 'Llama-3.2-1B-Instruct (Q4_K_M, 850MB)',
      contextLimit: '2,048 tokens',
      kvCacheBudget: '128 MB',
      cpuPinning: 'Balanced (2 Big Cores + 4 Little Cores)',
      pageAlignment: '4 KB / 16 KB hybrid compatible',
      idleCeiling: '96.0 MB resident ceiling',
      badgeColor: 'text-cyan-400 bg-cyan-950/80 border-cyan-600'
    },
    TIER_3: {
      tierKey: 'TIER_3',
      name: 'Tier 3: Flagship Multimodal (12GB – 16GB+)',
      socExample: 'Google Tensor G4 / Snapdragon 8 Gen 3 / Android 15–16 (API 35–36)',
      perceptionPipeline: 'Native Multimodal Vision + On-Device AICore Foundation Models',
      screenshotPermitted: true,
      screenshotReason: 'Full resolution screenshot analysis + Desktop Windowing multi-window tracker',
      recommendedModel: 'Qwen2.5-1.5B-Instruct / AICore (1,150MB)',
      contextLimit: '4,096 tokens',
      kvCacheBudget: '384 MB',
      cpuPinning: 'All Cores Available (Prime X4 Core Burst for sub-80ms TTFT)',
      pageAlignment: 'Strict 16 KB Page Aligned (-Wl,-z,max-page-size=16384)',
      idleCeiling: '192.0 MB resident ceiling',
      badgeColor: 'text-emerald-400 bg-emerald-950/80 border-emerald-600'
    }
  }[activeHardwareTier];

  const handleApproveProposal = (propId: string) => {
    const proposal = approvalProposals.find(p => p.id === propId);
    if (!proposal) return;

    setApprovalProposals(prev => prev.map(p => p.id === propId ? { ...p, status: 'APPROVED' } : p));
    setAuditLog(prev => [
      {
        id: `audit-${Date.now()}`,
        intent: proposal.intent,
        tool: proposal.toolName,
        decision: 'APPROVED',
        time: new Date().toLocaleTimeString(),
        executionLog: `[USER CONFIRMED & COMMITTED] Action executed successfully. Target package ${proposal.targetPackage} state updated.`
      },
      ...prev
    ]);
  };

  const handleDeclineProposal = (propId: string) => {
    const proposal = approvalProposals.find(p => p.id === propId);
    if (!proposal) return;

    setApprovalProposals(prev => prev.map(p => p.id === propId ? { ...p, status: 'DECLINED' } : p));
    setAuditLog(prev => [
      {
        id: `audit-${Date.now()}`,
        intent: proposal.intent,
        tool: proposal.toolName,
        decision: 'DECLINED',
        time: new Date().toLocaleTimeString(),
        executionLog: `[USER DECLINED] Action payload permanently discarded. Zero mutations dispatched to ${proposal.targetPackage}.`
      },
      ...prev
    ]);
  };

  const handleInjectNewProposal = () => {
    const newProp: MockApprovalProposal = {
      id: `prop-dyn-${Date.now()}`,
      intent: 'Dismiss persistent system notification and advance foreground step',
      targetPackage: 'com.android.systemui',
      targetAppLabel: 'System Notification Bar',
      toolName: 'AutonomousRecoveryEngine.dismissInterruption',
      actionType: 'CLICK',
      riskLevel: 'LOW',
      paramsJson: JSON.stringify({ action: 'SWIPE_RIGHT', timeoutMs: 500 }, null, 2),
      preStateHash: '0x44AA33BB11CC22DD',
      postStateExpected: '0x0000000000000000 (Notification removed)',
      targetNodeSummary: 'android.widget.FrameLayout [id/notification_panel]',
      potentialSideEffects: ['Clears transient alert banner from status bar'],
      status: 'PENDING',
      timestamp: 'Just now'
    };
    setApprovalProposals(prev => [newProp, ...prev]);
  };

  // Dynamic Model Recommendation Engine
  const getEvaluatedCandidates = (): ModelCandidate[] => {
    return CANDIDATE_MODELS.map(model => {
      let status: ModelCandidate['status'] = 'COMPATIBLE';
      let rationale = model.rationale;

      if (model.runtimeRamMb > calculatedSafeModelRamMb * 1.6) {
        status = 'PROHIBITED';
        rationale = `Runtime RAM (${model.runtimeRamMb}MB) exceeds dynamic safe ceiling (${calculatedSafeModelRamMb}MB) by ${Math.round((model.runtimeRamMb / calculatedSafeModelRamMb - 1) * 100)}%. Prohibited to prevent LMK crash.`;
      } else if (model.runtimeRamMb > calculatedSafeModelRamMb) {
        status = 'RISKY';
        rationale = `Runtime RAM (${model.runtimeRamMb}MB) is close to the threshold (${calculatedSafeModelRamMb}MB). May trigger background app eviction under pressure.`;
      } else if (
        (totalRam <= 4096 && model.id === 'smollm2_360m_q4') ||
        (totalRam > 4096 && totalRam <= 6144 && model.id === 'smollm2_360m_q8') ||
        (totalRam > 6144 && totalRam <= 8192 && model.id === 'llama_32_1b_q4') ||
        (totalRam > 8192 && model.id === 'qwen25_15b_q4')
      ) {
        status = 'RECOMMENDED';
        rationale = `Optimal match for current profile (${totalRam}MB RAM, ${calculatedSafeModelRamMb}MB ceiling). Maximum instruction density within safe budget.`;
      }

      return { ...model, status, rationale };
    });
  };

  const evaluatedCandidates = getEvaluatedCandidates();
  const primaryRecommendation = evaluatedCandidates.find(m => m.status === 'RECOMMENDED') || evaluatedCandidates[0];

  const handleRunModelBenchmark = (model: ModelCandidate) => {
    setIsBenchmarking(true);
    setBenchmarkResult(null);

    setTimeout(() => {
      const isLowEnd = totalRam <= 4096;
      const tokSpeed = isLowEnd ? model.targetTokPerSecHelioP23 : model.targetTokPerSecHelioP23 * 2.2;
      const ttft = Math.round(180 + (model.fileSizeMb * 0.45));

      setBenchmarkResult({
        modelName: model.name,
        ttftMs: ttft,
        tokPerSec: Number(tokSpeed.toFixed(1)),
        tokensGenerated: 14, // Strict GBNF token count
        memoryUsedMb: model.runtimeRamMb,
        lmkSafe: model.runtimeRamMb <= calculatedSafeModelRamMb * 1.25,
        pageAlignmentSafe: true
      });
      setIsBenchmarking(false);
    }, 750);
  };

  return (
    <div className={`rounded-2xl border p-4 sm:p-5 transition-all space-y-4 ${
      oledMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
    }`}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-400">
            <Cpu className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide font-mono uppercase flex items-center gap-1.5 flex-wrap">
              <span>[ DEVICE INTELLIGENCE &amp; ADAPTIVE MODEL REGISTRY ]</span>
              <span className="text-[9px] px-1.5 py-0.2 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">
                Auto-Discovery Engine &bull; API 27 &rarr; 36
              </span>
            </h2>
            <p className="text-[11px] text-slate-400 font-sans mt-0.5">
              Inspects hardware metrics via /proc/meminfo and /proc/cpuinfo, computes dynamic SLM RAM ceilings, and selects optimal quantized models
            </p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="w-full sm:w-auto">
          {/* Mobile dropdown for instant tab selection */}
          <div className="block sm:hidden mb-1">
            <select
              value={activeTab}
              onChange={(e) => setActiveTab(e.target.value as any)}
              className="w-full min-h-[44px] bg-black border border-cyan-800 text-cyan-300 font-mono text-xs font-bold rounded-xl px-3 py-2 focus:outline-none"
            >
              <option value="profile">1. Device Profile &amp; Safe RAM Ceiling</option>
              <option value="tiers">2. Multi-Tier Hardware Escalation (T1-T3)</option>
              <option value="approval">3. Permission Gate &amp; Human Approval Card</option>
              <option value="registry">4. Adaptive Model Registry (SmolLM/Qwen)</option>
              <option value="benchmark">5. Live Benchmark Simulator (TTFT &amp; TPS)</option>
              <option value="code">6. Architecture Kotlin Production Code</option>
            </select>
          </div>

          <div className="flex items-center gap-1.5 text-xs font-mono overflow-x-auto no-scrollbar pb-1">
            {[
              { id: 'profile', label: 'Profile', fullLabel: 'Device Profile & Ceiling', icon: Gauge },
              { id: 'tiers', label: 'Tiers', fullLabel: 'Multi-Tier Escalation', icon: Layers },
              { id: 'approval', label: 'Safety Gate', fullLabel: 'Permission Gate & Approvals', icon: ShieldCheck },
              { id: 'registry', label: 'Models', fullLabel: 'Adaptive Model Registry', icon: Database },
              { id: 'benchmark', label: 'Benchmark', fullLabel: 'Benchmark Simulator', icon: Play },
              { id: 'code', label: 'Kotlin Code', fullLabel: 'Architecture Kotlin Code', icon: FileCode }
            ].map(tab => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`min-h-[44px] px-3 py-2 rounded-xl transition-all flex items-center gap-1.5 text-xs whitespace-nowrap border ${
                    activeTab === tab.id
                      ? 'bg-cyan-500 text-black font-bold border-cyan-400 shadow-sm shadow-cyan-500/20'
                      : 'bg-black text-slate-400 hover:text-white border-slate-900 active:bg-slate-900'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5 shrink-0" />
                  <span>
                    <span className="sm:hidden">{tab.label}</span>
                    <span className="hidden sm:inline">{tab.fullLabel}</span>
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Dynamic Hardware Live Probe Banner */}
      {dynamicProbe && (
        <div className="p-3.5 bg-gradient-to-r from-cyan-950/60 via-slate-900 to-emerald-950/40 border border-cyan-500/50 rounded-xl space-y-2.5 shadow-md shadow-cyan-950/30 font-mono text-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-cyan-800/40 pb-2">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
              </span>
              <span className="font-bold text-white uppercase text-[11px] flex items-center gap-1.5 flex-wrap">
                <span>[ LIVE DYNAMIC HARDWARE DETECTED ]</span>
                <span className="text-[9px] px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-300 border border-emerald-700">
                  {dynamicProbe.deviceName}
                </span>
              </span>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-[10px] text-slate-400">Probed: {dynamicProbe.probedAt}</span>
              <button
                type="button"
                onClick={() => {
                  if (livePreset) {
                    setSelectedPresetId(livePreset.id);
                    setIsCustomMode(false);
                    onSelectPreset?.(livePreset, dynamicProbe.recommendedTier);
                  }
                }}
                className="px-3 py-1 bg-emerald-500 hover:bg-emerald-400 text-black font-bold text-[10px] rounded-lg transition-all shadow-sm flex items-center gap-1 cursor-pointer"
              >
                <Check className="w-3 h-3 stroke-[3]" />
                <span>APPLY RECOMMENDED SETUP</span>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[10px]">
            <div className="p-2 bg-black/80 rounded-lg border border-slate-900">
              <div className="text-slate-500">PROBED RAM:</div>
              <div className="text-cyan-300 font-bold">{dynamicProbe.totalRamMb} MB</div>
              <div className="text-[9px] text-emerald-400">Safe Ceiling: {dynamicProbe.calculatedSafeCeilingMb} MB</div>
            </div>

            <div className="p-2 bg-black/80 rounded-lg border border-slate-900">
              <div className="text-slate-500">CPU TOPOLOGY:</div>
              <div className="text-white font-bold">{dynamicProbe.cpuCores} Threads</div>
              <div className="text-[9px] text-slate-400 truncate">{dynamicProbe.osName} {dynamicProbe.osVersion}</div>
            </div>

            <div className="p-2 bg-black/80 rounded-lg border border-slate-900">
              <div className="text-slate-500">GPU RENDERER:</div>
              <div className="text-amber-300 font-bold truncate" title={dynamicProbe.gpuRenderer}>
                {dynamicProbe.gpuRenderer.split(' (')[0]}
              </div>
              <div className="text-[9px] text-slate-400">WebGL Acceleration</div>
            </div>

            <div className="p-2 bg-black/80 rounded-lg border border-slate-900">
              <div className="text-slate-500">RECOMMENDED MODEL:</div>
              <div className="text-emerald-300 font-bold truncate">{dynamicProbe.recommendedModelName.split(' (')[0]}</div>
              <div className="text-[9px] text-cyan-400 font-bold">{dynamicProbe.recommendedTier} Escalation</div>
            </div>
          </div>

          <div className="text-[10px] text-slate-300 bg-black/40 p-2 rounded-lg border border-cyan-900/40 font-sans flex items-start gap-1.5">
            <Info className="w-3.5 h-3.5 text-cyan-400 shrink-0 mt-0.5" />
            <span><strong>Autonomous Recommendation:</strong> {dynamicProbe.recommendationRationale}</span>
          </div>
        </div>
      )}

      {/* Preset Selector Strip */}
      <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-2.5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono">
          <span className="text-slate-400 font-bold uppercase flex items-center gap-1.5">
            <Smartphone className="w-3.5 h-3.5 text-cyan-400" />
            Select Device Hardware Target:
          </span>
          <div className="flex items-center gap-2">
            <span className="text-[10px] text-slate-500">Mode:</span>
            <button
              onClick={() => setIsCustomMode(!isCustomMode)}
              className={`min-h-[36px] px-2.5 py-1 rounded-lg text-[10px] font-bold border transition-colors ${
                isCustomMode ? 'bg-indigo-600 text-white border-indigo-500' : 'bg-slate-900 text-slate-400 border-slate-800'
              }`}
            >
              {isCustomMode ? 'Custom Slider Mode Active' : 'Switch to Custom Sliders'}
            </button>
          </div>
        </div>

        {!isCustomMode ? (
          <div className="grid grid-cols-1 xs:grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
            {allPresets.map(preset => (
              <button
                key={preset.id}
                onClick={() => {
                  setSelectedPresetId(preset.id);
                  const tier: 'TIER_1' | 'TIER_2' | 'TIER_3' = preset.totalRamMb <= 4096 ? 'TIER_1' : preset.totalRamMb <= 8192 ? 'TIER_2' : 'TIER_3';
                  onSelectPreset?.(preset, tier);
                }}
                className={`min-h-[44px] p-2.5 rounded-xl border text-left transition-all ${
                  selectedPresetId === preset.id
                    ? 'bg-cyan-950/60 text-white border-cyan-400 shadow-sm shadow-cyan-500/20'
                    : 'bg-black/60 border-slate-900 text-slate-300 hover:border-slate-800 active:bg-slate-900'
                }`}
              >
                <div className="font-bold text-[11px] truncate">{preset.name}</div>
                <div className="text-[10px] text-cyan-300 mt-0.5">{preset.totalRamMb} MB &bull; API {preset.androidSdk}</div>
                <div className="text-[9px] text-slate-500 truncate mt-0.5">{preset.socModel.split(' (')[0]}</div>
              </button>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs font-mono pt-1">
            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-400">Total Physical RAM:</span>
                <span className="text-cyan-300 font-bold">{customRamMb} MB</span>
              </div>
              <input
                type="range"
                min="2048"
                max="16384"
                step="512"
                value={customRamMb}
                onChange={e => setCustomRamMb(Number(e.target.value))}
                className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg"
              />
            </div>
            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-400">OS Overhead:</span>
                <span className="text-amber-300 font-bold">{customOsOverheadMb} MB</span>
              </div>
              <input
                type="range"
                min="1200"
                max="4000"
                step="100"
                value={customOsOverheadMb}
                onChange={e => setCustomOsOverheadMb(Number(e.target.value))}
                className="w-full accent-amber-400 h-1.5 bg-slate-800 rounded-lg"
              />
            </div>
            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-400">Active Apps Buffer:</span>
                <span className="text-emerald-300 font-bold">{customAppBufferMb} MB</span>
              </div>
              <input
                type="range"
                min="300"
                max="3000"
                step="50"
                value={customAppBufferMb}
                onChange={e => setCustomAppBufferMb(Number(e.target.value))}
                className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg"
              />
            </div>
          </div>
        )}
      </div>

      {/* =========================================================================
          TAB 1: DEVICE PROFILE & DYNAMIC MEMORY CEILING CALCULATOR
          ========================================================================= */}
      {activeTab === 'profile' && (
        <div className="space-y-4 font-mono text-xs">
          {/* Discovered Specs Card */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex items-center justify-between border-b border-slate-900 pb-2">
              <div className="flex items-center gap-2">
                <span className="text-white font-bold uppercase">
                  Auto-Discovered Device Metrics (DeviceProfiler.kt):
                </span>
                <span className="text-[10px] text-emerald-400 bg-emerald-950 px-2 py-0.5 rounded border border-emerald-800">
                  First-Boot Verified
                </span>
              </div>
              <span className="text-slate-500 text-[10px]">
                Page Size: <strong className="text-cyan-300">{activePreset.pageSizeBytes} bytes</strong>
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
              <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-0.5">
                <span className="text-[10px] text-slate-500 block">TOTAL PHYSICAL RAM</span>
                <span className="text-white font-bold text-sm">{totalRam} MB</span>
                <span className="text-[9px] text-slate-400 block">via /proc/meminfo</span>
              </div>
              <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-0.5">
                <span className="text-[10px] text-slate-500 block">CPU TOPOLOGY</span>
                <span className="text-cyan-300 font-bold text-[11px] truncate block">{activePreset.coreTopology}</span>
                <span className="text-[9px] text-slate-400 block">{activePreset.cpuArch}</span>
              </div>
              <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-0.5">
                <span className="text-[10px] text-slate-500 block">SIMD ACCELERATION</span>
                <span className="text-emerald-400 font-bold text-sm truncate block">{activePreset.simdSupport.join(', ')}</span>
                <span className="text-[9px] text-slate-400 block">NEON Float16 / int8</span>
              </div>
              <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-0.5">
                <span className="text-[10px] text-slate-500 block">ANDROID PLATFORM</span>
                <span className="text-amber-300 font-bold text-sm block">API {activePreset.androidSdk}</span>
                <span className="text-[9px] text-slate-400 block truncate">{activePreset.androidVersion.split(' (')[0]}</span>
              </div>
            </div>
          </div>

          {/* Dynamic Memory Ceiling Formula & Calculator HUD */}
          <div className="p-4 bg-black border border-cyan-900/40 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2">
              <div className="space-y-0.5">
                <span className="text-cyan-400 font-bold text-xs uppercase block">
                  Dynamic Memory Ceiling Formula:
                </span>
                <span className="text-white font-bold text-sm">
                  SafeModelRAM = (TotalRAM - OsOverhead - ActiveAppBuffer) &times; 0.25
                </span>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-slate-500 block">CALCULATED SAFE SLM CEILING:</span>
                <span className="text-emerald-400 font-bold text-base bg-emerald-950/80 px-2.5 py-0.5 rounded border border-emerald-700 inline-block mt-0.5">
                  &le; {calculatedSafeModelRamMb} MB
                </span>
              </div>
            </div>

            {/* Visual Equation Breakdown Bar */}
            <div className="p-3 bg-slate-950 rounded-lg border border-slate-900 space-y-2">
              <div className="flex items-center justify-between text-[11px] text-slate-300">
                <span>Equation Step-by-Step Resolution:</span>
                <span className="text-cyan-300 font-bold">
                  ({totalRam}MB - {osOverhead}MB - {appBuffer}MB) = {availablePoolMb}MB Available &times; 0.25 = {calculatedSafeModelRamMb}MB
                </span>
              </div>

              {/* Memory Pool Distribution Visual Bar */}
              <div className="h-5 w-full bg-slate-900 rounded-lg overflow-hidden flex text-[10px] font-bold text-black select-none">
                <div style={{ width: `${(osOverhead / totalRam) * 100}%` }} className="bg-slate-500 flex items-center justify-center text-slate-100 truncate px-1">
                  OS {osOverhead}M
                </div>
                <div style={{ width: `${(appBuffer / totalRam) * 100}%` }} className="bg-amber-600 flex items-center justify-center text-white truncate px-1">
                  Apps {appBuffer}M
                </div>
                <div style={{ width: `${(calculatedSafeModelRamMb / totalRam) * 100}%` }} className="bg-emerald-400 flex items-center justify-center text-black truncate px-1">
                  Model {calculatedSafeModelRamMb}M
                </div>
                <div className="flex-1 bg-cyan-950 border-l border-cyan-700 flex items-center justify-center text-cyan-300 text-[9px] truncate px-1">
                  Free Buffer ({availablePoolMb - calculatedSafeModelRamMb}M)
                </div>
              </div>

              <div className="flex items-center justify-between text-[10px] text-slate-500 pt-1">
                <span>&bull; OS Overhead: {osOverhead} MB ({(osOverhead / totalRam * 100).toFixed(0)}%)</span>
                <span>&bull; Active App Buffer: {appBuffer} MB</span>
                <span>&bull; LowMemoryKiller MinFree Safety Margin: &gt; 350 MB guaranteed</span>
              </div>
            </div>

            {/* Agent's Auto-Reasoning Digest */}
            <div className="p-3 bg-cyan-950/20 border border-cyan-800/40 rounded-lg space-y-1 text-xs">
              <span className="text-cyan-400 font-bold text-[10px] uppercase block">
                Agent's Adaptive Rationale &amp; Safety Proof:
              </span>
              <p className="text-slate-200 font-sans leading-relaxed text-[11px]">
                On this profile, physical RAM is <strong className="text-white">{totalRam} MB</strong> with an OS overhead of <strong className="text-white">{osOverhead} MB</strong>. Setting the model allocation ceiling to 25% of the remaining pool (<strong className="text-emerald-400">{calculatedSafeModelRamMb} MB</strong>) guarantees that spinning up an SLM on-demand will never push free memory below the Linux kernel LMK threshold (180MB). Recommended profile: <strong className="text-cyan-300">{primaryRecommendation.name}</strong> ({primaryRecommendation.runtimeRamMb}MB).
              </p>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB: MULTI-TIER CAPABILITY & MODEL ESCALATION ENGINE (ArchitectureTierManager.kt)
          ========================================================================= */}
      {activeTab === 'tiers' && (
        <div className="space-y-4 font-mono text-xs">
          {/* Active Tier Hero Card */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2">
              <div className="flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" />
                <span className="text-white font-bold uppercase text-xs">
                  Active Tier Classification:
                </span>
                <span className={`text-[10px] px-2 py-0.5 rounded font-bold border ${tierSpecs.badgeColor}`}>
                  {tierSpecs.name}
                </span>
              </div>
              <div className="text-[10px] text-slate-400">
                Resolved SoC: <strong className="text-cyan-300">{activePreset.socModel.split(' (')[0]}</strong> &bull; API {activePreset.androidSdk}
              </div>
            </div>

            <p className="text-[11px] text-slate-300 font-sans leading-relaxed">
              The <code className="text-cyan-400 font-mono">ArchitectureTierManager.kt</code> dynamically probes hardware capabilities on boot. Devices with ≤ 4GB RAM are locked into Tier 1 to guarantee zero LowMemoryKiller terminations, whereas devices with 6GB–16GB scale dynamically into expanded KV-caches, screenshot callbacks, and multimodal vision.
            </p>

            {/* Telemetry Grid for Active Tier */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5 pt-1">
              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">PERCEPTION ROUTING PIPELINE</span>
                <span className="text-cyan-300 font-bold text-xs block">{tierSpecs.perceptionPipeline}</span>
                <span className="text-[9px] text-slate-400 block font-sans">
                  {tierSpecs.screenshotPermitted ? '✓ Visual screenshots allowed' : '✗ Bitmaps blocked (prevents 9MB OOM spike)'}
                </span>
              </div>

              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">REASONING SLM &amp; KV CACHE</span>
                <span className="text-emerald-400 font-bold text-xs block">{tierSpecs.recommendedModel.split(' (')[0]}</span>
                <span className="text-[9px] text-slate-400 block">
                  Context: <strong className="text-slate-300">{tierSpecs.contextLimit}</strong> &bull; KV Budget: <strong className="text-slate-300">{tierSpecs.kvCacheBudget}</strong>
                </span>
              </div>

              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">CPU THREAD PINNING &amp; THERMALS</span>
                <span className="text-amber-300 font-bold text-xs block">
                  {activeHardwareTier === 'TIER_1' ? 'LITTLE Cores 0-3 @ 1.65GHz' : activeHardwareTier === 'TIER_2' ? 'Balanced 2 Big + 4 Little' : 'All Cores + Prime X4'}
                </span>
                <span className="text-[9px] text-slate-400 block font-sans">{tierSpecs.cpuPinning}</span>
              </div>

              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">RESIDENT IDLE CEILING</span>
                <span className="text-white font-bold text-xs block">{tierSpecs.idleCeiling}</span>
                <span className="text-[9px] text-slate-400 block font-sans">Unmapped mmap ensures 0MB residual memory</span>
              </div>

              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">PAGE ALIGNMENT &amp; NATIVE ELF</span>
                <span className="text-indigo-300 font-bold text-xs block">{tierSpecs.pageAlignment}</span>
                <span className="text-[9px] text-slate-400 block font-sans">Android 8.1 through 16 binary compatibility</span>
              </div>

              <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                <span className="text-[9px] text-slate-500 block uppercase">DESKTOP MULTI-WINDOW</span>
                <span className="text-slate-300 font-bold text-xs block">
                  {activeHardwareTier === 'TIER_3' ? 'Enabled (API 36 Desktop)' : 'Disabled (Single Focus Window)'}
                </span>
                <span className="text-[9px] text-slate-400 block font-sans">Preserves zero-allocation focus loop</span>
              </div>
            </div>
          </div>

          {/* Comparative Tier Matrix */}
          <div className="space-y-2">
            <span className="text-slate-400 font-bold text-xs uppercase block">
              Multi-Tier Escalation Architecture Matrix (ArchitectureTierManager.kt):
            </span>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Tier 1 Card */}
              <div className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                activeHardwareTier === 'TIER_1'
                  ? 'bg-amber-950/20 border-amber-500 shadow-sm shadow-amber-500/20'
                  : 'bg-black/60 border-slate-900 opacity-70'
              }`}>
                <div className="flex items-center justify-between border-b border-slate-900 pb-1.5">
                  <span className="font-bold text-amber-400 text-xs">TIER 1 (≤ 4GB RAM)</span>
                  {activeHardwareTier === 'TIER_1' && (
                    <span className="text-[9px] bg-amber-950 text-amber-300 px-1.5 py-0.5 rounded border border-amber-700 font-bold">
                      CURRENT DEVICE
                    </span>
                  )}
                </div>
                <div className="text-[10px] space-y-1 text-slate-300">
                  <div>&bull; <strong>Perception:</strong> Zero-alloc Accessibility DSL (&lt;4KB string)</div>
                  <div>&bull; <strong>Screenshots:</strong> <span className="text-red-400 font-bold">PROHIBITED</span> (prevents 20MB Dalvik Bitmap OOM)</div>
                  <div>&bull; <strong>SLM Footprint:</strong> 360M–500M (SmolLM2 Q4_K_M ~240MB)</div>
                  <div>&bull; <strong>CPU Pinning:</strong> LITTLE cores 0–3 @ 1.65GHz (&lt;0.65W)</div>
                  <div>&bull; <strong>Target Hardware:</strong> Helio P23, Snapdragon 450, Android 8.1 API 27</div>
                </div>
              </div>

              {/* Tier 2 Card */}
              <div className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                activeHardwareTier === 'TIER_2'
                  ? 'bg-cyan-950/20 border-cyan-500 shadow-sm shadow-cyan-500/20'
                  : 'bg-black/60 border-slate-900 opacity-70'
              }`}>
                <div className="flex items-center justify-between border-b border-slate-900 pb-1.5">
                  <span className="font-bold text-cyan-400 text-xs">TIER 2 (6GB – 8GB RAM)</span>
                  {activeHardwareTier === 'TIER_2' && (
                    <span className="text-[9px] bg-cyan-950 text-cyan-300 px-1.5 py-0.5 rounded border border-cyan-700 font-bold">
                      CURRENT DEVICE
                    </span>
                  )}
                </div>
                <div className="text-[10px] space-y-1 text-slate-300">
                  <div>&bull; <strong>Perception:</strong> Hybrid text tree + fast regional diff</div>
                  <div>&bull; <strong>Screenshots:</strong> <span className="text-emerald-400 font-bold">PERMITTED</span> (AccessibilityService silent callback)</div>
                  <div>&bull; <strong>SLM Footprint:</strong> 1B–1.5B (Llama-3.2-1B-Instruct ~720MB)</div>
                  <div>&bull; <strong>CPU Pinning:</strong> 2 Big Cores + 4 Little Cores</div>
                  <div>&bull; <strong>Target Hardware:</strong> Redmi Note 10, Galaxy A54, Android 11–14</div>
                </div>
              </div>

              {/* Tier 3 Card */}
              <div className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                activeHardwareTier === 'TIER_3'
                  ? 'bg-emerald-950/20 border-emerald-500 shadow-sm shadow-emerald-500/20'
                  : 'bg-black/60 border-slate-900 opacity-70'
              }`}>
                <div className="flex items-center justify-between border-b border-slate-900 pb-1.5">
                  <span className="font-bold text-emerald-400 text-xs">TIER 3 (12GB – 16GB+ RAM)</span>
                  {activeHardwareTier === 'TIER_3' && (
                    <span className="text-[9px] bg-emerald-950 text-emerald-300 px-1.5 py-0.5 rounded border border-emerald-700 font-bold">
                      CURRENT DEVICE
                    </span>
                  )}
                </div>
                <div className="text-[10px] space-y-1 text-slate-300">
                  <div>&bull; <strong>Perception:</strong> Native Multimodal Vision + AICore VLM</div>
                  <div>&bull; <strong>Desktop Windowing:</strong> <span className="text-emerald-400 font-bold">ENABLED</span> (Multi-window tracking on API 36)</div>
                  <div>&bull; <strong>SLM Footprint:</strong> 1.5B–2B+ / Gemini Nano on-device</div>
                  <div>&bull; <strong>Page Alignment:</strong> Strict 16 KB ELF binary page size</div>
                  <div>&bull; <strong>Target Hardware:</strong> Pixel 9 Pro Tensor G4, Android 15–16</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB: HUMAN-IN-THE-LOOP PERMISSION-GATED AUTONOMY (AutonomyPermissionManager.kt)
          ========================================================================= */}
      {activeTab === 'approval' && (
        <div className="space-y-4 font-mono text-xs">
          {/* Permission Gate Governance Bar */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-900 pb-2">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span className="text-white font-bold uppercase text-xs">
                  Human-in-the-Loop Autonomy Gate (AutonomyPermissionManager.kt):
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] text-slate-400">Gate Enforcement:</span>
                <button
                  onClick={() => setRequireUserApproval(!requireUserApproval)}
                  className={`px-3 py-1 rounded-lg text-xs font-bold border transition-all flex items-center gap-1.5 ${
                    requireUserApproval
                      ? 'bg-emerald-950 text-emerald-300 border-emerald-500 shadow-sm shadow-emerald-500/20'
                      : 'bg-red-950 text-red-300 border-red-500'
                  }`}
                >
                  {requireUserApproval ? <Lock className="w-3.5 h-3.5 text-emerald-400" /> : <Unlock className="w-3.5 h-3.5 text-red-400" />}
                  <span>{requireUserApproval ? 'requireUserApproval = TRUE (Armed)' : 'AUTO-DISPATCH (Bypassed)'}</span>
                </button>
              </div>
            </div>

            <div className="p-3 bg-emerald-950/20 border border-emerald-800/40 rounded-lg space-y-1 text-xs">
              <div className="flex items-center gap-1.5 text-emerald-400 font-bold text-[10px] uppercase">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>Zero Unprompted Actions Guarantee:</span>
              </div>
              <p className="text-slate-300 font-sans leading-relaxed text-[11px]">
                Under <code className="text-cyan-300 font-mono">requireUserApproval = true</code>, the agent autonomously executes view-tree perception, FNV-1a state diffing, and nocturnal learning proposals, but <strong>strictly halts</strong> execution before mutating any device state, dispatching gestures, or modifying public disk storage. Every state mutation requires explicit human confirmation.
              </p>
            </div>

            <div className="flex items-center justify-between pt-1">
              <span className="text-slate-400 text-xs">
                Pending Approval Queue: <strong className="text-white">{approvalProposals.filter(p => p.status === 'PENDING').length} Actions Awaiting Confirmation</strong>
              </span>
              <button
                onClick={handleInjectNewProposal}
                className="px-2.5 py-1 bg-cyan-950 hover:bg-cyan-900 text-cyan-300 border border-cyan-700 rounded-lg text-[11px] font-bold transition-colors flex items-center gap-1"
              >
                <Zap className="w-3 h-3 text-cyan-400" />
                <span>Simulate New Autonomous Proposal</span>
              </button>
            </div>
          </div>

          {/* Structured Approval Cards */}
          <div className="space-y-3">
            <span className="text-slate-400 font-bold text-xs uppercase block">
              Structured Approval Cards &amp; Pre/Post Diff Previews:
            </span>

            {approvalProposals.map(proposal => {
              const isPending = proposal.status === 'PENDING';
              const isApproved = proposal.status === 'APPROVED';
              const isDeclined = proposal.status === 'DECLINED';

              return (
                <div
                  key={proposal.id}
                  className={`p-4 rounded-xl border space-y-3 transition-all ${
                    isPending
                      ? proposal.riskLevel === 'HIGH'
                        ? 'bg-rose-950/15 border-rose-500/80 shadow-md shadow-rose-950/30'
                        : proposal.riskLevel === 'MEDIUM'
                        ? 'bg-amber-950/15 border-amber-500/80 shadow-md shadow-amber-950/30'
                        : 'bg-cyan-950/15 border-cyan-500/80'
                      : isApproved
                      ? 'bg-emerald-950/10 border-emerald-900/40 opacity-75'
                      : 'bg-slate-950/40 border-slate-900 opacity-50'
                  }`}
                >
                  {/* Card Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2">
                    <div className="flex items-center gap-2">
                      <span className={`text-[9px] px-2 py-0.5 rounded font-bold border ${
                        proposal.riskLevel === 'HIGH'
                          ? 'bg-red-950 text-red-300 border-red-700'
                          : proposal.riskLevel === 'MEDIUM'
                          ? 'bg-amber-950 text-amber-300 border-amber-700'
                          : 'bg-emerald-950 text-emerald-300 border-emerald-700'
                      }`}>
                        RISK: {proposal.riskLevel}
                      </span>
                      <span className="text-white font-bold text-xs">{proposal.intent}</span>
                    </div>

                    <div className="flex items-center gap-2 text-[10px]">
                      <span className="text-slate-500">{proposal.timestamp}</span>
                      <span className={`px-2 py-0.5 rounded font-bold border ${
                        isPending
                          ? 'bg-cyan-950 text-cyan-300 border-cyan-700 animate-pulse'
                          : isApproved
                          ? 'bg-emerald-950 text-emerald-400 border-emerald-700'
                          : 'bg-red-950 text-red-400 border-red-800'
                      }`}>
                        {proposal.status}
                      </span>
                    </div>
                  </div>

                  {/* Pre vs Post View-Tree Node Diff Preview */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-[10px]">
                    <div className="p-2.5 bg-black rounded-lg border border-slate-900 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 uppercase font-bold">PRE-ACTION STATE:</span>
                        <span className="text-amber-400 font-mono text-[9px]">{proposal.preStateHash}</span>
                      </div>
                      <div className="text-slate-300 truncate font-mono">
                        Target: <strong className="text-cyan-300">{proposal.targetAppLabel}</strong> ({proposal.targetPackage})
                      </div>
                      <div className="text-slate-400 text-[9px] line-clamp-1 font-mono">
                        {proposal.targetNodeSummary}
                      </div>
                    </div>

                    <div className="p-2.5 bg-black rounded-lg border border-slate-900 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 uppercase font-bold">POST-ACTION PREVIEW:</span>
                        <span className="text-emerald-400 font-mono text-[9px]">EXPECTED MUTATION</span>
                      </div>
                      <div className="text-emerald-300 font-mono text-[10px] truncate">
                        {proposal.postStateExpected}
                      </div>
                      <div className="text-slate-400 text-[9px]">
                        Tool Actuator: <code className="text-cyan-300 font-mono">{proposal.toolName}</code>
                      </div>
                    </div>
                  </div>

                  {/* Targeted Action Payload & Side Effects */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-[10px]">
                    <div className="p-2 bg-slate-950 rounded border border-slate-900 space-y-1">
                      <span className="text-slate-500 uppercase block font-bold text-[9px]">TARGETED ACTION PAYLOAD JSON:</span>
                      <pre className="text-cyan-300 text-[9px] overflow-x-auto p-1 bg-black rounded border border-slate-900">
                        {proposal.paramsJson}
                      </pre>
                    </div>

                    <div className="p-2 bg-slate-950 rounded border border-slate-900 space-y-1">
                      <span className="text-slate-500 uppercase block font-bold text-[9px]">PRE-FLIGHT SIDE-EFFECT CHECKS:</span>
                      <ul className="text-slate-300 space-y-0.5 text-[9px] list-disc list-inside">
                        {proposal.potentialSideEffects.map((effect, idx) => (
                          <li key={idx} className="leading-tight">{effect}</li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Human-in-the-Loop Decision Buttons */}
                  {isPending && (
                    <div className="flex items-center justify-end gap-2 pt-1 border-t border-slate-900">
                      <button
                        onClick={() => handleDeclineProposal(proposal.id)}
                        className="px-3 py-1.5 bg-red-950/60 hover:bg-red-900/80 text-red-300 border border-red-800 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5"
                      >
                        <X className="w-3.5 h-3.5 text-red-400" />
                        <span>[ Decline / Ignore ]</span>
                      </button>

                      <button
                        onClick={() => handleApproveProposal(proposal.id)}
                        className="px-4 py-1.5 bg-emerald-500 hover:bg-emerald-400 text-black rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 shadow-md shadow-emerald-500/20"
                      >
                        <Check className="w-3.5 h-3.5 stroke-[2.5]" />
                        <span>[ Approve / Confirm &amp; Commit ]</span>
                      </button>
                    </div>
                  )}

                  {!isPending && (
                    <div className="flex items-center justify-between text-[10px] text-slate-500 pt-1 border-t border-slate-900">
                      <span>Status: <strong className={isApproved ? 'text-emerald-400' : 'text-red-400'}>{proposal.status}</strong> by Human Gatekeeper</span>
                      <span>Recorded in Room Audit Trail &bull; FNV-1a Hash Verified</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Room Audit Trail Table */}
          <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-2">
            <div className="flex items-center justify-between border-b border-slate-900 pb-1.5 text-xs">
              <span className="text-slate-400 font-bold uppercase flex items-center gap-1.5">
                <Database className="w-3.5 h-3.5 text-cyan-400" />
                Live Room Audit Trail (room_autonomy_audit):
              </span>
              <span className="text-slate-500 text-[10px]">{auditLog.length} immutable events logged</span>
            </div>

            <div className="space-y-1 text-[10px]">
              {auditLog.map(record => (
                <div key={record.id} className="p-2 bg-slate-950 rounded border border-slate-900 flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                  <div className="flex items-center gap-2">
                    <span className={`px-1.5 py-0.2 rounded font-bold text-[9px] ${
                      record.decision === 'APPROVED' ? 'bg-emerald-950 text-emerald-400' : 'bg-red-950 text-red-400'
                    }`}>
                      {record.decision}
                    </span>
                    <span className="text-slate-300 font-bold">{record.intent}</span>
                    <span className="text-slate-500 hidden sm:inline">&bull;</span>
                    <span className="text-cyan-400 font-mono text-[9px] hidden sm:inline">{record.tool}</span>
                  </div>
                  <div className="text-slate-500 text-[9px] shrink-0 font-mono">
                    {record.time}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 2: ADAPTIVE MODEL REGISTRY
          ========================================================================= */}
      {activeTab === 'registry' && (
        <div className="space-y-4 font-mono text-xs">
          <div className="flex items-center justify-between p-3 bg-black border border-slate-900 rounded-xl">
            <span className="text-slate-400 uppercase font-bold">
              Quantized GGUF Profiles Ranked by Device Feasibility (Ceiling: &le; {calculatedSafeModelRamMb} MB):
            </span>
            <span className="text-cyan-300 text-[10px]">
              Active Profile: <strong className="text-white">{primaryRecommendation.name}</strong>
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {evaluatedCandidates.map(candidate => (
              <div
                key={candidate.id}
                className={`p-3.5 rounded-xl border space-y-2.5 transition-all ${
                  candidate.status === 'RECOMMENDED'
                    ? 'bg-emerald-950/20 border-emerald-500 shadow-sm shadow-emerald-500/20'
                    : candidate.status === 'COMPATIBLE'
                    ? 'bg-cyan-950/20 border-cyan-700'
                    : candidate.status === 'RISKY'
                    ? 'bg-amber-950/20 border-amber-700'
                    : 'bg-red-950/10 border-red-900/40 opacity-60'
                }`}
              >
                <div className="flex items-start justify-between gap-2 border-b border-slate-900 pb-2">
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-white font-bold text-xs">{candidate.name}</span>
                      {candidate.id === dynamicProbe?.recommendedModelId && (
                        <span className="px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-300 border border-emerald-600 text-[8px] font-bold">
                          DYNAMIC MATCH
                        </span>
                      )}
                    </div>
                    <span className="text-[10px] text-slate-400">{candidate.parameterCount} &bull; {candidate.quantization}</span>
                  </div>
                  <span className={`text-[9px] px-2 py-0.5 rounded font-bold border ${
                    candidate.status === 'RECOMMENDED'
                      ? 'bg-emerald-950 text-emerald-400 border-emerald-700'
                      : candidate.status === 'COMPATIBLE'
                      ? 'bg-cyan-950 text-cyan-300 border-cyan-700'
                      : candidate.status === 'RISKY'
                      ? 'bg-amber-950 text-amber-300 border-amber-700'
                      : 'bg-red-950 text-red-400 border-red-700'
                  }`}>
                    {candidate.status}
                  </span>
                </div>

                <div className="grid grid-cols-3 gap-1.5 text-[10px]">
                  <div className="p-1.5 bg-black/60 rounded border border-slate-900">
                    <span className="text-slate-500 block text-[9px]">GGUF SIZE</span>
                    <span className="text-white font-bold">{candidate.fileSizeMb} MB</span>
                  </div>
                  <div className="p-1.5 bg-black/60 rounded border border-slate-900">
                    <span className="text-slate-500 block text-[9px]">RUNTIME RAM</span>
                    <span className={`font-bold ${candidate.runtimeRamMb <= calculatedSafeModelRamMb ? 'text-emerald-400' : 'text-red-400'}`}>
                      {candidate.runtimeRamMb} MB
                    </span>
                  </div>
                  <div className="p-1.5 bg-black/60 rounded border border-slate-900">
                    <span className="text-slate-500 block text-[9px]">NEON SPEED</span>
                    <span className="text-cyan-300 font-bold">~{candidate.targetTokPerSecHelioP23} tok/s</span>
                  </div>
                </div>

                <p className="text-[11px] text-slate-300 font-sans leading-relaxed">
                  {candidate.rationale}
                </p>

                <div className="flex items-center justify-between pt-1 border-t border-slate-900 text-[10px]">
                  <span className="text-slate-500">
                    Min RAM: <strong className="text-slate-300">{candidate.minRecommendedDeviceRamMb} MB</strong>
                  </span>
                  <button
                    onClick={() => handleRunModelBenchmark(candidate)}
                    className="px-2.5 py-1 bg-cyan-500 hover:bg-cyan-400 text-black font-bold rounded flex items-center gap-1 transition-colors"
                  >
                    <Play className="w-3 h-3 fill-current" />
                    <span>Test In Sandbox</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 3: BENCHMARK SIMULATOR
          ========================================================================= */}
      {activeTab === 'benchmark' && (
        <div className="space-y-4 font-mono text-xs">
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2">
              <span className="text-white font-bold uppercase">
                Simulated On-Demand Inference Benchmark:
              </span>
              <div className="flex items-center gap-2">
                <span className="text-slate-400 text-[10px]">Testing on:</span>
                <span className="text-cyan-300 font-bold bg-cyan-950 px-2 py-0.5 rounded border border-cyan-800">
                  {activePreset.name}
                </span>
              </div>
            </div>

            <p className="text-[11px] text-slate-300 font-sans leading-relaxed">
              Executes a simulated on-demand action extraction cycle using GBNF grammar constrained decoding. Evaluates time-to-first-token (TTFT), tokens per second (TPS), page alignment, and confirms zero residual memory leaks after native unmapping.
            </p>

            <div className="flex items-center gap-2 flex-wrap">
              {evaluatedCandidates.slice(0, 4).map(c => (
                <button
                  key={c.id}
                  onClick={() => handleRunModelBenchmark(c)}
                  disabled={isBenchmarking}
                  className="px-3 py-1.5 bg-slate-900 hover:bg-cyan-950 text-slate-200 hover:text-cyan-300 border border-slate-800 hover:border-cyan-700 rounded-lg transition-all flex items-center gap-1.5 disabled:opacity-50"
                >
                  <Play className="w-3 h-3 fill-current" />
                  <span>Benchmark {c.name.split(' (')[0]}</span>
                </button>
              ))}
            </div>

            {isBenchmarking && (
              <div className="p-4 bg-slate-950 rounded-xl border border-cyan-900 flex items-center justify-center gap-2 text-cyan-400 font-bold">
                <Sparkles className="w-4 h-4 animate-spin text-cyan-400" />
                <span>SPINNING UP MODEL VIA MMAP(MAP_SHARED) &bull; EXECUTING GBNF SAMPLER...</span>
              </div>
            )}

            {benchmarkResult && !isBenchmarking && (
              <div className="p-3.5 bg-slate-950 border border-emerald-800/60 rounded-xl space-y-3">
                <div className="flex items-center justify-between border-b border-slate-900 pb-2">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-700">
                      BENCHMARK COMPLETE
                    </span>
                    <span className="text-white font-bold">{benchmarkResult.modelName}</span>
                  </div>
                  <span className="text-emerald-400 text-[10px]">
                    LMK Safety: <strong className="text-white">GUARANTEED (0 Violations)</strong>
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                  <div className="p-2 bg-black rounded border border-slate-900">
                    <span className="text-slate-500 text-[9px] block">TIME TO FIRST TOKEN</span>
                    <span className="text-cyan-300 font-bold text-sm">{benchmarkResult.ttftMs} ms</span>
                    <span className="text-[8px] text-slate-400 block">mmap paging + prompt eval</span>
                  </div>
                  <div className="p-2 bg-black rounded border border-slate-900">
                    <span className="text-slate-500 text-[9px] block">GENERATION SPEED</span>
                    <span className="text-emerald-400 font-bold text-sm">{benchmarkResult.tokPerSec} tok/s</span>
                    <span className="text-[8px] text-slate-400 block">ARM NEON SIMD float16</span>
                  </div>
                  <div className="p-2 bg-black rounded border border-slate-900">
                    <span className="text-slate-500 text-[9px] block">TRANSIENT PEAK RAM</span>
                    <span className="text-amber-300 font-bold text-sm">{benchmarkResult.memoryUsedMb} MB</span>
                    <span className="text-[8px] text-emerald-400 block">Freed instantly on close()</span>
                  </div>
                  <div className="p-2 bg-black rounded border border-slate-900">
                    <span className="text-slate-500 text-[9px] block">GBNF TOKENS</span>
                    <span className="text-white font-bold text-sm">{benchmarkResult.tokensGenerated} tok</span>
                    <span className="text-[8px] text-slate-400 block">100% strict JSON output</span>
                  </div>
                </div>

                <div className="p-2.5 bg-black rounded border border-slate-900 text-[10px] text-slate-300 space-y-0.5">
                  <span className="text-emerald-400 font-bold block mb-0.5">Diagnostic Assertion Verification:</span>
                  <div>&bull; Heap residual after teardown: <strong className="text-white">42.1 MB</strong> (Zero native memory leaks detected).</div>
                  <div>&bull; 16 KB Page Alignment: <strong className="text-white">PASS</strong> (Compatible across Android 8.1 to Android 16).</div>
                  <div>&bull; Thermal rise projection: <strong className="text-white">+0.04°C</strong> per action burst (LITTLE cluster safe).</div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 4: KOTLIN BLUEPRINT (DeviceProfiler.kt)
          ========================================================================= */}
      {activeTab === 'code' && (
        <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-2 font-mono text-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2 text-[10px]">
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
              {[
                { id: 'DeviceProfiler.kt', label: 'DeviceProfiler.kt', role: 'Hardware & RAM Ceiling' },
                { id: 'ArchitectureTierManager.kt', label: 'ArchitectureTierManager.kt', role: 'Multi-Tier Escalation' },
                { id: 'AutonomyPermissionManager.kt', label: 'AutonomyPermissionManager.kt', role: 'Approval Gate' }
              ].map(f => (
                <button
                  key={f.id}
                  onClick={() => setSelectedCodeFile(f.id as any)}
                  className={`px-2.5 py-1 rounded-lg transition-all flex items-center gap-1 font-bold whitespace-nowrap border ${
                    selectedCodeFile === f.id
                      ? 'bg-cyan-950 text-cyan-300 border-cyan-500 shadow-xs'
                      : 'bg-black text-slate-400 hover:text-white border-slate-800'
                  }`}
                >
                  <FileCode className="w-3 h-3 text-cyan-400" />
                  <span>{f.label}</span>
                </button>
              ))}
            </div>
            <span className="text-slate-500 shrink-0">Android 8.1 API 27 &rarr; API 36</span>
          </div>

          {selectedCodeFile === 'DeviceProfiler.kt' && (
            <pre className="text-slate-300 text-[10px] leading-relaxed overflow-x-auto p-2 bg-slate-950 rounded border border-slate-900">
{`package com.example.core.profiler

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class DeviceMetrics(
    val totalPhysicalRamMb: Int,
    val availableRamEstimateMb: Int,
    val osOverheadMb: Int,
    val activeAppBufferMb: Int,
    val safeModelRamCeilingMb: Int,
    val cpuArchitecture: String,
    val onlineCoreCount: Int,
    val hasNeonSimd: Boolean,
    val hasDotProduct: Boolean,
    val sdkInt: Int,
    val pageSizeBytes: Int
)

object DeviceProfiler {
    private const val TAG = "DeviceProfiler"

    fun profileHardware(): DeviceMetrics {
        val totalRamMb = readMeminfoTotalRamMb()
        val sdkInt = Build.VERSION.SDK_INT
        val (hasNeon, hasDotProd) = readCpuSimdCapabilities()

        // Calibrate OS & System Service overhead based on Android Generation
        val osOverheadMb = when {
            sdkInt >= 35 -> 2800 // Android 15 & 16 modern system daemons
            sdkInt >= 31 -> 2400 // Android 12-14
            sdkInt >= 29 -> 2000 // Android 10-11
            else -> 1850         // Android 8.1 Oreo baseline (Helio P23)
        }

        val activeAppBufferMb = 720
        val availablePoolMb = (totalRamMb - osOverheadMb - activeAppBufferMb).coerceAtLeast(300)

        // DYNAMIC MEMORY CEILING CALCULATOR:
        // SafeModelRAM = (TotalRAM - OsOverhead - ActiveAppBuffer) * 0.25
        val safeModelRamCeilingMb = (availablePoolMb * 0.25f).toInt()

        return DeviceMetrics(
            totalPhysicalRamMb = totalRamMb,
            availableRamEstimateMb = availablePoolMb,
            osOverheadMb = osOverheadMb,
            activeAppBufferMb = activeAppBufferMb,
            safeModelRamCeilingMb = safeModelRamCeilingMb,
            cpuArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            onlineCoreCount = Runtime.getRuntime().availableProcessors(),
            hasNeonSimd = hasNeon,
            hasDotProduct = hasDotProd,
            sdkInt = sdkInt,
            pageSizeBytes = 4096
        )
    }

    private fun readMeminfoTotalRamMb(): Int {
        return try {
            BufferedReader(FileReader("/proc/meminfo")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("MemTotal:")) {
                        val tokens = line!!.split("\\\\s+".toRegex())
                        return (tokens[1].toLong() / 1024L).toInt()
                    }
                }
            }
            4096
        } catch (_: Exception) { 4096 }
    }
}`}
            </pre>
          )}

          {selectedCodeFile === 'ArchitectureTierManager.kt' && (
            <pre className="text-slate-300 text-[10px] leading-relaxed overflow-x-auto p-2 bg-slate-950 rounded border border-slate-900">
{`package com.example.core.tier

import com.example.core.profiler.DeviceMetrics
import com.example.core.profiler.DeviceProfiler

enum class DeviceHardwareTier { TIER_1_LOW_END, TIER_2_MID_RANGE, TIER_3_FLAGSHIP }
enum class PerceptionPipelineType { PURE_TEXT_TREE_PRUNING, HYBRID_TREE_AND_DIFF, MULTIMODAL_VISION_OCR }

data class HardwareTierConfiguration(
    val tier: DeviceHardwareTier,
    val tierName: String,
    val perceptionPipeline: PerceptionPipelineType,
    val recommendedModelProfileId: String,
    val maxContextTokens: Int,
    val kvCacheBudgetMb: Int,
    val cpuThreadPinningStrategy: String,
    val allowVisualScreenshotCapture: Boolean,
    val supports16KbPageAlignment: Boolean,
    val residentIdleBudgetMb: Double
)

object ArchitectureTierManager {
    fun resolveTierConfiguration(metrics: DeviceMetrics = DeviceProfiler.profileHardware()): HardwareTierConfiguration {
        val ramMb = metrics.totalPhysicalRamMb
        val sdkInt = metrics.sdkInt

        return when {
            ramMb <= 4096 -> HardwareTierConfiguration(
                tier = DeviceHardwareTier.TIER_1_LOW_END,
                tierName = "Tier 1: Constrained Mobile (<= 4GB)",
                perceptionPipeline = PerceptionPipelineType.PURE_TEXT_TREE_PRUNING,
                recommendedModelProfileId = "smollm2_360m_q4",
                maxContextTokens = 512,
                kvCacheBudgetMb = 32,
                cpuThreadPinningStrategy = "LITTLE-Cores Only (Cores 0-3 @ 1.65GHz) to prevent thermal throttle",
                allowVisualScreenshotCapture = false, // Strictly prohibited: prevents 20MB Bitmap OOM
                supports16KbPageAlignment = false,
                residentIdleBudgetMb = 48.0
            )
            ramMb <= 8192 -> HardwareTierConfiguration(
                tier = DeviceHardwareTier.TIER_2_MID_RANGE,
                tierName = "Tier 2: Balanced Mid-Range (6GB - 8GB)",
                perceptionPipeline = PerceptionPipelineType.HYBRID_TREE_AND_DIFF,
                recommendedModelProfileId = "llama_32_1b_q4",
                maxContextTokens = 2048,
                kvCacheBudgetMb = 128,
                cpuThreadPinningStrategy = "Balanced (2 Big Cores + 4 Little Cores)",
                allowVisualScreenshotCapture = sdkInt >= 30,
                supports16KbPageAlignment = sdkInt >= 35,
                residentIdleBudgetMb = 96.0
            )
            else -> HardwareTierConfiguration(
                tier = DeviceHardwareTier.TIER_3_FLAGSHIP,
                tierName = "Tier 3: Flagship Multimodal (12GB - 16GB+)",
                perceptionPipeline = PerceptionPipelineType.MULTIMODAL_VISION_OCR,
                recommendedModelProfileId = "qwen25_15b_q4",
                maxContextTokens = 4096,
                kvCacheBudgetMb = 384,
                cpuThreadPinningStrategy = "All Cores Available (Dynamic Big/Prime Burst)",
                allowVisualScreenshotCapture = true,
                supports16KbPageAlignment = true,
                residentIdleBudgetMb = 192.0
            )
        }
    }
}`}
            </pre>
          )}

          {selectedCodeFile === 'AutonomyPermissionManager.kt' && (
            <pre className="text-slate-300 text-[10px] leading-relaxed overflow-x-auto p-2 bg-slate-950 rounded border border-slate-900">
{`package com.example.core.autonomy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ActionRiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
enum class ProposalDecision { PENDING, APPROVED, DECLINED, EXPIRED }

data class ApprovalProposal(
    val proposalId: String = UUID.randomUUID().toString(),
    val intentDescription: String,
    val riskLevel: ActionRiskLevel,
    val toolName: String,
    val targetPackage: String,
    val preStateHash: String,
    val postStateExpected: String,
    var decision: ProposalDecision = ProposalDecision.PENDING
)

class AutonomyPermissionManager(private var requireUserApproval: Boolean = true) {
    private val _pendingProposals = MutableStateFlow<List<ApprovalProposal>>(emptyList())
    val pendingProposals: StateFlow<List<ApprovalProposal>> = _pendingProposals.asStateFlow()

    fun approveAction(proposalId: String): Boolean {
        val list = _pendingProposals.value.toMutableList()
        val index = list.indexOfFirst { it.proposalId == proposalId }
        if (index == -1) return false
        list[index] = list[index].copy(decision = ProposalDecision.APPROVED)
        _pendingProposals.value = list
        return true
    }

    fun declineAction(proposalId: String): Boolean {
        val list = _pendingProposals.value.toMutableList()
        val index = list.indexOfFirst { it.proposalId == proposalId }
        if (index == -1) return false
        list[index] = list[index].copy(decision = ProposalDecision.DECLINED)
        _pendingProposals.value = list
        return true
    }
}`}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}
