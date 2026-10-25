/**
 * Web Torch Actuator & Multimodal Waving Engine.
 *
 * Integrated with the Authoritative UnifiedHardwareController.
 * Real Hardware Operations:
 * 1. Physical Hardware Torch via Web Camera API (MediaStreamTrack torch constraint).
 * 2. Multi-format waving optical patterns: SINE_WAVE, DUAL_ALTERNATING, STROBE_BURST, SOS_BEACON, HEARTBEAT.
 * 3. Synced multimodal haptics and procedural harmonic audio.
 * 4. Transparent distinction between physical hardware execution and visual animation simulation.
 */

import { unifiedHardwareController } from '../hardware/UnifiedHardwareController';
import { OpticalWaveFormat } from '../hardware/types';

export type FlashlightWaveFormat = OpticalWaveFormat;

class WebTorchActuator {
  private waveAnimationId: number | null = null;
  private audioContext: AudioContext | null = null;
  private currentIntensity = 0;

  async init(): Promise<boolean> {
    const reg = await unifiedHardwareController.discoverCapabilities();
    const torchActuator = reg.actuators.find(a => a.type === 'TORCH');
    return !!torchActuator && torchActuator.provenance !== 'UNSUPPORTED';
  }

  async acquireCameraTorch(cameraId?: string): Promise<boolean> {
    const res = await unifiedHardwareController.executeHardwareAction('TORCH_ON', {
      enabled: false, // probe check without turning on
      cameraId
    });
    // Check observed actuator
    const state = unifiedHardwareController.getObservedHardwareState('actuator-torch-primary');
    return state.status !== 'UNSUPPORTED';
  }

  async setHardwareTorch(on: boolean): Promise<boolean> {
    const res = await unifiedHardwareController.executeHardwareAction(on ? 'TORCH_ON' : 'TORCH_OFF', {
      enabled: on
    });
    return res.status === 'COMMAND_ACCEPTED_UNVERIFIED' || res.status === 'SUCCESS_VERIFIED';
  }

  releaseHardwareTorch() {
    this.stopWave();
    unifiedHardwareController.executeHardwareAction('TORCH_OFF', { enabled: false });
  }

  startWave(
    format: FlashlightWaveFormat,
    speedMultiplier: number = 1.0,
    onFrame: (intensity: number, target: 'BACK' | 'FRONT' | 'DUAL') => void
  ) {
    this.stopWave();
    const startTime = performance.now();

    // Notify UnifiedHardwareController
    unifiedHardwareController.executeHardwareAction('TORCH_WAVE', {
      enabled: true,
      waveFormat: format,
      speedMultiplier
    });

    const loop = (now: number) => {
      const elapsed = (now - startTime) * speedMultiplier;
      let intensity = 1.0;
      let emitter: 'BACK' | 'FRONT' | 'DUAL' = 'BACK';

      switch (format) {
        case 'CONSTANT':
          intensity = 1.0;
          emitter = 'BACK';
          break;

        case 'SINE_WAVE': {
          const phase = (elapsed % 1800) / 1800;
          intensity = 0.15 + 0.85 * (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI - Math.PI / 2));
          emitter = phase > 0.5 ? 'DUAL' : 'BACK';
          break;
        }

        case 'STROBE_BURST': {
          const cycle = Math.floor(elapsed / 80) % 2;
          intensity = cycle === 0 ? 1.0 : 0.05;
          emitter = 'DUAL';
          break;
        }

        case 'DUAL_ALTERNATING': {
          const pos = (elapsed % 2400) / 2400;
          if (pos < 0.35) {
            emitter = 'BACK';
            intensity = 0.2 + 0.8 * Math.sin((pos / 0.35) * Math.PI);
          } else if (pos < 0.50) {
            emitter = 'DUAL';
            intensity = 1.0;
          } else if (pos < 0.85) {
            emitter = 'FRONT';
            intensity = 0.2 + 0.8 * Math.sin(((pos - 0.5) / 0.35) * Math.PI);
          } else {
            emitter = 'DUAL';
            intensity = 0.7;
          }
          break;
        }

        case 'SOS_BEACON': {
          const t = Math.floor(elapsed / 150) % 24;
          const isDit = t === 0 || t === 2 || t === 4;
          const isDah = t === 8 || t === 9 || t === 11 || t === 12 || t === 14 || t === 15;
          const isDit2 = t === 18 || t === 20 || t === 22;
          intensity = isDit || isDah || isDit2 ? 1.0 : 0.0;
          emitter = isDah ? 'DUAL' : 'BACK';
          break;
        }

        case 'HEARTBEAT': {
          const beatPos = (elapsed % 1000) / 1000;
          if (beatPos < 0.15) {
            intensity = Math.sin((beatPos / 0.15) * Math.PI);
          } else if (beatPos >= 0.25 && beatPos < 0.45) {
            intensity = 0.8 * Math.sin(((beatPos - 0.25) / 0.2) * Math.PI);
          } else {
            intensity = 0.05;
          }
          emitter = 'BACK';
          break;
        }
      }

      this.currentIntensity = intensity;
      onFrame(intensity, emitter);

      this.waveAnimationId = requestAnimationFrame(loop);
    };

    this.waveAnimationId = requestAnimationFrame(loop);
  }

  stopWave() {
    if (this.waveAnimationId !== null) {
      cancelAnimationFrame(this.waveAnimationId);
      this.waveAnimationId = null;
    }
    this.currentIntensity = 0;
  }

  getCurrentIntensity(): number {
    return this.currentIntensity;
  }

  triggerHapticWave(format: FlashlightWaveFormat) {
    let pattern: number | number[] = 50;
    switch (format) {
      case 'SINE_WAVE':
        pattern = [40, 60, 80, 60, 120];
        break;
      case 'STROBE_BURST':
        pattern = [20, 20, 20, 20, 20, 20, 40];
        break;
      case 'DUAL_ALTERNATING':
        pattern = [60, 80, 140, 80, 60];
        break;
      case 'SOS_BEACON':
        pattern = [100, 100, 100, 100, 100, 200, 300, 100, 300, 100, 300, 200, 100, 100, 100];
        break;
      case 'HEARTBEAT':
        pattern = [80, 80, 120];
        break;
      default:
        pattern = 40;
        break;
    }
    unifiedHardwareController.executeHardwareAction('HAPTIC_PULSE', { pattern });
  }

  playWaveChime(format: FlashlightWaveFormat) {
    let freq = 587.33;
    if (format === 'SINE_WAVE') freq = 660;
    else if (format === 'HEARTBEAT') freq = 110;
    else if (format === 'STROBE_BURST') freq = 880;

    unifiedHardwareController.executeHardwareAction('AUDIO_TONE', {
      frequency: freq,
      durationMs: 300
    });
  }
}

export const webTorchActuator = new WebTorchActuator();
