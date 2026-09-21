package com.example.service.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.YapeTransaction
import com.example.data.preferences.VoicePreferences
import com.example.data.preferences.StoreConfig
import com.example.data.preferences.StorePreferences
import com.example.service.sound.NotificationSoundManager
import com.example.service.sound.NotificationSoundType
import com.example.service.sound.SoundAlertMode
import com.example.service.tts.YapeTtsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemoteAlertManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val ttsManager = YapeTtsManager.getInstance(context)
    private val soundManager = NotificationSoundManager.getInstance(context)
    private val storePreferences = StorePreferences.getInstance(context)
    private val voicePreferences = VoicePreferences.getInstance(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Nota: Se silencia el sonido por defecto del canal de notificación (setSound(null, null))
            // para que Android no dispare obligatoriamente el ding por defecto del sistema,
            // permitiendo que Brynn reproduzca exactamente el tono elegido por el usuario (Cha-Ching, Campana, Monedas, etc.)
            // con NotificationSoundManager sin que se superpongan ni sea ignorado.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pagos Remotos en Tienda",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas y avisos en vivo de Yapes recibidos en el negocio"
                enableVibration(false) // La vibración es controlada según la configuración de la app
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerRemotePaymentAlert(transaction: YapeTransaction, storeConfig: StoreConfig) {
        // 1. Post native Android system notification
        postSystemNotification(transaction, storeConfig)

        // 2. Play vibration if enabled in store config
        if (storeConfig.remoteVibrateEnabled) {
            vibrateDevice()
        }

        // 3. Sound and voice alert evaluation
        val voiceSettings = voicePreferences.loadSettings()
        val soundType = NotificationSoundType.fromKey(voiceSettings.notificationSound)
        val soundAlertMode = SoundAlertMode.fromKey(voiceSettings.soundAlertMode)

        val willSpeakVoice = (storeConfig.remoteTtsEnabled || voiceSettings.voiceEnabled) && voiceSettings.announceRemoteNotiyape
        val shouldPlaySound = soundAlertMode == SoundAlertMode.ALWAYS || (soundAlertMode == SoundAlertMode.WHEN_VOICE_DISABLED && !willSpeakVoice)

        if (shouldPlaySound) {
            soundManager.playSound(
                soundType = soundType,
                volume = voiceSettings.soundVolume,
                vibrate = voiceSettings.vibrateWithSound && !storeConfig.remoteVibrateEnabled,
                keepFocusForCallback = willSpeakVoice,
                onFinished = {
                    if (willSpeakVoice) {
                        ttsManager.speakPayment(transaction.senderName, transaction.amount, isRemote = true)
                    }
                }
            )
        } else if (willSpeakVoice) {
            ttsManager.speakPayment(transaction.senderName, transaction.amount, isRemote = true)
        }
    }

    private fun buildRemoteSpeechText(transaction: YapeTransaction, template: String): String {
        val timeStr = SimpleDateFormat("hh:mm a", Locale("es", "PE")).format(Date(transaction.timestamp))
        val amountWords = YapeTtsManager.formatAmountForSpeech(transaction.amount)
        val spokenSender = YapeTtsManager.cleanSenderForSpeech(transaction.senderName)

        return template
            .replace("{monto} soles", amountWords, ignoreCase = true)
            .replace("{monto} sol", amountWords, ignoreCase = true)
            .replace("{monto}", amountWords)
            .replace("{emisor}", spokenSender)
            .replace("{hora}", timeStr)
    }

    private fun postSystemNotification(transaction: YapeTransaction, storeConfig: StoreConfig) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id.toInt().coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val storeDisplayName = storeConfig.storeName.ifBlank { "Mi Tienda" }
        val title = "¡Pago recibido en $storeDisplayName!"
        val secCode = transaction.securityCode
        val shortBody = if (!secCode.isNullOrBlank()) {
            "${transaction.formattedAmount} de ${transaction.senderName} • Cód: $secCode"
        } else {
            "${transaction.formattedAmount} de ${transaction.senderName}"
        }

        val expandedText = buildString {
            append("Monto: ").append(transaction.formattedAmount).append("\n")
            append("De: ").append(transaction.senderName).append("\n")
            append("Hora: ").append(transaction.formattedTime)
            if (!secCode.isNullOrBlank()) {
                append("\nCód. Seguridad: ").append(secCode)
            }
            if (transaction.note.isNotBlank()) {
                append("\nNota: ").append(transaction.note)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_payment)
            .setColor(0xFF00A86B.toInt()) // Elegante verde esmeralda / pago exitoso
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("💰 ¡Pago confirmado! ${transaction.formattedAmount}")
                    .setSummaryText(storeDisplayName)
                    .bigText(expandedText)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSilent(true)
            .setSound(null)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (transaction.timestamp % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
                }
            }
        } catch (_: Exception) {}
    }

    fun showRevokedNotification(storeName: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                8888,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Vinculación finalizada")
                .setContentText("Has sido desvinculado de $storeName. Ya no recibirás alertas de cobro.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("El dueño de $storeName ha revocado tu acceso a la tienda. Este dispositivo ha sido desvinculado y ya no recibirá notificaciones de ventas."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(8888, notification)
        } catch (e: Exception) {
            android.util.Log.e("RemoteAlertManager", "Error showing revoked notification", e)
        }
    }


    companion object {
        const val CHANNEL_ID = "remote_yape_payments_channel_v2"

        @Volatile
        private var INSTANCE: RemoteAlertManager? = null

        fun getInstance(context: Context): RemoteAlertManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RemoteAlertManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
