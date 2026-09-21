package com.example.service.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class NotificationSoundType(
    val key: String,
    val displayName: String,
    val description: String,
    val iconEmoji: String
) {
    CHA_CHING(
        key = "CHA_CHING",
        displayName = "Caja Registradora (Cha-Ching)",
        description = "Sonido clásico metálico de cobro en caja",
        iconEmoji = "💰"
    ),
    BELL_CHIME(
        key = "BELL_CHIME",
        displayName = "Campana de Tienda (Ding-Dong)",
        description = "Doble campanada limpia de atención",
        iconEmoji = "🔔"
    ),
    COIN_DROP(
        key = "COIN_DROP",
        displayName = "Caída de Monedas",
        description = "Tintineo metálico de monedas cayendo",
        iconEmoji = "🪙"
    ),
    SUCCESS_CHORD(
        key = "SUCCESS_CHORD",
        displayName = "Acorde de Éxito",
        description = "Arpegio armónico ascendente de confirmación",
        iconEmoji = "✨"
    ),
    DIGITAL_BEEP(
        key = "DIGITAL_BEEP",
        displayName = "Bip Digital POS",
        description = "Doble tono electrónico de terminal de pago",
        iconEmoji = "📟"
    ),
    GENTLE_PING(
        key = "GENTLE_PING",
        displayName = "Ping Suave",
        description = "Notificación sutil y discreta de alta frecuencia",
        iconEmoji = "💧"
    ),
    SYSTEM_DEFAULT(
        key = "SYSTEM_DEFAULT",
        displayName = "Tono Predeterminado del Sistema",
        description = "Sonido estándar de notificaciones de Android",
        iconEmoji = "📱"
    ),
    MUTE(
        key = "MUTE",
        displayName = "Silencioso (Solo Vibración)",
        description = "Sin tono auditivo al recibir pagos",
        iconEmoji = "🔕"
    );

    companion object {
        fun fromKey(key: String): NotificationSoundType {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: CHA_CHING
        }
    }
}

enum class SoundAlertMode(
    val key: String,
    val title: String,
    val description: String
) {
    WHEN_VOICE_DISABLED(
        key = "WHEN_VOICE_DISABLED",
        title = "Sonar si la voz está desactivada",
        description = "Si no deseas locución hablada, sonará el tono elegido de inmediato"
    ),
    ALWAYS(
        key = "ALWAYS",
        title = "Sonar siempre (Tono + Voz)",
        description = "Emite el tono sonoro como aviso instantáneo en cada Yape"
    ),
    OFF(
        key = "OFF",
        title = "Desactivado",
        description = "No emitir sonidos alternativos"
    );

    companion object {
        fun fromKey(key: String): SoundAlertMode {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: WHEN_VOICE_DISABLED
        }
    }
}

