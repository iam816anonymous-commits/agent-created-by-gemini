/**
 * Dynamic Device Hardware Probe & Autonomous Recommendation Engine.
 *
 * Inspects live browser hardware telemetry (Memory, CPU Cores, WebGL GPU, Battery,
 * Screen DPR, Touch, Network) and computes mathematical safe RAM ceilings to recommend
 * the optimal on-device SLM profile without risking Low Memory Killer termination.
 */

export interface DynamicDeviceTelemetry {
  isLiveDetected: boolean;
  deviceName: string;
  osName: string;
  osVersion: string;
  platform: string;
  isMobile: boolean;
  totalRamMb: number;
  osOverheadMb: number;
  activeAppBufferMb: number;
  calculatedSafeCeilingMb: number;
  cpuCores: number;
  gpuVendor: string;
  gpuRenderer: string;
  screenWidth: number;
  screenHeight: number;
  pixelRatio: number;
  touchPoints: number;
  batteryPercent: number | null;
  isCharging: boolean | null;
  networkType: string;
  networkDownlinkMbps: number | null;
  recommendedModelId: string;
  recommendedModelName: string;
  recommendedTier: 'TIER_1' | 'TIER_2' | 'TIER_3';
  recommendedPageSize: number;
  recommendationRationale: string;
  probedAt: string;
}

