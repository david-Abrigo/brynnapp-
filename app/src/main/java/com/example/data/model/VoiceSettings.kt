package com.example.data.model

data class VoiceSettings(
    val voiceEnabled: Boolean = true,
    val announceLocalYape: Boolean = true, // Locución de Yapes en dispositivo emisor (monto cantado al recibir Yape)
    val announceRemoteNotiyape: Boolean = true, // Anuncios por voz por cada pago recibido en modo receptor (Brynn)
    val volume: Float = 1.0f, // 0.0f to 1.0f
    val speechRate: Float = 1.0f, // 0.5f to 2.0f
    val pitch: Float = 1.0f, // 0.5f to 2.0f
    val messageTemplate: String = "¡Recibiste {monto} soles de {emisor}!",
    // Notification Sound customization (especially useful when voice is disabled or as chime alert)
    val notificationSound: String = "CHA_CHING", // CHA_CHING, BELL_CHIME, COIN_DROP, SUCCESS_CHORD, DIGITAL_BEEP, GENTLE_PING, SYSTEM_DEFAULT, MUTE
    val soundAlertMode: String = "WHEN_VOICE_DISABLED", // WHEN_VOICE_DISABLED, ALWAYS, OFF
    val soundVolume: Float = 1.0f, // 0.0f to 1.0f
    val vibrateWithSound: Boolean = true
)

