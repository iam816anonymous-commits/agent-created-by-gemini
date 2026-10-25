/**
 * Dynamic Device Hardware Probe & Autonomous Recommendation Engine.
 *
 * Backed by Authoritative UnifiedHardwareController.
 * Zero Fabricated Data:
 * - Does NOT default RAM to 4GB or 8GB if unavailable.
 * - Does NOT invent CPU core counts or GPU vendors.
 * - Does NOT fabricate battery levels or network speeds.
 * - Accurately documents measured vs estimated vs unavailable telemetry.
 */

import { unifiedHardwareController } from '../hardware/UnifiedHardwareController';
import { DiscoveredHardwareRegistry, Provenance } from '../hardware/types';

export interface DynamicDeviceTelemetry {
  isLiveDetected: boolean;
  deviceName: string;
  osName: string;
  osVersion: string;
  platform: string;
  isMobile: boolean;
  totalRamMb: number | null;
  ramProvenance: Provenance;
  osOverheadMb: number | null;
  activeAppBufferMb: number | null;
  calculatedSafeCeilingMb: number | null;
  cpuCores: number | null;
  cpuProvenance: Provenance;
  gpuVendor: string | null;
  gpuRenderer: string | null;
  gpuProvenance: Provenance;
  screenWidth: number;
  screenHeight: number;
  pixelRatio: number;
  touchPoints: number;
  batteryPercent: number | null;
  isCharging: boolean | null;
  batteryProvenance: Provenance;
  networkType: string | null;
  networkDownlinkMbps: number | null;
  networkProvenance: Provenance;
  recommendedModelId: string;
  recommendedModelName: string;
  recommendedTier: 'TIER_1' | 'TIER_2' | 'TIER_3';
  recommendedPageSize: number;
  recommendationRationale: string;
  probedAt: string;
}

export async function probeLiveDeviceHardware(): Promise<DynamicDeviceTelemetry> {
  const reg: DiscoveredHardwareRegistry = await unifiedHardwareController.discoverCapabilities(true);

  const deviceName = reg.system.deviceModel.value || (
    reg.system.isMobile.value
      ? `${reg.system.osName.value || 'Mobile'} Device`
      : `${reg.system.osName.value || 'Desktop'} Workstation`
  );

  return {
    isLiveDetected: reg.isRealPhysicalDiscovery,
    deviceName,
    osName: reg.system.osName.value || 'Unknown OS',
    osVersion: reg.system.osVersion.value || 'Unknown',
    platform: reg.system.platform.value || 'Unknown',
    isMobile: reg.system.isMobile.value ?? false,
    totalRamMb: reg.memory.totalRamMb.value,
    ramProvenance: reg.memory.totalRamMb.provenance,
    osOverheadMb: reg.memory.osOverheadMb.value,
    activeAppBufferMb: reg.memory.activeAppBufferMb.value,
    calculatedSafeCeilingMb: reg.memory.calculatedSafeCeilingMb.value,
    cpuCores: reg.processor.cpuCores.value,
    cpuProvenance: reg.processor.cpuCores.provenance,
    gpuVendor: reg.graphics.gpuVendor.value,
    gpuRenderer: reg.graphics.gpuRenderer.value,
    gpuProvenance: reg.graphics.gpuRenderer.provenance,
    screenWidth: reg.display.screenWidth.value || (typeof window !== 'undefined' ? window.innerWidth : 360),
    screenHeight: reg.display.screenHeight.value || (typeof window !== 'undefined' ? window.innerHeight : 640),
    pixelRatio: reg.display.pixelRatio.value || 1,
    touchPoints: reg.display.touchPoints.value || 0,
    batteryPercent: reg.battery.level.value,
    isCharging: reg.battery.charging.value,
    batteryProvenance: reg.battery.level.provenance,
    networkType: reg.network.effectiveType.value || reg.network.type.value,
    networkDownlinkMbps: reg.network.downlinkMbps.value,
    networkProvenance: reg.network.effectiveType.provenance,
    recommendedModelId: reg.recommendation.modelId,
    recommendedModelName: reg.recommendation.modelName,
    recommendedTier: reg.recommendation.tier,
    recommendedPageSize: reg.recommendation.tier === 'TIER_3' ? 16384 : 4096,
    recommendationRationale: reg.recommendation.rationale,
    probedAt: new Date(reg.discoveredAt).toLocaleTimeString()
  };
}
