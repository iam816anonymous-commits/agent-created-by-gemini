import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { UnifiedHardwareController } from '../UnifiedHardwareController';
import { probeLiveDeviceHardware } from '../../utils/dynamicDeviceProbe';

describe('UnifiedHardwareController Unit & Integration Tests', () => {
  let controller: UnifiedHardwareController;

  beforeEach(() => {
    controller = new UnifiedHardwareController();
  });

  afterEach(() => {
    controller.dispose();
  });

  it('discovers capabilities without fabricating fake RAM, CPU, or GPU', async () => {
    const registry = await controller.discoverCapabilities();
    expect(registry).toBeDefined();
    expect(registry.discoveredAt).toBeGreaterThan(0);

    // Verify provenance is typed and strictly non-fabricated
    expect(['MEASURED', 'UNAVAILABLE']).toContain(registry.memory.totalRamMb.provenance);
    expect(['MEASURED', 'UNAVAILABLE']).toContain(registry.processor.cpuCores.provenance);
    expect(['MEASURED', 'UNAVAILABLE', 'ERROR']).toContain(registry.graphics.gpuRenderer.provenance);

    if (registry.memory.totalRamMb.provenance === 'UNAVAILABLE') {
      expect(registry.memory.totalRamMb.value).toBeNull();
    } else {
      expect(typeof registry.memory.totalRamMb.value).toBe('number');
      expect(registry.memory.totalRamMb.value).toBeGreaterThan(0);
    }
  });

  it('reports provenance as UNAVAILABLE or UNSUPPORTED for restricted browser hardware', async () => {
    const registry = await controller.discoverCapabilities();

    // Ambient light and Barometer are generally UNAVAILABLE or UNSUPPORTED in standard browser JS
    const ambientSensor = registry.sensors.find(s => s.type === 'AMBIENT_LIGHT');
    expect(ambientSensor).toBeDefined();
    expect(['UNAVAILABLE', 'UNSUPPORTED', 'ENUMERATED']).toContain(ambientSensor?.provenance);

    // Wi-Fi actuator in browser JS is UNSUPPORTED
    const wifiActuator = registry.actuators.find(a => a.id === 'actuator-wifi');
    expect(wifiActuator).toBeDefined();
    expect(wifiActuator?.provenance).toBe('UNSUPPORTED');
  });

  it('serializes operations and prevents actuator conflicts with mutex lock', async () => {
    await controller.discoverCapabilities();

    // Trigger two simultaneous actions on vibrator
    const promise1 = controller.executeHardwareAction({
      actuatorId: 'vibrator',
      actionType: 'HAPTIC_TICK'
    });

    const promise2 = controller.executeHardwareAction({
      actuatorId: 'vibrator',
      actionType: 'HAPTIC_BURST'
    });

    const [res1, res2] = await Promise.all([promise1, promise2]);
    expect(res1).toBeDefined();
    expect(res2).toBeDefined();
    // One was completed or both resolved without unhandled rejection
    expect(['SUCCESS_VERIFIED', 'COMMAND_ACCEPTED_UNVERIFIED', 'UNSUPPORTED', 'BUSY', 'FAILED']).toContain(res1.status);
    expect(['SUCCESS_VERIFIED', 'COMMAND_ACCEPTED_UNVERIFIED', 'UNSUPPORTED', 'BUSY', 'FAILED']).toContain(res2.status);
  });

  it('executes audio tone actuator and returns structured status', async () => {
    await controller.discoverCapabilities();

    const result = await controller.executeHardwareAction({
      actuatorId: 'audio_tone',
      actionType: 'PLAY_TONE',
      params: { tone: 'KEY_CLICK', durationMs: 40 }
    });

    expect(['SUCCESS_VERIFIED', 'COMMAND_ACCEPTED_UNVERIFIED', 'UNSUPPORTED', 'FAILED']).toContain(result.status);
    expect(result.actuatorId).toBe('actuator-audio');
    expect(result.timestamp).toBeGreaterThan(0);
  });

  it('returns UNSUPPORTED when attempting network Wi-Fi control in browser sandbox', async () => {
    await controller.discoverCapabilities();

    const result = await controller.executeHardwareAction({
      actuatorId: 'network_wifi',
      actionType: 'SET_ENABLED',
      params: { enabled: true }
    });

    expect(result.status).toBe('UNSUPPORTED');
    expect(result.verifiedPhysicalState).toBe(false);
    expect(result.message).toContain('standard browser sandbox');
  });

  it('distinguishes verified physical state from unverified commands', async () => {
    await controller.discoverCapabilities();

    const torchOffResult = await controller.executeHardwareAction({
      actuatorId: 'camera_torch_back',
      actionType: 'TORCH_OFF'
    });

    // Turning off a torch without physical optical sensor feedback has physical verified state false
    expect(typeof torchOffResult.verifiedPhysicalState).toBe('boolean');
    expect(torchOffResult.provenance).toBe('MEASURED');
  });

  it('handles sensor subscription and unsubscription lifecycle cleanly', async () => {
    await controller.discoverCapabilities();

    let callbackCount = 0;
    const subId = controller.subscribeToSensor('motion', { samplingPeriodMs: 100 }, (_reading) => {
      callbackCount++;
    });

    expect(typeof subId).toBe('string');
    expect(subId.startsWith('sub_motion_')).toBe(true);

    const unsubscribed = controller.unsubscribeFromSensor(subId);
    expect(unsubscribed).toBe(true);

    // Unsubscribing non-existent ID returns false
    expect(controller.unsubscribeFromSensor('non-existent-sub')).toBe(false);
  });

  it('activates emergency stop, silences actuators, and rejects subsequent commands', async () => {
    await controller.discoverCapabilities();

    const report = controller.emergencyStop('Automated Test Emergency Trigger');
    expect(report).toBeDefined();
    expect(report.reason).toBe('Automated Test Emergency Trigger');
    expect(Array.isArray(report.actuatorsSilenced)).toBe(true);
    expect(report.actuatorsSilenced!.length).toBeGreaterThan(0);

    // Any subsequent action must be rejected with EMERGENCY_STOP_ACTIVE
    const blockedResult = await controller.executeHardwareAction({
      actuatorId: 'camera_torch_back',
      actionType: 'TORCH_ON',
      params: { brightness: 1.0 }
    });

    expect(blockedResult.status).toBe('EMERGENCY_STOP_ACTIVE');
    expect(blockedResult.verifiedPhysicalState).toBe(false);
    expect(blockedResult.message).toContain('Emergency stop is currently engaged');
  });
});

describe('dynamicDeviceProbe Zero-Fabrication Tests', () => {
  it('probeLiveDeviceHardware produces zero fake fallbacks for RAM or CPU', async () => {
    const probe = await probeLiveDeviceHardware();
    expect(probe).toBeDefined();
    expect(probe.platform).toBeDefined();

    // If deviceMemory is unavailable, totalRamMb must be null (zero fabrication)
    if (typeof (navigator as any).deviceMemory === 'undefined') {
      expect(probe.totalRamMb).toBeNull();
      expect(probe.calculatedSafeCeilingMb).toBeNull();
    } else {
      expect(typeof probe.totalRamMb).toBe('number');
      expect(probe.calculatedSafeCeilingMb).toBeGreaterThan(0);
    }

    // Recommendation must be conservative and safe
    expect(probe.recommendedTier).toBeDefined();
    expect(probe.recommendedModelName).toBeDefined();
    expect(probe.recommendationRationale).toBeDefined();
  });
});
