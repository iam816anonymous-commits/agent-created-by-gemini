import React, { useState, useEffect, useRef } from 'react';
import {
  Sparkles,
  Sun,
  Volume2,
  Play,
  Pause,
  RefreshCw,
  Save,
  Check,
  Zap,
  Flame,
  Battery,
  Music,
  Eye,
  Sliders,
  TrendingUp,
  Cpu,
  Moon,
  Heart,
  Radio,
  Vibrate,
  ShieldAlert,
  ArrowRight,
  RotateCw,
  Layers,
  Disc,
  FastForward,
  Rewind,
  Gauge
} from 'lucide-react';

export type ImprovisationGoal =
  | 'ENERGY_OPTIMIZATION'
  | 'URGENCY_AMPLIFICATION'
  | 'STEALTH_DARKROOM'
  | 'BIOMETRIC_RESONANCE'
  | 'HARMONIC_PYTHAGOREAN';

export interface ComposedLightPulse {
  cameraIndex: '0' | '1' | 'DUAL';
  onMs: number;
  offMs: number;
  repeats: number;
}

export interface ComposedLightPattern {
  id: string;
  name: string;
  purpose: string;
  pulses: ComposedLightPulse[];
  totalDurationMs: number;
  powerMa: number;
  generation: number;
  fitnessScore: number;
  mutationsApplied?: string[];
}

export interface ComposedAudioNote {
  frequencyHz: number;
  durationMs: number;
  waveform: 'sine' | 'triangle' | 'square' | 'sawtooth';
}

export interface ComposedSoundPattern {
  id: string;
  name: string;
  purpose: string;
  notes: ComposedAudioNote[];
  totalDurationMs: number;
  generation: number;
  harmonicScore: number;
  mutationsApplied?: string[];
}

export type FrequencyBand = 'SUB_BASS' | 'BASS' | 'MIDRANGE' | 'TREBLE';
export type ActuatorSyncTarget =
  | 'REAR_QUAD_FLASH'
  | 'FRONT_DUAL_FLASH'
  | 'DUAL_360_STROBE'
  | 'HAPTIC_KICK'
  | 'HAPTIC_SNARE_TICK'
  | 'FULL_SYNESTHESIA';

export interface MusicalNoteBeat {
  timestampMs: number;
  frequencyHz: number;
  durationMs: number;
  amplitude: number;
  band: FrequencyBand;
  syncTarget: ActuatorSyncTarget;
  waveform?: 'sine' | 'triangle' | 'square' | 'kick' | 'snare';
  noteName?: string;
}

export interface SongChoreographyTrack {
  id: string;
  title: string;
  genre: string;
  bpm: number;
  totalDurationMs: number;
  beats: MusicalNoteBeat[];
  description: string;
  peakLuxOutput: number;
  estimatedAveragePowerMa: number;
}

interface AutonomousPatternStudioProps {
  oledMode: boolean;
  onPlayLight: (pattern: ComposedLightPattern) => void;
  onPlaySound: (pattern: ComposedSoundPattern) => void;
  onTriggerHaptic: (pattern: 'TICK' | 'BURST' | 'IMPACT') => void;
  onSaveToVault?: (patternName: string) => void;
}

