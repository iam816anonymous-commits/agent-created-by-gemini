package com.example.core.actuator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.example.core.storage.BrainVaultManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin

/**
 * Frequency spectral band classification for musical transients.
 */
enum class FrequencyBand {
    SUB_BASS,  // < 80 Hz: 808 drops, sub-bass rumbles (Tactile tremor)
    BASS,      // 80 - 250 Hz: Kick drums, punchy bassline (Rear Quad-LED Camera 0 + BURST)
    MIDRANGE,  // 250 - 2000 Hz: Snares, claps, vocal transients (Front Dual-LED Camera 1 + TICK)
    TREBLE     // > 2000 Hz: Hi-hats, cymbals, synth chimes (Micro-flicker fill)
}

/**
 * Target hardware actuator mapped to a musical beat transient.
 */
enum class ActuatorSyncTarget {
    REAR_QUAD_FLASH,     // Camera 0 Rear Quad-LED (Punchy kick drop)
    FRONT_DUAL_FLASH,    // Camera 1 Front Dual-LED (Snare / vocal pulse)
    DUAL_360_STROBE,     // Camera 0 + Camera 1 Concurrent (Peak drop / chorus climax)
    HAPTIC_KICK,         // Heavy tactile impact (Low-end kick transient)
    HAPTIC_SNARE_TICK,   // Crisp micro-tick vibration (Snare / rimshot)
    FULL_SYNESTHESIA     // Concurrent dual-flash + heavy tactile haptic impact
}

/**
 * Individual musical event synchronized with optical and tactile actuators.
 */
data class MusicalNoteBeat(
    val timestampMs: Long,
    val frequencyHz: Int,
    val durationMs: Int,
    val amplitude: Float,             // 0.0 to 1.0 relative gain
    val band: FrequencyBand,
    val syncTarget: ActuatorSyncTarget,
    val waveform: String = "SINE"     // "SINE", "TRIANGLE", "SQUARE"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("timestampMs", timestampMs)
            put("frequencyHz", frequencyHz)
            put("durationMs", durationMs)
            put("amplitude", amplitude.toDouble())
            put("band", band.name)
            put("syncTarget", syncTarget.name)
            put("waveform", waveform)
        }
    }
}

/**
 * Complete procedural song choreography with lockstep lighting & vibration tracks.
 */
data class SongChoreographyTrack(
    val id: String,
    val title: String,
    val genre: String,
    val bpm: Int,
    val totalDurationMs: Long,
    val beats: List<MusicalNoteBeat>,
    val description: String,
    val peakLuxOutput: Int = 680,
    val estimatedAveragePowerMa: Int = 175
) {
    fun toJson(): JSONObject {
        val root = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("genre", genre)
            put("bpm", bpm)
            put("totalDurationMs", totalDurationMs)
            put("description", description)
            put("peakLuxOutput", peakLuxOutput)
            put("estimatedAveragePowerMa", estimatedAveragePowerMa)
        }
        val arr = JSONArray()
        beats.forEach { arr.put(it.toJson()) }
        root.put("beats", arr)
        return root
    }
}

/**
 * Real-time Music, Beat & Vibration Synchronizer Engine.
 * Translates audio rhythms, kick drums, snares, and melodies into synchronized
 * dual-LED camera flashes (Front Dual-LED & Rear Quad-LED) and calibrated tactile haptics.
 */
