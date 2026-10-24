/**
 * Web Torch Actuator & Multimodal Waving Engine.
 *
 * Integrates:
 * 1. Physical Hardware Torch via Web Camera API (MediaStreamTrack torch constraint) on supported mobile browsers.
 * 2. Multi-format waving optical patterns: SINE_WAVE, DUAL_ALTERNATING, STROBE_BURST, SOS_BEACON, HEARTBEAT.
 * 3. Synced multimodal haptics (navigator.vibrate) and harmonic audio pitch modulation.
 */

export type FlashlightWaveFormat =
  | 'CONSTANT'
  | 'SINE_WAVE'
  | 'STROBE_BURST'
  | 'DUAL_ALTERNATING'
  | 'SOS_BEACON'
  | 'HEARTBEAT';

class WebTorchActuator {
  private mediaStream: MediaStream | null = null;
  private videoTrack: MediaStreamTrack | null = null;
  private isTorchSupported = false;
  private waveAnimationId: number | null = null;
  private audioContext: AudioContext | null = null;

  async init(): Promise<boolean> {
    if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      return false;
    }
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const hasVideo = devices.some(d => d.kind === 'videoinput');
      return hasVideo;
    } catch (_e) {
      return false;
    }
  }

  async acquireCameraTorch(): Promise<boolean> {
    if (this.videoTrack) return this.isTorchSupported;
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } }
      });
      this.videoTrack = this.mediaStream.getVideoTracks()[0] || null;
      if (this.videoTrack) {
        const capabilities: any = this.videoTrack.getCapabilities?.() || {};
        this.isTorchSupported = !!capabilities.torch;
        return this.isTorchSupported;
      }
    } catch (_e) {
      this.isTorchSupported = false;
    }
    return false;
  }

  async setHardwareTorch(on: boolean): Promise<boolean> {
    if (!this.videoTrack) {
      await this.acquireCameraTorch();
    }
    if (this.videoTrack && this.isTorchSupported) {
      try {
        await (this.videoTrack as any).applyConstraints({
          advanced: [{ torch: on }]
        });
        return true;
      } catch (_e) {
        return false;
      }
    }
    return false;
  }

  releaseHardwareTorch() {
    this.stopWave();
    if (this.videoTrack) {
      try {
        (this.videoTrack as any).applyConstraints({ advanced: [{ torch: false }] });
      } catch (_e) {}
      this.videoTrack.stop();
      this.videoTrack = null;
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
    }
  }

  /**
   * Starts multimodal waving loop, invoking onFrame with the current optical intensity (0.0 to 1.0)
   * and emitter target ('BACK' | 'FRONT' | 'DUAL').
   */
  startWave(
    format: FlashlightWaveFormat,
    speedMultiplier: number = 1.0,
    onFrame: (intensity: number, target: 'BACK' | 'FRONT' | 'DUAL') => void
  ) {
    this.stopWave();
    const startTime = performance.now();

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
          // Smooth fluid sinusoidal optical wave
          const phase = (elapsed % 1800) / 1800; // 1.8s per wave cycle
          intensity = 0.15 + 0.85 * (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI - Math.PI / 2));
          emitter = phase > 0.5 ? 'DUAL' : 'BACK';
          break;
        }

        case 'STROBE_BURST': {
          // Rapid high-cadence strobe pulse wave
          const cycle = Math.floor(elapsed / 80) % 2;
          intensity = cycle === 0 ? 1.0 : 0.05;
          emitter = 'DUAL';
          break;
        }

        case 'DUAL_ALTERNATING': {
          // Waving back and forth between rear and front emitters
          const pos = (elapsed % 2400) / 2400; // 2.4s cycle
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
          // Morse code: 3 short, 3 long, 3 short (... --- ...)
          const t = Math.floor(elapsed / 150) % 24;
          const isDit = t === 0 || t === 2 || t === 4;
          const isDah = t === 8 || t === 9 || t === 11 || t === 12 || t === 14 || t === 15;
          const isDit2 = t === 18 || t === 20 || t === 22;
          intensity = isDit || isDah || isDit2 ? 1.0 : 0.0;
          emitter = isDah ? 'DUAL' : 'BACK';
          break;
        }

        case 'HEARTBEAT': {
          // Double systolic-diastolic cardiac pulse wave
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

      onFrame(intensity, emitter);

      // Periodically trigger physical hardware torch state if supported
      if (this.isTorchSupported && format === 'STROBE_BURST') {
        this.setHardwareTorch(intensity > 0.5);
      }

      this.waveAnimationId = requestAnimationFrame(loop);
    };

    this.waveAnimationId = requestAnimationFrame(loop);
  }

  stopWave() {
    if (this.waveAnimationId !== null) {
      cancelAnimationFrame(this.waveAnimationId);
      this.waveAnimationId = null;
    }
  }

  /**
   * Dispatches multimodal haptic wave pattern matching the optical wave.
   */
  triggerHapticWave(format: FlashlightWaveFormat) {
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try {
        switch (format) {
          case 'SINE_WAVE':
            navigator.vibrate([40, 60, 80, 60, 120]);
            break;
          case 'STROBE_BURST':
            navigator.vibrate([20, 20, 20, 20, 20, 20, 40]);
            break;
          case 'DUAL_ALTERNATING':
            navigator.vibrate([60, 80, 140, 80, 60]);
            break;
          case 'SOS_BEACON':
            navigator.vibrate([100, 100, 100, 100, 100, 200, 300, 100, 300, 100, 300, 200, 100, 100, 100]);
            break;
          case 'HEARTBEAT':
            navigator.vibrate([80, 80, 120]);
            break;
          default:
            navigator.vibrate(50);
            break;
        }
      } catch (_e) {}
    }
  }

  /**
   * Plays harmonic audio pitch wave synced to the light oscillation.
   */
  playWaveChime(format: FlashlightWaveFormat) {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      if (!this.audioContext || this.audioContext.state === 'closed') {
        this.audioContext = new AudioCtx();
      }
      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume();
      }
      const osc = this.audioContext.createOscillator();
      const gain = this.audioContext.createGain();
      osc.connect(gain);
      gain.connect(this.audioContext.destination);

      const now = this.audioContext.currentTime;
      osc.type = 'sine';

      if (format === 'SINE_WAVE') {
        osc.frequency.setValueAtTime(440, now);
        osc.frequency.exponentialRampToValueAtTime(880, now + 0.3);
        osc.frequency.exponentialRampToValueAtTime(440, now + 0.6);
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
        osc.start(now);
        osc.stop(now + 0.6);
      } else if (format === 'HEARTBEAT') {
        osc.frequency.setValueAtTime(120, now);
        osc.frequency.exponentialRampToValueAtTime(80, now + 0.15);
        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
        osc.start(now);
        osc.stop(now + 0.25);
      } else {
        osc.frequency.setValueAtTime(587.33, now); // D5
        gain.gain.setValueAtTime(0.06, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
        osc.start(now);
        osc.stop(now + 0.35);
      }
    } catch (_e) {}
  }
}

export const webTorchActuator = new WebTorchActuator();