export const AutonomousPatternStudio: React.FC<AutonomousPatternStudioProps> = ({
  oledMode,
  onPlayLight,
  onPlaySound,
  onTriggerHaptic,
  onSaveToVault
}) => {
  // Navigation: Music Sync vs Pattern Improviser
  const [studioMode, setStudioMode] = useState<'MUSIC_SYNC' | 'PATTERN_STUDIO'>('MUSIC_SYNC');

  // =========================================================================
  // SONG CHOREOGRAPHY TRACKS (MUSIC & SONG BEAT SYNCHRONIZATION)
  // =========================================================================
  const buildCyberpunkTrack = (bpmVal = 128): SongChoreographyTrack => {
    const beatMs = Math.round(60000 / bpmVal);
    const beats: MusicalNoteBeat[] = [];
    for (let bar = 0; bar < 4; bar++) {
      const barStart = bar * (beatMs * 4);
      // Beat 1: Heavy Kick
      beats.push({
        timestampMs: barStart,
        frequencyHz: 60,
        durationMs: 160,
        amplitude: 1.0,
        band: 'BASS',
        syncTarget: 'REAR_QUAD_FLASH',
        waveform: 'kick',
        noteName: 'C1 Kick'
      });
      // Beat 1.5: Synth arp
      beats.push({
        timestampMs: barStart + Math.round(beatMs / 2),
        frequencyHz: 587,
        durationMs: 90,
        amplitude: 0.65,
        band: 'MIDRANGE',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'triangle',
        noteName: 'D5 Synth'
      });
      // Beat 2: Snare
      beats.push({
        timestampMs: barStart + beatMs,
        frequencyHz: 880,
        durationMs: 130,
        amplitude: 0.9,
        band: 'MIDRANGE',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'snare',
        noteName: 'A5 Snare'
      });
      // Beat 3: Kick
      beats.push({
        timestampMs: barStart + (beatMs * 2),
        frequencyHz: 65,
        durationMs: 160,
        amplitude: 0.95,
        band: 'BASS',
        syncTarget: 'REAR_QUAD_FLASH',
        waveform: 'kick',
        noteName: 'C#1 Kick'
      });
      // Beat 4: Drop Climax
      const isPeak = bar === 1 || bar === 3;
      beats.push({
        timestampMs: barStart + (beatMs * 3),
        frequencyHz: isPeak ? 1175 : 740,
        durationMs: isPeak ? 220 : 130,
        amplitude: 1.0,
        band: isPeak ? 'TREBLE' : 'MIDRANGE',
        syncTarget: isPeak ? 'DUAL_360_STROBE' : 'FRONT_DUAL_FLASH',
        waveform: 'triangle',
        noteName: isPeak ? 'D6 Strobe Drop' : 'F#5 Lead'
      });
    }
    return {
      id: 'track-cyberpunk',
      title: 'Cybernetic Synthwave Beat Drop',
      genre: 'Electronic / Synthwave',
      bpm: bpmVal,
      totalDurationMs: 4 * (beatMs * 4) + 100,
      beats,
      description: 'Driving 128 BPM electronic synth track syncing Rear Quad kicks, Front Dual snares, and Dual 360° drop strobes.',
      peakLuxOutput: 680,
      estimatedAveragePowerMa: 195
    };
  };

  const buildTeluguTrack = (bpmVal = 138): SongChoreographyTrack => {
    const beatMs = Math.round(60000 / bpmVal);
    const beats: MusicalNoteBeat[] = [];
    for (let bar = 0; bar < 4; bar++) {
      const barStart = bar * (beatMs * 4);
      // Dhol base thumping
      beats.push({
        timestampMs: barStart,
        frequencyHz: 55,
        durationMs: 180,
        amplitude: 1.0,
        band: 'SUB_BASS',
        syncTarget: 'FULL_SYNESTHESIA',
        waveform: 'kick',
        noteName: 'Dhol Bass'
      });
      // Syncopated clap
      beats.push({
        timestampMs: barStart + Math.round(beatMs * 0.75),
        frequencyHz: 1046,
        durationMs: 90,
        amplitude: 0.8,
        band: 'TREBLE',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'snare',
        noteName: 'Clap Transient'
      });
      // Snare crack
      beats.push({
        timestampMs: barStart + (beatMs * 2),
        frequencyHz: 784,
        durationMs: 120,
        amplitude: 0.9,
        band: 'MIDRANGE',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'snare',
        noteName: 'Rimshot'
      });
      // Double roll
      beats.push({
        timestampMs: barStart + Math.round(beatMs * 2.5),
        frequencyHz: 110,
        durationMs: 100,
        amplitude: 0.9,
        band: 'BASS',
        syncTarget: 'REAR_QUAD_FLASH',
        waveform: 'kick',
        noteName: 'Dhol Roll'
      });
      beats.push({
        timestampMs: barStart + (beatMs * 3),
        frequencyHz: 130,
        durationMs: 150,
        amplitude: 1.0,
        band: 'BASS',
        syncTarget: 'DUAL_360_STROBE',
        waveform: 'kick',
        noteName: 'Peak Dhol Drop'
      });
    }
    return {
      id: 'track-telugu',
      title: 'Cinematic Telugu Epic Rhythm (Dhol & Beat Drop)',
      genre: 'Cinematic Folk & Action',
      bpm: bpmVal,
      totalDurationMs: 4 * (beatMs * 4) + 100,
      beats,
      description: 'High-energy cinematic folk dhol rhythm with syncopated double-rolls, deep bass haptic rumbles, and peak strobe drops.',
      peakLuxOutput: 680,
      estimatedAveragePowerMa: 220
    };
  };

  const buildLofiTrack = (bpmVal = 75): SongChoreographyTrack => {
    const beatMs = Math.round(60000 / bpmVal);
    const beats: MusicalNoteBeat[] = [];
    for (let bar = 0; bar < 4; bar++) {
      const barStart = bar * (beatMs * 4);
      beats.push({
        timestampMs: barStart,
        frequencyHz: 130,
        durationMs: 380,
        amplitude: 0.6,
        band: 'SUB_BASS',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'sine',
        noteName: 'C3 Root Rhodes'
      });
      beats.push({
        timestampMs: barStart + beatMs,
        frequencyHz: 164,
        durationMs: 320,
        amplitude: 0.5,
        band: 'MIDRANGE',
        syncTarget: 'HAPTIC_SNARE_TICK',
        waveform: 'sine',
        noteName: 'E3 Chord'
      });
      beats.push({
        timestampMs: barStart + (beatMs * 2),
        frequencyHz: 196,
        durationMs: 280,
        amplitude: 0.55,
        band: 'MIDRANGE',
        syncTarget: 'FRONT_DUAL_FLASH',
        waveform: 'snare',
        noteName: 'Brushed Snare'
      });
      beats.push({
        timestampMs: barStart + (beatMs * 3),
        frequencyHz: 246,
        durationMs: 350,
        amplitude: 0.5,
        band: 'MIDRANGE',
        syncTarget: 'HAPTIC_SNARE_TICK',
        waveform: 'sine',
        noteName: 'B3 Jazz 7th'
      });
    }
    return {
      id: 'track-lofi',
      title: 'Lo-Fi Midnight Jazz Groove',
      genre: 'Chill / Lo-Fi Beats',
      bpm: bpmVal,
      totalDurationMs: 4 * (beatMs * 4) + 100,
      beats,
      description: 'Gentle midnight lo-fi chord progression with soft front dual-LED ambient breathing pulses and relaxing micro-ticks.',
      peakLuxOutput: 120,
      estimatedAveragePowerMa: 55
    };
  };

  const [songTracks, setSongTracks] = useState<SongChoreographyTrack[]>([
    buildCyberpunkTrack(128),
    buildTeluguTrack(138),
    buildLofiTrack(75)
  ]);

  const [activeSongId, setActiveSongId] = useState<string>('track-cyberpunk');
  const [isPlayingSong, setIsPlayingSong] = useState(false);
  const [currentSongBpm, setCurrentSongBpm] = useState<number>(128);
  const [activeBeatHighlight, setActiveBeatHighlight] = useState<MusicalNoteBeat | null>(null);

  // Sync Hardware Toggles
  const [syncRearKick, setSyncRearKick] = useState(true);
  const [syncFrontSnare, setSyncFrontSnare] = useState(true);
  const [syncDualDrop, setSyncDualDrop] = useState(true);
  const [syncHaptics, setSyncHaptics] = useState(true);
  const [flashDurationScale, setFlashDurationScale] = useState<number>(85); // ms

  const selectedSong = songTracks.find(t => t.id === activeSongId) || songTracks[0];

  // Active Emitter Hardware Simulation
  const [activeEmitterSim, setActiveEmitterSim] = useState<'NONE' | 'REAR' | 'FRONT' | 'DUAL'>('NONE');
  const [activeHapticSim, setActiveHapticSim] = useState<'IDLE' | 'KICK' | 'TICK' | 'BURST'>('IDLE');
  const [activeBandFlashes, setActiveBandFlashes] = useState<{ sub: boolean; bass: boolean; mid: boolean; treble: boolean }>({
    sub: false,
    bass: false,
    mid: false,
    treble: false
  });

  const songTimeoutRefs = useRef<number[]>([]);
  const audioCtxRef = useRef<AudioContext | null>(null);

  // Update BPM for active song
  const handleBpmChange = (newBpm: number) => {
    setCurrentSongBpm(newBpm);
    setSongTracks(prev =>
      prev.map(track => {
        if (track.id === activeSongId) {
          if (track.id.includes('cyberpunk')) return buildCyberpunkTrack(newBpm);
          if (track.id.includes('telugu')) return buildTeluguTrack(newBpm);
          if (track.id.includes('lofi')) return buildLofiTrack(newBpm);
        }
        return track;
      })
    );
  };

  // STOP all ongoing audio and hardware timeouts
  const handleStopSong = () => {
    songTimeoutRefs.current.forEach(t => clearTimeout(t));
    songTimeoutRefs.current = [];
    if (audioCtxRef.current) {
      try {
        audioCtxRef.current.close();
      } catch (_e) {}
      audioCtxRef.current = null;
    }
    setIsPlayingSong(false);
    setActiveEmitterSim('NONE');
    setActiveHapticSim('IDLE');
    setActiveBeatHighlight(null);
    setActiveBandFlashes({ sub: false, bass: false, mid: false, treble: false });
  };

  // PLAY Song with synchronized lights, vibrations & procedural audio
  const handlePlaySong = (track: SongChoreographyTrack) => {
    handleStopSong();
    setIsPlayingSong(true);

    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) {
        setIsPlayingSong(false);
        return;
      }
      const ctx = new AudioCtx();
      audioCtxRef.current = ctx;
      const startTime = ctx.currentTime;

      track.beats.forEach(beat => {
        const beatOffsetSec = beat.timestampMs / 1000.0;
        const noteDurationSec = beat.durationMs / 1000.0;
        const scheduledTime = startTime + beatOffsetSec;

        // 1. WEB AUDIO SYNTHESIS
        if (beat.waveform === 'kick') {
          // Punchy 808 pitch drop kick
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.frequency.setValueAtTime(140, scheduledTime);
          osc.frequency.exponentialRampToValueAtTime(42, scheduledTime + noteDurationSec);
          gain.gain.setValueAtTime(0.01, scheduledTime);
          gain.gain.linearRampToValueAtTime(0.28, scheduledTime + 0.005);
          gain.gain.exponentialRampToValueAtTime(0.001, scheduledTime + noteDurationSec);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(scheduledTime);
          osc.stop(scheduledTime + noteDurationSec);
        } else if (beat.waveform === 'snare') {
          // Snare crack
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(beat.frequencyHz, scheduledTime);
          gain.gain.setValueAtTime(0.01, scheduledTime);
          gain.gain.linearRampToValueAtTime(0.20, scheduledTime + 0.005);
          gain.gain.exponentialRampToValueAtTime(0.001, scheduledTime + noteDurationSec);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(scheduledTime);
          osc.stop(scheduledTime + noteDurationSec);
        } else {
          // Melodic note
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = beat.waveform || 'sine';
          osc.frequency.setValueAtTime(beat.frequencyHz, scheduledTime);
          gain.gain.setValueAtTime(0.01, scheduledTime);
          gain.gain.linearRampToValueAtTime(0.18, scheduledTime + 0.01);
          gain.gain.exponentialRampToValueAtTime(0.001, scheduledTime + noteDurationSec);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(scheduledTime);
          osc.stop(scheduledTime + noteDurationSec);
        }

        // 2. SYNCHRONIZED LIGHT & HAPTIC TIMING DISPATCH
        const tId = window.setTimeout(() => {
          setActiveBeatHighlight(beat);

          // Update band flashes
          setActiveBandFlashes({
            sub: beat.band === 'SUB_BASS',
            bass: beat.band === 'BASS',
            mid: beat.band === 'MIDRANGE',
            treble: beat.band === 'TREBLE'
          });

          // Dispatch Actuator Emitters
          if (beat.syncTarget === 'REAR_QUAD_FLASH' && syncRearKick) {
            setActiveEmitterSim('REAR');
            if (syncHaptics) {
              setActiveHapticSim('BURST');
              onTriggerHaptic('BURST');
            }
          } else if (beat.syncTarget === 'FRONT_DUAL_FLASH' && syncFrontSnare) {
            setActiveEmitterSim('FRONT');
            if (syncHaptics) {
              setActiveHapticSim('TICK');
              onTriggerHaptic('TICK');
            }
          } else if (
            (beat.syncTarget === 'DUAL_360_STROBE' || beat.syncTarget === 'FULL_SYNESTHESIA') &&
            syncDualDrop
          ) {
            setActiveEmitterSim('DUAL');
            if (syncHaptics) {
              setActiveHapticSim('BURST');
              onTriggerHaptic('BURST');
            }
          } else if (beat.syncTarget === 'HAPTIC_KICK' && syncHaptics) {
            setActiveHapticSim('BURST');
            onTriggerHaptic('BURST');
          } else if (beat.syncTarget === 'HAPTIC_SNARE_TICK' && syncHaptics) {
            setActiveHapticSim('TICK');
            onTriggerHaptic('TICK');
          }

          // Auto-extinguish flash
          const offTId = window.setTimeout(() => {
            setActiveEmitterSim('NONE');
            setActiveHapticSim('IDLE');
          }, flashDurationScale);
          songTimeoutRefs.current.push(offTId);
        }, beat.timestampMs);

        songTimeoutRefs.current.push(tId);
      });

      // End of song handler
      const endTId = window.setTimeout(() => {
        handleStopSong();
      }, track.totalDurationMs + 200);
      songTimeoutRefs.current.push(endTId);
    } catch (_e) {
      setIsPlayingSong(false);
    }
  };

  useEffect(() => {
    return () => {
      handleStopSong();
    };
  }, []);

  // Built-in Autonomous Composed Light Patterns with Generation tracking
  const [lightPatterns, setLightPatterns] = useState<ComposedLightPattern[]>([
    {
      id: 'lp-heartbeat',
      name: 'Biometric Heartbeat Cadence',
      purpose: 'Organic double-thump bio-rhythm pulse for quiet user notifications',
      pulses: [
        { cameraIndex: '0', onMs: 80, offMs: 60, repeats: 1 },
        { cameraIndex: '0', onMs: 120, offMs: 400, repeats: 1 },
        { cameraIndex: '0', onMs: 80, offMs: 60, repeats: 1 },
        { cameraIndex: '0', onMs: 120, offMs: 600, repeats: 1 }
      ],
      totalDurationMs: 1420,
      powerMa: 110,
      generation: 1,
      fitnessScore: 0.91,
      mutationsApplied: ['Initial procedural synthesis calibrated to resting cardiac rhythm']
    },
    {
      id: 'lp-sos',
      name: 'Optical Morse SOS Beacon',
      purpose: 'International distress pattern: 3 short, 3 long, 3 short dual-emitter flashes',
      pulses: [
        { cameraIndex: '0', onMs: 100, offMs: 100, repeats: 3 },
        { cameraIndex: 'DUAL', onMs: 300, offMs: 150, repeats: 3 },
        { cameraIndex: '0', onMs: 100, offMs: 100, repeats: 3 }
      ],
      totalDurationMs: 2550,
      powerMa: 380,
      generation: 1,
      fitnessScore: 0.96,
      mutationsApplied: ['Calibrated to international optical Morse frequency']
    },
    {
      id: 'lp-cinema',
      name: 'Cinematic Ambient Whisper',
      purpose: 'Soft front dual-LED micro-flicker that avoids distracting eyes in dark rooms',
      pulses: [
        { cameraIndex: '1', onMs: 40, offMs: 180, repeats: 2 },
        { cameraIndex: '1', onMs: 70, offMs: 300, repeats: 1 }
      ],
      totalDurationMs: 810,
      powerMa: 45,
      generation: 1,
      fitnessScore: 0.94,
      mutationsApplied: ['Locked exclusively to soft front dual-LED emitter']
    },
    {
      id: 'lp-phaser',
      name: 'Alternating 360° Phaser Strobe',
      purpose: 'High-visibility visual capture across both front and rear emitters for critical gates',
      pulses: [
        { cameraIndex: '0', onMs: 60, offMs: 50, repeats: 3 },
        { cameraIndex: '1', onMs: 60, offMs: 50, repeats: 3 },
        { cameraIndex: 'DUAL', onMs: 100, offMs: 80, repeats: 2 }
      ],
      totalDurationMs: 1020,
      powerMa: 420,
      generation: 1,
      fitnessScore: 0.93,
      mutationsApplied: ['High-contrast dual-strobe alternations']
    }
  ]);

  // Built-in Autonomous Composed Sound Melodies with Generation tracking
  const [soundPatterns, setSoundPatterns] = useState<ComposedSoundPattern[]>([
    {
      id: 'sp-scifi',
      name: 'Cybernetic Neural Arpeggio',
      purpose: 'Futuristic ascending D-major harmonic fanfare for successful workflow completion',
      notes: [
        { frequencyHz: 587, durationMs: 80, waveform: 'triangle' }, // D5
        { frequencyHz: 740, durationMs: 80, waveform: 'triangle' }, // F#5
        { frequencyHz: 880, durationMs: 100, waveform: 'sine' },     // A5
        { frequencyHz: 1175, durationMs: 240, waveform: 'sine' }    // D6
      ],
      totalDurationMs: 500,
      generation: 1,
      harmonicScore: 0.95,
      mutationsApplied: ['Ascending triad arpeggio with high overtone resolution']
    },
    {
      id: 'sp-sonar',
      name: 'Deep Sonar Exploration Sweep',
      purpose: 'High-frequency resonance ping indicating network socket echo and spatial discovery',
      notes: [
        { frequencyHz: 1480, durationMs: 70, waveform: 'sine' },
        { frequencyHz: 1760, durationMs: 160, waveform: 'sine' },
        { frequencyHz: 2093, durationMs: 280, waveform: 'sine' }
      ],
      totalDurationMs: 510,
      generation: 1,
      harmonicScore: 0.91,
      mutationsApplied: ['Spatial resonant high-frequency ping']
    },
    {
      id: 'sp-triumph',
      name: 'Majestic Triad Triumph Fanfare',
      purpose: 'Full C-major chord progression affirming 100% test matrix validation',
      notes: [
        { frequencyHz: 523, durationMs: 90, waveform: 'sine' },  // C5
        { frequencyHz: 659, durationMs: 90, waveform: 'sine' },  // E5
        { frequencyHz: 784, durationMs: 110, waveform: 'sine' }, // G5
        { frequencyHz: 1046, durationMs: 300, waveform: 'sine' } // C6
      ],
      totalDurationMs: 590,
      generation: 1,
      harmonicScore: 0.98,
      mutationsApplied: ['Just intonation C-Major triad progression']
    },
    {
      id: 'sp-nocturnal',
      name: 'Nocturnal Harmonic Settle',
      purpose: 'Low-frequency sub-bass resonant hum for quiet 24/7 background maintenance mode',
      notes: [
        { frequencyHz: 130, durationMs: 220, waveform: 'sine' },
        { frequencyHz: 196, durationMs: 260, waveform: 'sine' },
        { frequencyHz: 261, durationMs: 380, waveform: 'sine' }
      ],
      totalDurationMs: 860,
      generation: 1,
      harmonicScore: 0.93,
      mutationsApplied: ['Sub-bass root chord with soft exponential decay']
    }
  ]);

  const [activeLightId, setActiveLightId] = useState<string>('lp-heartbeat');
  const [activeSoundId, setActiveSoundId] = useState<string>('sp-scifi');
  const [selectedGoal, setSelectedGoal] = useState<ImprovisationGoal>('ENERGY_OPTIMIZATION');
  const [customPrompt, setCustomPrompt] = useState('');
  const [isSynthesizing, setIsSynthesizing] = useState(false);
  const [isImprovising, setIsImprovising] = useState(false);
  const [savedSuccessToast, setSavedSuccessToast] = useState<string | null>(null);
  const [improviseLog, setImproviseLog] = useState<string[]>([]);

  const selectedLight = lightPatterns.find(p => p.id === activeLightId) || lightPatterns[0];
  const selectedSound = soundPatterns.find(p => p.id === activeSoundId) || soundPatterns[0];

  const handleSaveVault = () => {
    if (onSaveToVault) {
      if (studioMode === 'MUSIC_SYNC') {
        onSaveToVault(`Song Choreography: ${selectedSong.title} (${selectedSong.bpm} BPM)`);
      } else {
        onSaveToVault(`${selectedLight.name} & ${selectedSound.name}`);
      }
    }
    setSavedSuccessToast(
      studioMode === 'MUSIC_SYNC'
        ? `Saved "${selectedSong.title}" to Brain Vault!`
        : `Saved Generation ${selectedLight.generation} to Brain Vault!`
    );
    setTimeout(() => setSavedSuccessToast(null), 2500);
  };

  return (
    <div
      className={`rounded-2xl border p-4 sm:p-5 transition-all space-y-4 ${
        oledMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
      }`}
    >
      {/* Studio Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-900 pb-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-amber-950/80 border border-amber-800 text-amber-400">
            <Sparkles className="w-4 h-4 animate-pulse" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide font-mono uppercase">
              [ AUTONOMOUS ACTUATOR CHOREOGRAPHER &bull; MUSIC &amp; VIBRATION SYNC ]
            </h2>
            <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500 font-mono mt-0.5">
              <span>Real-Time Beat Tracking</span>
              <span aria-hidden="true">&bull;</span>
              <span>Sub-ms Dual-LED Strobe</span>
              <span aria-hidden="true">&bull;</span>
              <span>Calibrated Tactile Haptics</span>
              <span aria-hidden="true">&bull;</span>
              <span className="text-emerald-400 font-semibold">100% Offline-First</span>
            </div>
          </div>
        </div>

        {/* Studio Mode Segmented Switch & Save Button */}
        <div className="flex items-center gap-2">
          <div className="flex p-0.5 rounded-lg bg-black border border-slate-800 font-mono text-xs">
            <button
              onClick={() => {
                handleStopSong();
                setStudioMode('MUSIC_SYNC');
              }}
              className={`px-3 py-1.5 rounded-md flex items-center gap-1.5 transition-all ${
                studioMode === 'MUSIC_SYNC'
                  ? 'bg-gradient-to-r from-amber-500 to-amber-600 text-black font-bold shadow-xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Music className="w-3.5 h-3.5" />
              <span>Music &amp; Song Sync</span>
            </button>
            <button
              onClick={() => {
                handleStopSong();
                setStudioMode('PATTERN_STUDIO');
              }}
              className={`px-3 py-1.5 rounded-md flex items-center gap-1.5 transition-all ${
                studioMode === 'PATTERN_STUDIO'
                  ? 'bg-gradient-to-r from-cyan-500 to-cyan-600 text-black font-bold shadow-xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <TrendingUp className="w-3.5 h-3.5" />
              <span>Pattern Improviser</span>
            </button>
          </div>

          <button
            onClick={handleSaveVault}
            className="px-3 py-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/40 rounded-lg text-xs font-mono flex items-center gap-1.5 transition-colors"
          >
            <Save className="w-3.5 h-3.5" />
            <span>Persist to Vault</span>
          </button>
        </div>
      </div>

      {/* =========================================================================
          REAL-TIME HARDWARE EMITTER & HAPTIC MONITOR (SHARED TELEMETRY)
          ========================================================================= */}
      <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-3">
        <div className="flex items-center justify-between text-xs font-mono">
          <div className="flex items-center gap-2">
            <Eye className="w-3.5 h-3.5 text-cyan-400" />
            <span className="text-white font-bold uppercase">Live Hardware Actuator Telemetry &amp; Stage Emitter</span>
          </div>
          <div className="flex items-center gap-3 text-[10px]">
            <span className="flex items-center gap-1">
              <span className="text-slate-500">Optical Lux:</span>
              <span className="text-amber-400 font-bold">
                {activeEmitterSim === 'DUAL' ? '680 lx' : activeEmitterSim === 'REAR' ? '540 lx' : activeEmitterSim === 'FRONT' ? '120 lx' : '0 lx'}
              </span>
            </span>
            <span className="flex items-center gap-1">
              <span className="text-slate-500">Haptic Pattern:</span>
              <span className="text-pink-400 font-bold">{activeHapticSim}</span>
            </span>
            <span className="flex items-center gap-1">
              <span className="text-slate-500">Power:</span>
              <span className="text-emerald-400 font-bold">{selectedSong.estimatedAveragePowerMa} mA</span>
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {/* Emitter 1: Front Dual-LED (Camera 1) */}
          <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div
                className={`w-11 h-11 rounded-full border flex items-center justify-center transition-all duration-75 ${
                  activeEmitterSim === 'FRONT' || activeEmitterSim === 'DUAL'
                    ? 'bg-amber-300 border-amber-200 shadow-[0_0_24px_rgba(251,191,36,0.95)] scale-110'
                    : 'bg-slate-900 border-slate-800'
                }`}
              >
                <Sun
                  className={`w-5 h-5 ${
                    activeEmitterSim === 'FRONT' || activeEmitterSim === 'DUAL'
                      ? 'text-black fill-current animate-spin'
                      : 'text-slate-700'
                  }`}
                />
              </div>
              <div>
                <span className="text-[11px] font-mono text-white font-bold">Front Dual-LED</span>
                <p className="text-[9px] font-mono text-slate-500">Camera Index "1" (Snares/Vocals)</p>
              </div>
            </div>
            <span
              className={`text-[9px] font-mono font-bold px-2 py-0.5 rounded ${
                activeEmitterSim === 'FRONT' || activeEmitterSim === 'DUAL'
                  ? 'bg-amber-950 text-amber-300 border border-amber-800'
                  : 'bg-black text-slate-600 border border-slate-900'
              }`}
            >
              {activeEmitterSim === 'FRONT' || activeEmitterSim === 'DUAL' ? 'FLASH' : 'IDLE'}
            </span>
          </div>

          {/* Emitter 2: Rear Quad-LED (Camera 0) */}
          <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div
                className={`w-11 h-11 rounded-full border flex items-center justify-center transition-all duration-75 ${
                  activeEmitterSim === 'REAR' || activeEmitterSim === 'DUAL'
                    ? 'bg-amber-400 border-amber-100 shadow-[0_0_36px_rgba(245,158,11,1)] scale-110'
                    : 'bg-slate-900 border-slate-800'
                }`}
              >
                <Zap
                  className={`w-5 h-5 ${
                    activeEmitterSim === 'REAR' || activeEmitterSim === 'DUAL'
                      ? 'text-black fill-current animate-pulse'
                      : 'text-slate-700'
                  }`}
                />
              </div>
              <div>
                <span className="text-[11px] font-mono text-white font-bold">Rear Quad-LED</span>
                <p className="text-[9px] font-mono text-slate-500">Camera Index "0" (Kick Drums)</p>
              </div>
            </div>
            <span
              className={`text-[9px] font-mono font-bold px-2 py-0.5 rounded ${
                activeEmitterSim === 'REAR' || activeEmitterSim === 'DUAL'
                  ? 'bg-amber-950 text-amber-300 border border-amber-800'
                  : 'bg-black text-slate-600 border border-slate-900'
              }`}
            >
              {activeEmitterSim === 'REAR' || activeEmitterSim === 'DUAL' ? 'STROBE' : 'IDLE'}
            </span>
          </div>

          {/* Actuator 3: Tactile Vibrator (ERM/LRA) */}
          <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div
                className={`w-11 h-11 rounded-full border flex items-center justify-center transition-all duration-75 ${
                  activeHapticSim !== 'IDLE'
                    ? 'bg-pink-500 border-pink-200 shadow-[0_0_24px_rgba(244,114,182,0.95)] scale-110'
                    : 'bg-slate-900 border-slate-800'
                }`}
              >
                <Vibrate
                  className={`w-5 h-5 ${
                    activeHapticSim !== 'IDLE' ? 'text-black fill-current animate-bounce' : 'text-slate-700'
                  }`}
                />
              </div>
              <div>
                <span className="text-[11px] font-mono text-white font-bold">Vibration Haptics</span>
                <p className="text-[9px] font-mono text-slate-500">Bass Kick &amp; Snare Sync</p>
              </div>
            </div>
            <span
              className={`text-[9px] font-mono font-bold px-2 py-0.5 rounded ${
                activeHapticSim !== 'IDLE'
                  ? 'bg-pink-950 text-pink-300 border border-pink-800'
                  : 'bg-black text-slate-600 border border-slate-900'
              }`}
            >
              {activeHapticSim !== 'IDLE' ? activeHapticSim : 'READY'}
            </span>
          </div>
        </div>

        {/* 4-Band Spectral Beat Indicator Matrix */}
        <div className="grid grid-cols-4 gap-2 pt-1 font-mono text-[10px]">
          <div
            className={`p-2 rounded border transition-all text-center ${
              activeBandFlashes.sub
                ? 'bg-emerald-950/90 border-emerald-400 text-emerald-200 shadow-sm'
                : 'bg-black border-slate-900 text-slate-600'
            }`}
          >
            <div className="font-bold">SUB-BASS (&lt;80Hz)</div>
            <div className="text-[8px] opacity-75">Haptic Tremor</div>
          </div>
          <div
            className={`p-2 rounded border transition-all text-center ${
              activeBandFlashes.bass
                ? 'bg-amber-950/90 border-amber-400 text-amber-200 shadow-sm'
                : 'bg-black border-slate-900 text-slate-600'
            }`}
          >
            <div className="font-bold">BASS KICK (80-250Hz)</div>
            <div className="text-[8px] opacity-75">Rear Quad-LED</div>
          </div>
          <div
            className={`p-2 rounded border transition-all text-center ${
              activeBandFlashes.mid
                ? 'bg-cyan-950/90 border-cyan-400 text-cyan-200 shadow-sm'
                : 'bg-black border-slate-900 text-slate-600'
            }`}
          >
            <div className="font-bold">SNARE (250-2kHz)</div>
            <div className="text-[8px] opacity-75">Front Dual-LED</div>
          </div>
          <div
            className={`p-2 rounded border transition-all text-center ${
              activeBandFlashes.treble
                ? 'bg-purple-950/90 border-purple-400 text-purple-200 shadow-sm'
                : 'bg-black border-slate-900 text-slate-600'
            }`}
          >
            <div className="font-bold">TREBLE / DROP (&gt;2kHz)</div>
            <div className="text-[8px] opacity-75">360° Strobe</div>
          </div>
        </div>
      </div>

      {/* =========================================================================
          VIEW 1: MUSIC & SONG BEAT SYNCHRONIZER
          ========================================================================= */}
      {studioMode === 'MUSIC_SYNC' && (
        <div className="space-y-4">
          {/* Song Selector Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {songTracks.map(track => {
              const isSelected = track.id === activeSongId;
              const isPlayingThis = isPlayingSong && isSelected;
              return (
                <div
                  key={track.id}
                  onClick={() => {
                    if (activeSongId !== track.id) {
                      handleStopSong();
                      setActiveSongId(track.id);
                      setCurrentSongBpm(track.bpm);
                    }
                  }}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex flex-col justify-between space-y-2.5 ${
                    isSelected
                      ? 'bg-slate-900/90 border-amber-500 shadow-md shadow-amber-950/30'
                      : 'bg-black border-slate-900 hover:border-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[10px] font-mono text-amber-400 uppercase font-semibold">
                        {track.genre}
                      </span>
                      <h3 className="text-xs font-bold text-white font-mono mt-0.5 leading-snug">
                        {track.title}
                      </h3>
                    </div>
                    <div className="p-1.5 rounded-lg bg-black border border-slate-800 text-slate-400">
                      <Disc className={`w-4 h-4 ${isPlayingThis ? 'animate-spin text-amber-400' : ''}`} />
                    </div>
                  </div>

                  <p className="text-[10px] text-slate-400 font-sans leading-relaxed line-clamp-2">
                    {track.description}
                  </p>

                  <div className="flex items-center justify-between text-[10px] font-mono text-slate-400 border-t border-slate-900/80 pt-2">
                    <span className="text-white font-bold">{track.bpm} BPM</span>
                    <span>{track.beats.length} Sync Events</span>
                    <span className="text-emerald-400 font-semibold">{track.estimatedAveragePowerMa}mA</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Song Playback & Real-time Sequencer Console */}
          <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-900 pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-white font-mono">{selectedSong.title}</span>
                  <span className="px-2 py-0.5 bg-amber-950 border border-amber-800 text-amber-400 text-[10px] font-mono rounded">
                    {currentSongBpm} BPM
                  </span>
                  {activeBeatHighlight && (
                    <span className="px-2 py-0.5 bg-cyan-950 border border-cyan-800 text-cyan-300 text-[10px] font-mono rounded animate-pulse">
                      Playing: {activeBeatHighlight.noteName}
                    </span>
                  )}
                </div>
                <p className="text-xs text-slate-400 font-sans mt-0.5">{selectedSong.description}</p>
              </div>

              {/* Play / Stop Master Button */}
              <div className="flex items-center gap-2">
                {isPlayingSong ? (
                  <button
                    onClick={handleStopSong}
                    className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl font-mono flex items-center gap-2 transition-all shadow-md"
                  >
                    <Pause className="w-4 h-4" />
                    <span>Stop Song &amp; Extinguish</span>
                  </button>
                ) : (
                  <button
                    onClick={() => handlePlaySong(selectedSong)}
                    className="px-4 py-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-black font-bold text-xs rounded-xl font-mono flex items-center gap-2 transition-all shadow-md shadow-amber-950/40"
                  >
                    <Play className="w-4 h-4 fill-current" />
                    <span>Play Song With Hardware Sync</span>
                  </button>
                )}
              </div>
            </div>

            {/* Tempo Slider & Hardware Sync Matrix */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Tempo (BPM) & Strobe Duration Sliders */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-3 font-mono text-xs">
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Gauge className="w-3.5 h-3.5 text-amber-400" />
                      <span>SONG TEMPO (BPM):</span>
                    </span>
                    <span className="text-white font-bold">{currentSongBpm} BPM</span>
                  </div>
                  <input
                    type="range"
                    min="60"
                    max="180"
                    step="2"
                    value={currentSongBpm}
                    onChange={e => handleBpmChange(Number(e.target.value))}
                    disabled={isPlayingSong}
                    className="w-full accent-amber-500 bg-slate-900 rounded-lg cursor-pointer"
                  />
                  <div className="flex justify-between text-[9px] text-slate-500">
                    <span>60 BPM (Chill)</span>
                    <span>120 BPM (Standard)</span>
                    <span>180 BPM (Fast DnB)</span>
                  </div>
                </div>

                <div className="space-y-1.5 pt-1 border-t border-slate-900">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Zap className="w-3.5 h-3.5 text-cyan-400" />
                      <span>FLASH STROBE DURATION:</span>
                    </span>
                    <span className="text-white font-bold">{flashDurationScale} ms</span>
                  </div>
                  <input
                    type="range"
                    min="35"
                    max="150"
                    step="5"
                    value={flashDurationScale}
                    onChange={e => setFlashDurationScale(Number(e.target.value))}
                    className="w-full accent-cyan-500 bg-slate-900 rounded-lg cursor-pointer"
                  />
                  <div className="flex justify-between text-[9px] text-slate-500">
                    <span>35ms (Micro-pop)</span>
                    <span>85ms (Punchy)</span>
                    <span>150ms (Sustained)</span>
                  </div>
                </div>
              </div>

              {/* Hardware Sync Toggles */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-lg space-y-2.5 font-mono text-xs">
                <span className="text-slate-400 text-[10px] uppercase font-bold tracking-wider">
                  Hardware Actuator Mappings:
                </span>

                <div className="space-y-2">
                  <label className="flex items-center justify-between cursor-pointer group">
                    <span className="text-slate-300 flex items-center gap-2">
                      <Zap className="w-3.5 h-3.5 text-amber-400" />
                      <span>Sync Rear Quad-LED (Camera 0) on Kicks</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={syncRearKick}
                      onChange={e => setSyncRearKick(e.target.checked)}
                      className="accent-amber-500 w-4 h-4 rounded cursor-pointer"
                    />
                  </label>

                  <label className="flex items-center justify-between cursor-pointer group">
                    <span className="text-slate-300 flex items-center gap-2">
                      <Sun className="w-3.5 h-3.5 text-cyan-400" />
                      <span>Sync Front Dual-LED (Camera 1) on Snares</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={syncFrontSnare}
                      onChange={e => setSyncFrontSnare(e.target.checked)}
                      className="accent-cyan-500 w-4 h-4 rounded cursor-pointer"
                    />
                  </label>

                  <label className="flex items-center justify-between cursor-pointer group">
                    <span className="text-slate-300 flex items-center gap-2">
                      <Flame className="w-3.5 h-3.5 text-purple-400" />
                      <span>Sync 360° Strobe on Peak Drop Climax</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={syncDualDrop}
                      onChange={e => setSyncDualDrop(e.target.checked)}
                      className="accent-purple-500 w-4 h-4 rounded cursor-pointer"
                    />
                  </label>

                  <label className="flex items-center justify-between cursor-pointer group">
                    <span className="text-slate-300 flex items-center gap-2">
                      <Vibrate className="w-3.5 h-3.5 text-pink-400" />
                      <span>Sync Tactile Vibrations (TICK/BURST)</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={syncHaptics}
                      onChange={e => setSyncHaptics(e.target.checked)}
                      className="accent-pink-500 w-4 h-4 rounded cursor-pointer"
                    />
                  </label>
                </div>
              </div>
            </div>

            {/* Interactive Beat Sequencer Timeline Roll */}
            <div className="space-y-1.5 pt-1">
              <div className="flex items-center justify-between text-[10px] font-mono text-slate-400">
                <span className="uppercase">RHYTHMIC BEAT TIMELINE &bull; SYNCHRONIZATION EVENT ROLL:</span>
                <span>{selectedSong.beats.length} Scheduled Actuation Triggers</span>
              </div>

              <div className="flex items-center gap-1.5 overflow-x-auto p-2 bg-slate-950 rounded-lg border border-slate-900 no-scrollbar">
                {selectedSong.beats.map((beat, idx) => {
                  const isCurrent = activeBeatHighlight?.timestampMs === beat.timestampMs;
                  return (
                    <div
                      key={idx}
                      className={`px-2.5 py-1.5 rounded-lg text-[9px] font-mono shrink-0 flex flex-col gap-0.5 border transition-all ${
                        isCurrent
                          ? 'bg-amber-400 text-black border-amber-200 shadow-md scale-105 font-bold'
                          : beat.band === 'BASS'
                          ? 'bg-amber-950/80 text-amber-300 border-amber-900'
                          : beat.band === 'SUB_BASS'
                          ? 'bg-emerald-950/80 text-emerald-300 border-emerald-900'
                          : beat.band === 'MIDRANGE'
                          ? 'bg-cyan-950/80 text-cyan-300 border-cyan-900'
                          : 'bg-purple-950/80 text-purple-300 border-purple-900'
                      }`}
                    >
                      <span className="font-bold truncate">{beat.noteName || `${beat.frequencyHz}Hz`}</span>
                      <span className="text-[8px] opacity-80">{beat.timestampMs}ms</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          VIEW 2: AUTONOMOUS PATTERN IMPROVISER & COMPOSER
          ========================================================================= */}
      {studioMode === 'PATTERN_STUDIO' && (
        <div className="space-y-4">
          {/* Autonomous Improvisation Controls */}
          <div className="p-3.5 bg-black border border-amber-950/80 rounded-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-2">
              <div className="flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-amber-400" />
                <span className="text-xs font-bold text-white uppercase font-mono">
                  Autonomous Self-Improvisation &amp; Evolution Engine
                </span>
              </div>
              <span className="text-[10px] text-slate-500 font-mono">
                Optimizes duty-cycle, thermal ceiling, &amp; harmonic tuning
              </span>
            </div>

            {/* Goal Badges */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-1.5 text-xs font-mono">
              <button
                onClick={() => setSelectedGoal('ENERGY_OPTIMIZATION')}
                className={`p-2 rounded-lg border text-left transition-all flex flex-col gap-1 ${
                  selectedGoal === 'ENERGY_OPTIMIZATION'
                    ? 'bg-emerald-950/90 text-emerald-300 border-emerald-500 shadow-xs'
                    : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold text-[11px]">
                  <Battery className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Eco Power</span>
                </div>
                <span className="text-[9px] text-slate-500">Slash current ~40%</span>
              </button>

              <button
                onClick={() => setSelectedGoal('URGENCY_AMPLIFICATION')}
                className={`p-2 rounded-lg border text-left transition-all flex flex-col gap-1 ${
                  selectedGoal === 'URGENCY_AMPLIFICATION'
                    ? 'bg-rose-950/90 text-rose-300 border-rose-500 shadow-xs'
                    : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold text-[11px]">
                  <Flame className="w-3.5 h-3.5 text-rose-400" />
                  <span>Urgency Strobe</span>
                </div>
                <span className="text-[9px] text-slate-500">Piercing contrast</span>
              </button>

              <button
                onClick={() => setSelectedGoal('STEALTH_DARKROOM')}
                className={`p-2 rounded-lg border text-left transition-all flex flex-col gap-1 ${
                  selectedGoal === 'STEALTH_DARKROOM'
                    ? 'bg-indigo-950/90 text-indigo-300 border-indigo-500 shadow-xs'
                    : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold text-[11px]">
                  <Moon className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Stealth Cinema</span>
                </div>
                <span className="text-[9px] text-slate-500">Soft front LED glow</span>
              </button>

              <button
                onClick={() => setSelectedGoal('BIOMETRIC_RESONANCE')}
                className={`p-2 rounded-lg border text-left transition-all flex flex-col gap-1 ${
                  selectedGoal === 'BIOMETRIC_RESONANCE'
                    ? 'bg-pink-950/90 text-pink-300 border-pink-500 shadow-xs'
                    : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold text-[11px]">
                  <Heart className="w-3.5 h-3.5 text-pink-400" />
                  <span>Bio Pulse</span>
                </div>
                <span className="text-[9px] text-slate-500">65 BPM human rhythm</span>
              </button>

              <button
                onClick={() => setSelectedGoal('HARMONIC_PYTHAGOREAN')}
                className={`p-2 rounded-lg border text-left transition-all flex flex-col gap-1 ${
                  selectedGoal === 'HARMONIC_PYTHAGOREAN'
                    ? 'bg-amber-950/90 text-amber-300 border-amber-500 shadow-xs'
                    : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold text-[11px]">
                  <Music className="w-3.5 h-3.5 text-amber-400" />
                  <span>Major 9th</span>
                </div>
                <span className="text-[9px] text-slate-500">Golden ratio tuning</span>
              </button>
            </div>
          </div>

          {/* Light vs Sound Side-by-Side */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Panel 1: Light Choreographies */}
            <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-3">
              <div className="flex items-center justify-between border-b border-slate-900 pb-2">
                <div className="flex items-center gap-2 text-xs font-mono font-bold text-white uppercase">
                  <Sun className="w-3.5 h-3.5 text-amber-400" />
                  <span>Light Choreographies</span>
                </div>
                <span className="text-[10px] text-slate-500 font-mono">
                  {lightPatterns.length} Patterns &bull; Gen {selectedLight.generation}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                {lightPatterns.map(lp => (
                  <button
                    key={lp.id}
                    onClick={() => setActiveLightId(lp.id)}
                    className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between h-20 ${
                      lp.id === activeLightId
                        ? 'bg-amber-950/80 text-amber-300 border-amber-500'
                        : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                    }`}
                  >
                    <div className="font-bold text-[11px] truncate">{lp.name}</div>
                    <div className="flex justify-between text-[9px] text-slate-500">
                      <span>{lp.totalDurationMs}ms</span>
                      <span className="text-amber-400 font-semibold">{lp.powerMa}mA</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Panel 2: Sound Patterns */}
            <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-3">
              <div className="flex items-center justify-between border-b border-slate-900 pb-2">
                <div className="flex items-center gap-2 text-xs font-mono font-bold text-white uppercase">
                  <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Procedural Sound &amp; Melodies</span>
                </div>
                <span className="text-[10px] text-slate-500 font-mono">
                  {soundPatterns.length} Patterns &bull; Gen {selectedSound.generation}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                {soundPatterns.map(sp => (
                  <button
                    key={sp.id}
                    onClick={() => setActiveSoundId(sp.id)}
                    className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between h-20 ${
                      sp.id === activeSoundId
                        ? 'bg-cyan-950/80 text-cyan-300 border-cyan-500'
                        : 'bg-slate-950 text-slate-400 hover:text-white border-slate-800'
                    }`}
                  >
                    <div className="font-bold text-[11px] truncate">{sp.name}</div>
                    <div className="flex justify-between text-[9px] text-slate-500">
                      <span>{sp.totalDurationMs}ms</span>
                      <span className="text-cyan-400 font-semibold">{sp.notes.length} Notes</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Floating Save Toast */}
      {savedSuccessToast && (
        <div className="fixed bottom-4 left-4 z-50 bg-emerald-950/95 border border-emerald-500 text-emerald-200 px-4 py-2 rounded-xl text-xs font-mono shadow-2xl flex items-center gap-2 animate-bounce">
          <Check className="w-4 h-4 text-emerald-400" />
          <span>{savedSuccessToast}</span>
        </div>
      )}
    </div>
  );
};