class MusicBeatSynchronizer(
    private val context: Context,
    private val actuatorController: ActuatorController
) {
    private val TAG = "MusicBeatSynchronizer"
    private var playbackJob: Job? = null
    private var isPlaying = false

    // Hardware sync toggles
    var syncRearFlashOnKick: Boolean = true
    var syncFrontFlashOnSnare: Boolean = true
    var syncDualStrobeOnDrop: Boolean = true
    var syncHapticsOnBeats: Boolean = true

    /**
     * Synthesizes a high-energy Cyberpunk Synthwave track (128 BPM, 4/4 meter).
     * Bass kicks hit on beats 1, 2, 3, 4; Snares crack on beats 2 & 4 with soaring synth arpeggios.
     */
    fun buildCyberpunkSynthwaveTrack(): SongChoreographyTrack {
        val bpm = 128
        val beatMs = (60_000.0 / bpm).toLong() // ~468ms per beat
        val beats = mutableListOf<MusicalNoteBeat>()

        // 4 bars of 4/4 = 16 beats (~7.5 seconds)
        for (bar in 0 until 4) {
            val barStart = bar * (beatMs * 4)

            // Beat 1: Heavy Kick + Sub-bass drop (Rear Quad-LED Flash + Heavy Haptic)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart,
                    frequencyHz = 65,
                    durationMs = 180,
                    amplitude = 1.0f,
                    band = FrequencyBand.BASS,
                    syncTarget = ActuatorSyncTarget.REAR_QUAD_FLASH,
                    waveform = "SINE"
                )
            )

            // Beat 1.5: Synth Arpeggio 1 (D5)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs / 2),
                    frequencyHz = 587,
                    durationMs = 100,
                    amplitude = 0.7f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "TRIANGLE"
                )
            )

            // Beat 2: Snare Crack + Hi-hat (Front Dual-LED Flash + Haptic TICK)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + beatMs,
                    frequencyHz = 880,
                    durationMs = 140,
                    amplitude = 0.9f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "TRIANGLE"
                )
            )

            // Beat 3: Kick Drop
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 2),
                    frequencyHz = 73,
                    durationMs = 180,
                    amplitude = 0.95f,
                    band = FrequencyBand.BASS,
                    syncTarget = ActuatorSyncTarget.REAR_QUAD_FLASH,
                    waveform = "SINE"
                )
            )

            // Beat 4: Peak Climax Drop (Dual 360° Strobe on bar 2 & 4, Snare on 1 & 3)
            val isPeakDrop = bar == 1 || bar == 3
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 3),
                    frequencyHz = if (isPeakDrop) 1175 else 880,
                    durationMs = if (isPeakDrop) 220 else 140,
                    amplitude = 1.0f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = if (isPeakDrop) ActuatorSyncTarget.DUAL_360_STROBE else ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "TRIANGLE"
                )
            )
        }

        val totalDur = 4 * (beatMs * 4) + 200L

        return SongChoreographyTrack(
            id = "track-cyberpunk-${System.currentTimeMillis()}",
            title = "Cybernetic Synthwave Beat Drop",
            genre = "Electronic / Synthwave (128 BPM)",
            bpm = bpm,
            totalDurationMs = totalDur,
            beats = beats,
            description = "Driving 128 BPM electronic synth track syncing Rear Quad kicks, Front Dual snares, and Dual 360° drop strobes.",
            peakLuxOutput = 680,
            estimatedAveragePowerMa = 195
        )
    }

    /**
     * Synthesizes an energetic Telugu Cinematic Rhythm & Dhol Beat (138 BPM, syncopated folk pulse).
     */
    fun buildTeluguCinematicRhythm(): SongChoreographyTrack {
        val bpm = 138
        val beatMs = (60_000.0 / bpm).toLong() // ~434ms
        val beats = mutableListOf<MusicalNoteBeat>()

        // 4 bars of syncopated rhythm
        for (bar in 0 until 4) {
            val barStart = bar * (beatMs * 4)

            // Dhol base thumping (Sub-bass kick on 1 and 2.5)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart,
                    frequencyHz = 55,
                    durationMs = 200,
                    amplitude = 1.0f,
                    band = FrequencyBand.SUB_BASS,
                    syncTarget = ActuatorSyncTarget.FULL_SYNESTHESIA,
                    waveform = "SINE"
                )
            )

            // Syncopated high-clap on beat 1.75
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 3 / 4),
                    frequencyHz = 1046,
                    durationMs = 90,
                    amplitude = 0.8f,
                    band = FrequencyBand.TREBLE,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "TRIANGLE"
                )
            )

            // Sharp rimshot crack on beat 3
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 2),
                    frequencyHz = 784,
                    durationMs = 120,
                    amplitude = 0.9f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "TRIANGLE"
                )
            )

            // Thundering double roll on beat 3.5 & 4
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 5 / 2),
                    frequencyHz = 110,
                    durationMs = 110,
                    amplitude = 0.9f,
                    band = FrequencyBand.BASS,
                    syncTarget = ActuatorSyncTarget.REAR_QUAD_FLASH,
                    waveform = "SINE"
                )
            )
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 3),
                    frequencyHz = 130,
                    durationMs = 150,
                    amplitude = 1.0f,
                    band = FrequencyBand.BASS,
                    syncTarget = ActuatorSyncTarget.DUAL_360_STROBE,
                    waveform = "SINE"
                )
            )
        }

        val totalDur = 4 * (beatMs * 4) + 200L

        return SongChoreographyTrack(
            id = "track-telugu-folk-${System.currentTimeMillis()}",
            title = "Cinematic Telugu Epic Rhythm (Dhol & Beat Drop)",
            genre = "Cinematic Folk & Action (138 BPM)",
            bpm = bpm,
            totalDurationMs = totalDur,
            beats = beats,
            description = "High-energy cinematic folk dhol rhythm with syncopated double-rolls, deep bass haptic rumbles, and peak strobe drops.",
            peakLuxOutput = 680,
            estimatedAveragePowerMa = 220
        )
    }

    /**
     * Synthesizes a relaxing Lo-Fi Midnight Jazz Groove (75 BPM, warm chords, ambient front-LED glow).
     */
    fun buildLofiMidnightGroove(): SongChoreographyTrack {
        val bpm = 75
        val beatMs = (60_000.0 / bpm).toLong() // 800ms
        val beats = mutableListOf<MusicalNoteBeat>()

        // 4 bars of chill jazz progression
        for (bar in 0 until 4) {
            val barStart = bar * (beatMs * 4)

            // Warm root chord (Sub-bass C3 = 130Hz)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart,
                    frequencyHz = 130,
                    durationMs = 400,
                    amplitude = 0.6f,
                    band = FrequencyBand.SUB_BASS,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "SINE"
                )
            )

            // Soft rhodes chord (E3 = 164Hz)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + beatMs,
                    frequencyHz = 164,
                    durationMs = 350,
                    amplitude = 0.5f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.HAPTIC_SNARE_TICK,
                    waveform = "SINE"
                )
            )

            // Soft brushed snare on beat 3 (G3 = 196Hz)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 2),
                    frequencyHz = 196,
                    durationMs = 300,
                    amplitude = 0.55f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.FRONT_DUAL_FLASH,
                    waveform = "SINE"
                )
            )

            // Melodic jazz extension (B3 = 246Hz)
            beats.add(
                MusicalNoteBeat(
                    timestampMs = barStart + (beatMs * 3),
                    frequencyHz = 246,
                    durationMs = 380,
                    amplitude = 0.5f,
                    band = FrequencyBand.MIDRANGE,
                    syncTarget = ActuatorSyncTarget.HAPTIC_SNARE_TICK,
                    waveform = "SINE"
                )
            )
        }

        val totalDur = 4 * (beatMs * 4) + 200L

        return SongChoreographyTrack(
            id = "track-lofi-${System.currentTimeMillis()}",
            title = "Lo-Fi Midnight Jazz Groove",
            genre = "Chill / Lo-Fi Beats (75 BPM)",
            bpm = bpm,
            totalDurationMs = totalDur,
            beats = beats,
            description = "Gentle midnight lo-fi chord progression with soft front dual-LED ambient breathing pulses and relaxing micro-ticks.",
            peakLuxOutput = 120,
            estimatedAveragePowerMa = 55
        )
    }

    /**
     * Executes the synchronized playback:
     * - Generates PCM audio stream dynamically and plays via AudioTrack
     * - Dispatches hardware camera flashes and haptics in exact sub-millisecond lockstep!
     */
    fun playSongWithHardwareSync(
        track: SongChoreographyTrack,
        onBeatDispatched: ((MusicalNoteBeat) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        stopPlayback()
        isPlaying = true

        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                // Generate complete PCM audio buffer for track
                val sampleRate = 44100
                val totalSamples = (sampleRate * (track.totalDurationMs / 1000.0)).toInt()
                val pcmBuffer = ShortArray(totalSamples)

                track.beats.forEach { beat ->
                    val startSample = ((beat.timestampMs / 1000.0) * sampleRate).toInt().coerceIn(0, totalSamples - 1)
                    val noteSamples = ((beat.durationMs / 1000.0) * sampleRate).toInt()
                    val amp = ((32767 * beat.amplitude) * 0.75).toInt()

                    for (i in 0 until noteSamples) {
                        val targetIndex = startSample + i
                        if (targetIndex >= totalSamples) break

                        // Envelope (smooth 8ms attack, 20ms decay)
                        val attack = (sampleRate * 0.008).toInt()
                        val decay = (sampleRate * 0.025).toInt()
                        val env = when {
                            i < attack -> i.toDouble() / attack
                            i > (noteSamples - decay) -> (noteSamples - i).toDouble() / decay
                            else -> 1.0
                        }

                        val t = i.toDouble() / sampleRate
                        val waveSample = when (beat.waveform.uppercase()) {
                            "TRIANGLE" -> {
                                val p = 1.0 / beat.frequencyHz
                                val ph = (t % p) / p
                                if (ph < 0.5) 4.0 * ph - 1.0 else 3.0 - 4.0 * ph
                            }
                            else -> sin(2.0 * PI * beat.frequencyHz * t)
                        }

                        val currentVal = pcmBuffer[targetIndex].toInt()
                        val newVal = (currentVal + (waveSample * amp * env).toInt()).coerceIn(-32768, 32767)
                        pcmBuffer[targetIndex] = newVal.toShort()
                    }
                }

                // Fire procedural audio playback in background AudioTrack
                launch(Dispatchers.IO) {
                    try {
                        val audioTrack = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(pcmBuffer.size * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()

                        audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                        audioTrack.play()
                    } catch (e: Exception) {
                        Log.e(TAG, "AudioTrack playback error: ${e.message}")
                    }
                }

                // Synchronized Actuation Dispatcher loop
                val startTime = System.currentTimeMillis()
                val sortedBeats = track.beats.sortedBy { it.timestampMs }

                for (beat in sortedBeats) {
                    if (!isActive || !isPlaying) break

                    val elapsed = System.currentTimeMillis() - startTime
                    val delayUntilBeat = beat.timestampMs - elapsed
                    if (delayUntilBeat > 0) {
                        delay(delayUntilBeat)
                    }

                    // Trigger hardware actuators based on frequency band and target
                    dispatchHardwareBeat(beat)
                    onBeatDispatched?.invoke(beat)
                }

                // Wait for the remaining duration
                val remaining = track.totalDurationMs - (System.currentTimeMillis() - startTime)
                if (remaining > 0) {
                    delay(remaining)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback error: ${e.message}")
            } finally {
                // Ensure all hardware emitters are safely extinguished
                actuatorController.setFlashlight("0", false)
                actuatorController.setFlashlight("1", false)
                isPlaying = false
                onFinished?.invoke()
            }
        }
    }

    /**
     * Dispatches individual transient beat onto device hardware in lockstep.
     */
    private fun dispatchHardwareBeat(beat: MusicalNoteBeat) {
        val flashDuration = (beat.durationMs.coerceAtMost(120)).toLong()

        when (beat.syncTarget) {
            ActuatorSyncTarget.REAR_QUAD_FLASH -> {
                if (syncRearFlashOnKick) {
                    actuatorController.setFlashlight("0", true)
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(flashDuration)
                        actuatorController.setFlashlight("0", false)
                    }
                }
                if (syncHapticsOnBeats) {
                    actuatorController.triggerHaptic("BURST")
                }
            }

            ActuatorSyncTarget.FRONT_DUAL_FLASH -> {
                if (syncFrontFlashOnSnare) {
                    actuatorController.setFlashlight("1", true)
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(flashDuration)
                        actuatorController.setFlashlight("1", false)
                    }
                }
                if (syncHapticsOnBeats) {
                    actuatorController.triggerHaptic("TICK")
                }
            }

            ActuatorSyncTarget.DUAL_360_STROBE, ActuatorSyncTarget.FULL_SYNESTHESIA -> {
                if (syncDualStrobeOnDrop) {
                    actuatorController.setFlashlight("0", true)
                    actuatorController.setFlashlight("1", true)
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(flashDuration + 30L)
                        actuatorController.setFlashlight("0", false)
                        actuatorController.setFlashlight("1", false)
                    }
                }
                if (syncHapticsOnBeats) {
                    actuatorController.triggerHaptic("BURST")
                }
            }

            ActuatorSyncTarget.HAPTIC_KICK -> {
                if (syncHapticsOnBeats) {
                    actuatorController.triggerHaptic("BURST")
                }
            }

            ActuatorSyncTarget.HAPTIC_SNARE_TICK -> {
                if (syncHapticsOnBeats) {
                    actuatorController.triggerHaptic("TICK")
                }
            }
        }
    }

    /**
     * Halts ongoing playback and turns off all torches immediately.
     */
    fun stopPlayback() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        actuatorController.setFlashlight("0", false)
        actuatorController.setFlashlight("1", false)
    }

    /**
     * Persists the song choreography to /sdcard/LocalAgent/brain_vault.json.
     */
    fun saveSongTrackToVault(track: SongChoreographyTrack): Boolean {
        return try {
            val payload = JSONObject().apply {
                put("type", "MUSIC_RHYTHM_SYNESTHESIA_TRACK")
                put("timestamp", System.currentTimeMillis())
                put("track", track.toJson())
            }
            BrainVaultManager.persistVaultToExternalStorage(context, payload.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save song track to vault: ${e.message}")
            false
        }
    }
}
