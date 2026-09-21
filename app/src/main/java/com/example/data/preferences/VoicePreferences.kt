package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoicePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yape_voice_preferences", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    fun loadSettings(): VoiceSettings {
        val masterEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        return VoiceSettings(
            voiceEnabled = masterEnabled,
            announceLocalYape = prefs.getBoolean(KEY_ANNOUNCE_LOCAL_YAPE, masterEnabled),
            announceRemoteNotiyape = prefs.getBoolean(KEY_ANNOUNCE_REMOTE_NOTIYAPE, masterEnabled),
            volume = prefs.getFloat(KEY_VOLUME, 1.0f),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            pitch = prefs.getFloat(KEY_PITCH, 1.0f),
            messageTemplate = prefs.getString(KEY_TEMPLATE, "¡Recibiste {monto} soles de {emisor}!")
                ?: "¡Recibiste {monto} soles de {emisor}!",
            notificationSound = prefs.getString(KEY_NOTIFICATION_SOUND, "CHA_CHING") ?: "CHA_CHING",
            soundAlertMode = prefs.getString(KEY_SOUND_ALERT_MODE, "WHEN_VOICE_DISABLED") ?: "WHEN_VOICE_DISABLED",
            soundVolume = prefs.getFloat(KEY_SOUND_VOLUME, 1.0f),
            vibrateWithSound = prefs.getBoolean(KEY_VIBRATE_WITH_SOUND, true)
        )
    }

    fun updateSettings(newSettings: VoiceSettings) {
        prefs.edit()
            .putBoolean(KEY_VOICE_ENABLED, newSettings.voiceEnabled)
            .putBoolean(KEY_ANNOUNCE_LOCAL_YAPE, newSettings.announceLocalYape)
            .putBoolean(KEY_ANNOUNCE_REMOTE_NOTIYAPE, newSettings.announceRemoteNotiyape)
            .putFloat(KEY_VOLUME, newSettings.volume)
            .putFloat(KEY_SPEECH_RATE, newSettings.speechRate)
            .putFloat(KEY_PITCH, newSettings.pitch)
            .putString(KEY_TEMPLATE, newSettings.messageTemplate)
            .putString(KEY_NOTIFICATION_SOUND, newSettings.notificationSound)
            .putString(KEY_SOUND_ALERT_MODE, newSettings.soundAlertMode)
            .putFloat(KEY_SOUND_VOLUME, newSettings.soundVolume)
            .putBoolean(KEY_VIBRATE_WITH_SOUND, newSettings.vibrateWithSound)
            .apply()
        _settings.value = newSettings
    }

    companion object {
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_ANNOUNCE_LOCAL_YAPE = "announce_local_yape"
        private const val KEY_ANNOUNCE_REMOTE_NOTIYAPE = "announce_remote_notiyape"
        private const val KEY_VOLUME = "volume"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_PITCH = "pitch"
        private const val KEY_TEMPLATE = "template"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_SOUND_ALERT_MODE = "sound_alert_mode"
        private const val KEY_SOUND_VOLUME = "sound_volume"
        private const val KEY_VIBRATE_WITH_SOUND = "vibrate_with_sound"

        @Volatile
        private var INSTANCE: VoicePreferences? = null

        fun getInstance(context: Context): VoicePreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = VoicePreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