class NotificationSoundManager private constructor(private val context: Context) {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Reproduce el sonido de notificación configurado según el modo y volumen.
     */
    fun playSound(
        soundType: NotificationSoundType,
        volume: Float = 1.0f,
        vibrate: Boolean = true,
        keepFocusForCallback: Boolean = false,
        onFinished: (() -> Unit)? = null
    ) {
        if (vibrate) {
            triggerVibration()
        }

        if (soundType == NotificationSoundType.MUTE) {
            onFinished?.invoke()
            return
        }

        audioScope.launch {
            try {
                when (soundType) {
                    NotificationSoundType.SYSTEM_DEFAULT -> playSystemDefaultSound(keepFocusForCallback)
                    NotificationSoundType.CHA_CHING -> playSynthesizedChaChing(volume, keepFocusForCallback)
                    NotificationSoundType.BELL_CHIME -> playSynthesizedBellChime(volume, keepFocusForCallback)
                    NotificationSoundType.COIN_DROP -> playSynthesizedCoinDrop(volume, keepFocusForCallback)
                    NotificationSoundType.SUCCESS_CHORD -> playSynthesizedSuccessChord(volume, keepFocusForCallback)
                    NotificationSoundType.DIGITAL_BEEP -> playSynthesizedDigitalBeep(volume, keepFocusForCallback)
                    NotificationSoundType.GENTLE_PING -> playSynthesizedGentlePing(volume, keepFocusForCallback)
                    NotificationSoundType.MUTE -> { /* No audio */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing sound: ${soundType.name}", e)
                playSystemDefaultSound(keepFocusForCallback)
            } finally {
                onFinished?.invoke()
            }
        }
    }

    private fun playSystemDefaultSound(keepFocus: Boolean = false) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        var focusRequest: AudioFocusRequest? = null

        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { }
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }

        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.audioAttributes = audioAttributes
            ringtone?.play()
            var elapsed = 0
            while (ringtone?.isPlaying == true && elapsed < 1200) {
                Thread.sleep(100)
                elapsed += 100
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing default system ringtone", e)
        } finally {
            if (!keepFocus && audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
                Log.d(TAG, "Audio focus released after system default sound playback")
            }
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 150), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 120, 80, 150), -1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes and plays a PCM buffer using AudioTrack
     */
    private fun playPcmSamples(
        samples: ShortArray,
        sampleRate: Int = 44100,
        volume: Float = 1.0f,
        keepFocus: Boolean = false
    ) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var focusRequest: AudioFocusRequest? = null

        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* transient ducking */ }
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }

        try {
            audioTrack.setVolume(volume.coerceIn(0.05f, 1.0f))
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep((samples.size * 1000L / sampleRate) + 60L)
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack playback error", e)
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}

            if (!keepFocus && audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
                Log.d(TAG, "Audio focus released after sound playback")
            }
        }
    }

    /**
     * Sonido "Cha-Ching" (Caja registradora metálica con apertura y campana de cobro)
     */
    private fun playSynthesizedChaChing(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.65
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Part 1: Fast metallic mechanical slide / clink (0 to 0.12s)
        // Part 2: Bright ring bell (0.10s to 0.65s) at 2093 Hz (C7) + 3135 Hz (G7) harmonic
        val part1End = (sampleRate * 0.12).toInt()
        val bellStart = (sampleRate * 0.08).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            // Mechanical clink
            if (i < part1End) {
                val env1 = 1.0 - (i.toDouble() / part1End)
                val noise = (Math.random() * 2.0 - 1.0) * 0.4
                val tone1 = sin(2.0 * PI * 1400.0 * t) * 0.3 + sin(2.0 * PI * 2800.0 * t) * 0.3
                sample += (tone1 + noise) * env1
            }

            // High Cash Bell Ring with smooth exponential decay
            if (i >= bellStart) {
                val bellT = (i - bellStart).toDouble() / sampleRate
                val decay = exp(-5.5 * bellT)
                val f1 = 2093.0 // C7
                val f2 = 3135.0 // G7
                val f3 = 4186.0 // C8
                val ring = (sin(2.0 * PI * f1 * bellT) * 0.55 +
                        sin(2.0 * PI * f2 * bellT) * 0.30 +
                        sin(2.0 * PI * f3 * bellT) * 0.15) * decay
                sample += ring
            }

            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    /**
     * Sonido "Campana de Tienda / Ding Dong"
     */
    private fun playSynthesizedBellChime(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.75
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val dingSamples = (sampleRate * 0.28).toInt()

        for (i in 0 until totalSamples) {
            var sample = 0.0
            val t = i.toDouble() / sampleRate

            if (i < dingSamples) {
                // "Ding" (G5: 784 Hz + 1568 Hz)
                val decay1 = exp(-6.0 * t)
                sample += (sin(2.0 * PI * 783.99 * t) * 0.65 + sin(2.0 * PI * 1567.98 * t) * 0.35) * decay1
            } else {
                // "Dong" (E5: 659 Hz + 1318 Hz)
                val dongT = (i - dingSamples).toDouble() / sampleRate
                val decay2 = exp(-4.5 * dongT)
                sample += (sin(2.0 * PI * 659.25 * dongT) * 0.7 + sin(2.0 * PI * 1318.5 * dongT) * 0.3) * decay2
            }

            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    /**
     * Sonido "Caída de Monedas"
     */
    private fun playSynthesizedCoinDrop(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.6
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Multiple rapid coin pings
        val hits = listOf(
            Pair(0.0, 2400.0),
            Pair(0.08, 2750.0),
            Pair(0.15, 3100.0),
            Pair(0.22, 2900.0),
            Pair(0.31, 3300.0)
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            for ((hitTime, freq) in hits) {
                if (t >= hitTime) {
                    val dt = t - hitTime
                    val decay = exp(-18.0 * dt)
                    val metal = sin(2.0 * PI * freq * dt) * 0.6 + sin(2.0 * PI * (freq * 1.48) * dt) * 0.4
                    sample += metal * decay * 0.4
                }
            }

            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    /**
     * Sonido "Acorde de Éxito" (Arpegio ascendente C5 -> E5 -> G5 -> C6)
     */
    private fun playSynthesizedSuccessChord(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.7
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val notes = listOf(
            Pair(0.00, 523.25), // C5
            Pair(0.09, 659.25), // E5
            Pair(0.18, 783.99), // G5
            Pair(0.27, 1046.50) // C6
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            for ((startTime, freq) in notes) {
                if (t >= startTime) {
                    val dt = t - startTime
                    val decay = exp(-5.0 * dt)
                    val tone = (sin(2.0 * PI * freq * dt) * 0.7 + sin(2.0 * PI * (freq * 2) * dt) * 0.3) * decay
                    sample += tone * 0.35
                }
            }

            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    /**
     * Sonido "Bip Digital POS"
     */
    private fun playSynthesizedDigitalBeep(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.35
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val beep1End = (sampleRate * 0.10).toInt()
        val beep2Start = (sampleRate * 0.16).toInt()
        val beep2End = (sampleRate * 0.28).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            if (i < beep1End) {
                val env = sin(PI * (i.toDouble() / beep1End))
                sample = sin(2.0 * PI * 1800.0 * t) * env * 0.7
            } else if (i in beep2Start until beep2End) {
                val relI = i - beep2Start
                val len = beep2End - beep2Start
                val env = sin(PI * (relI.toDouble() / len))
                sample = sin(2.0 * PI * 2400.0 * t) * env * 0.8
            }

            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    /**
     * Sonido "Ping Suave"
     */
    private fun playSynthesizedGentlePing(volume: Float, keepFocus: Boolean = false) {
        val sampleRate = 44100
        val durationSec = 0.45
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-8.0 * t)
            val sample = (sin(2.0 * PI * 1480.0 * t) * 0.75 + sin(2.0 * PI * 2960.0 * t) * 0.25) * decay
            val clamped = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = clamped.toShort()
        }

        playPcmSamples(buffer, sampleRate, volume, keepFocus)
    }

    companion object {
        private const val TAG = "NotificationSoundMgr"

        @Volatile
        private var INSTANCE: NotificationSoundManager? = null

        fun getInstance(context: Context): NotificationSoundManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationSoundManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