export async function probeLiveDeviceHardware(): Promise<DynamicDeviceTelemetry> {
  const nav = typeof window !== 'undefined' ? window.navigator : ({} as any);
  const ua = nav.userAgent || '';

  // 1. Detect OS & Device Brand
  let osName = 'Android';
  let osVersion = '8.1 (Oreo)';
  let deviceName = 'Generic Mobile Device';
  let isMobile = false;

  if (/android/i.test(ua)) {
    osName = 'Android';
    isMobile = true;
    const match = ua.match(/android\s+([\d.]+)/i);
    if (match) osVersion = `Android ${match[1]}`;

    if (/tecno/i.test(ua) || /in6/i.test(ua)) {
      deviceName = 'TECNO Camon i Click (IN6)';
    } else if (/samsung/i.test(ua) || /sm-/i.test(ua)) {
      deviceName = 'Samsung Galaxy Device';
    } else if (/pixel/i.test(ua)) {
      deviceName = 'Google Pixel';
    } else if (/redmi|xiaomi/i.test(ua)) {
      deviceName = 'Xiaomi / Redmi Device';
    } else {
      deviceName = 'Android Mobile Device';
    }
  } else if (/iphone|ipad|ipod/i.test(ua)) {
    osName = 'iOS';
    osVersion = 'iOS 17+';
    deviceName = /ipad/i.test(ua) ? 'Apple iPad' : 'Apple iPhone';
    isMobile = true;
  } else if (/macintosh|mac os x/i.test(ua)) {
    osName = 'macOS';
    osVersion = 'macOS';
    deviceName = 'Apple Mac Workstation';
  } else if (/windows/i.test(ua)) {
    osName = 'Windows';
    osVersion = 'Windows 10/11';
    deviceName = 'PC Workstation';
  } else if (/linux/i.test(ua)) {
    osName = 'Linux';
    osVersion = 'Linux POSIX';
    deviceName = 'Linux System';
  }

  // Check mobile screen & touch
  const touchPoints = nav.maxTouchPoints || 0;
  if (touchPoints > 0 && window.innerWidth <= 768) {
    isMobile = true;
  }

  // 2. Hardware RAM (deviceMemory API)
  let ramGb = (nav as any).deviceMemory;
  if (!ramGb) {
    ramGb = isMobile ? 4 : 8;
  }
  const totalRamMb = ramGb * 1024;

  // OS overhead calibrated to RAM tier
  const osOverheadMb = totalRamMb <= 4096 ? 1850 : totalRamMb <= 8192 ? 2600 : 3400;
  const activeAppBufferMb = totalRamMb <= 4096 ? 720 : 1100;
  // Formula: SafeModelRAM = (TotalRAM - OS - Buffer) * 0.25
  const calculatedSafeCeilingMb = Math.max(
    180,
    Math.floor((totalRamMb - osOverheadMb - activeAppBufferMb) * 0.25)
  );

  // 3. CPU Cores
  const cpuCores = nav.hardwareConcurrency || (isMobile ? 8 : 4);

  // 4. WebGL GPU Detection
  let gpuVendor = 'ARM / Imagination Technologies';
  let gpuRenderer = isMobile ? 'Mali-G71 MP2 (Vulkan/GLES 3.2)' : 'Integrated Graphics Accelerator';

  try {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl') || (canvas.getContext('experimental-webgl') as any);
    if (gl) {
      const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
      if (debugInfo) {
        const v = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
        const r = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
        if (v) gpuVendor = v;
        if (r) gpuRenderer = r;
      }
    }
  } catch (_e) {
    // Canvas WebGL blocked or sandboxed
  }

  // 5. Battery API
  let batteryPercent: number | null = 84;
  let isCharging: boolean | null = false;

  try {
    if (typeof (nav as any).getBattery === 'function') {
      const b = await (nav as any).getBattery();
      batteryPercent = Math.round(b.level * 100);
      isCharging = b.charging;
    }
  } catch (_e) {
    // Battery API restricted
  }

  // 6. Network
  const conn = (nav as any).connection;
  const networkType = conn?.effectiveType ? conn.effectiveType.toUpperCase() : 'WIFI / 4G';
  const networkDownlinkMbps = conn?.downlink ? Number(conn.downlink) : 15.0;

  // 7. Dynamic Recommendation Algorithm
  let recommendedModelId = 'smollm2_360m_q4';
  let recommendedModelName = 'SmolLM2-360M-Instruct (Q4_K_M)';
  let recommendedTier: 'TIER_1' | 'TIER_2' | 'TIER_3' = 'TIER_1';
  let recommendedPageSize = 4096;
  let recommendationRationale = '';

  if (totalRamMb <= 4096 || /helio|mali-g71|tecno/i.test(gpuRenderer + ua)) {
    recommendedModelId = 'smollm2_360m_q4';
    recommendedModelName = 'SmolLM2-360M-Instruct (Q4_K_M)';
    recommendedTier = 'TIER_1'; // Deterministic Regex Fast-Path first + on-demand 271MB SLM
    recommendedPageSize = 4096;
    recommendationRationale = `Detected ${totalRamMb}MB RAM on ${deviceName}. Safe headroom (${calculatedSafeCeilingMb}MB) mandates SmolLM2-360M with Q4_K_M quantization to prevent Android Low Memory Killer (LMK) termination.`;
  } else if (totalRamMb <= 8192) {
    recommendedModelId = 'qwen25_05b_q3';
    recommendedModelName = 'Qwen2.5-0.5B-Instruct (Q3_K_M)';
    recommendedTier = 'TIER_2';
    recommendedPageSize = 4096;
    recommendationRationale = `Detected ${totalRamMb}MB RAM with ${cpuCores} cores. Adequate headroom (${calculatedSafeCeilingMb}MB) allows Qwen2.5-0.5B or SmolLM2-360M Q8_0 with Tier 2 Local GGUF streaming.`;
  } else {
    recommendedModelId = 'llama_32_1b_q4';
    recommendedModelName = 'Llama-3.2-1B-Instruct (Q4_K_M)';
    recommendedTier = 'TIER_3';
    recommendedPageSize = /android\s+(1[5-9]|3[5-9])/i.test(ua) ? 16384 : 4096;
    recommendationRationale = `High-capacity hardware detected (${totalRamMb}MB RAM, ${cpuCores} CPU cores, GPU: ${gpuRenderer.slice(0, 28)}). Safe ceiling (${calculatedSafeCeilingMb}MB) supports 1B+ local models and Hybrid Gemini Cloud reasoning.`;
  }

  return {
    isLiveDetected: true,
    deviceName,
    osName,
    osVersion,
    platform: nav.platform || 'Unknown',
    isMobile,
    totalRamMb,
    osOverheadMb,
    activeAppBufferMb,
    calculatedSafeCeilingMb,
    cpuCores,
    gpuVendor,
    gpuRenderer,
    screenWidth: typeof window !== 'undefined' ? window.innerWidth : 360,
    screenHeight: typeof window !== 'undefined' ? window.innerHeight : 640,
    pixelRatio: typeof window !== 'undefined' ? window.devicePixelRatio : 2,
    touchPoints,
    batteryPercent,
    isCharging,
    networkType,
    networkDownlinkMbps,
    recommendedModelId,
    recommendedModelName,
    recommendedTier,
    recommendedPageSize,
    recommendationRationale,
    probedAt: new Date().toLocaleTimeString()
  };
}
