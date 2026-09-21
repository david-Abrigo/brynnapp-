package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.DeviceRole
import com.example.data.model.YapeTransaction
import com.example.data.preferences.VoicePreferences
import com.example.data.repository.TransactionRepository
import com.example.service.parser.ParseResult
import com.example.service.parser.YapeNotificationParser
import com.example.service.sound.NotificationSoundManager
import com.example.service.sound.NotificationSoundType
import com.example.service.sound.SoundAlertMode
import com.example.service.tts.YapeTtsManager
import com.example.widget.YapeTotalWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class YapeNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: TransactionRepository
    private lateinit var ttsManager: YapeTtsManager
    private lateinit var soundManager: NotificationSoundManager
    private lateinit var voicePreferences: VoicePreferences
    private lateinit var storePreferences: com.example.data.preferences.StorePreferences

    // Cache to prevent duplicate processing within 10 seconds
    private val processedNotifications = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        repository = TransactionRepository(applicationContext)
        ttsManager = YapeTtsManager.getInstance(applicationContext)
        soundManager = NotificationSoundManager.getInstance(applicationContext)
        voicePreferences = VoicePreferences.getInstance(applicationContext)
        storePreferences = com.example.data.preferences.StorePreferences.getInstance(applicationContext)
        Log.d(TAG, "YapeNotificationListenerService started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val combinedText = if (bigText.isNotBlank() && bigText != text) {
            "$text $bigText"
        } else {
            text
        }

        // Deduplication key
        val dedupeKey = "${sbn.key}_${title}_${combinedText}"
        val now = System.currentTimeMillis()
        val lastProcessed = processedNotifications[dedupeKey] ?: 0L
        if (now - lastProcessed < 10000) {
            // Already processed this exact notification recently
            return
        }

        // 1. Ignorar completamente las notificaciones generadas por la propia app Brynn.
        // Esto evita que cuando el dispositivo receptor recibe una alerta remota y emite su notificación del sistema,
        // el NotificationListener vuelva a leerla creyendo que es un nuevo cobro local (bucle/autolectura).
        if (packageName == applicationContext.packageName) {
            return
        }

        val currentStoreConfig = storePreferences.config.value

        // Si el dispositivo está en modo RECEPTOR (Trabajador), ignorar la captura de notificaciones locales de pago.
        // El trabajador solo debe recibir pagos desde la nube (FCM push); sus Yapes personales no deben registrarse en la tienda.
        if (currentStoreConfig.deviceRole == DeviceRole.RECEIVER) {
            Log.d(TAG, "Dispositivo en modo RECEPTOR (Trabajador): ignorando captura de notificación local")
            return
        }

        // 2. FILTRADO ESTRICTO DE APLICACIONES CONFIGURADAS POR EL USUARIO:
        // Solo procesar notificaciones emitidas por las aplicaciones activadas en la configuración (Yape, BCP o Plin).
        // Si el usuario desmarca alguna o proviene de SMS/WhatsApp, se descarta inmediatamente.
        val isAllowed = isAllowedPaymentApp(
            packageName = packageName,
            captureYape = currentStoreConfig.captureYape,
            captureBcp = currentStoreConfig.captureBcp,
            capturePlin = currentStoreConfig.capturePlin
        )
        if (!isAllowed) {
            Log.d(TAG, "Ignorando notificación de app excluida o no seleccionada: $packageName")
            return
        }

        val parseResult = YapeNotificationParser.parse(
            title = title,
            text = combinedText,
            subText = subText,
            packageName = packageName
        )

        when (parseResult) {
            is ParseResult.Success -> {
                processedNotifications[dedupeKey] = now
                // Clean old cache entries
                if (processedNotifications.size > 100) {
                    processedNotifications.entries.removeIf { now - it.value > 60000 }
                }

                Log.d(TAG, "Yape payment detected! Sender: ${parseResult.senderName}, Amount: ${parseResult.amount}")

                scope.launch {
                    val transaction = YapeTransaction(
                        senderName = parseResult.senderName,
                        amount = parseResult.amount,
                        timestamp = now,
                        rawNotification = parseResult.rawNotification,
                        transactionType = "RECEIVED"
                    )

                    repository.insertTransaction(transaction)

                    // Check sound and voice preferences
                    val voiceSettings = voicePreferences.loadSettings()
                    val soundType = NotificationSoundType.fromKey(voiceSettings.notificationSound)
                    val soundAlertMode = SoundAlertMode.fromKey(voiceSettings.soundAlertMode)
                    val willSpeakVoice = voiceSettings.voiceEnabled && voiceSettings.announceLocalYape

                    // Play customized notification sound if configured
                    val shouldPlaySound = soundAlertMode == SoundAlertMode.ALWAYS || (soundAlertMode == SoundAlertMode.WHEN_VOICE_DISABLED && !willSpeakVoice)
                    if (shouldPlaySound) {
                        soundManager.playSound(
                            soundType = soundType,
                            volume = voiceSettings.soundVolume,
                            vibrate = voiceSettings.vibrateWithSound,
                            keepFocusForCallback = willSpeakVoice,
                            onFinished = {
                                if (willSpeakVoice) {
                                    ttsManager.speakPayment(parseResult.senderName, parseResult.amount, isRemote = false)
                                }
                            }
                        )
                    } else if (willSpeakVoice) {
                        ttsManager.speakPayment(parseResult.senderName, parseResult.amount, isRemote = false)
                    }

                    // Update Home Screen Widget
                    YapeTotalWidgetProvider.updateAllWidgets(applicationContext)

                    // Send local broadcast event
                    val broadcastIntent = Intent(ACTION_YAPE_PAYMENT_PROCESSED).apply {
                        putExtra(EXTRA_SENDER, parseResult.senderName)
                        putExtra(EXTRA_AMOUNT, parseResult.amount)
                        setPackage(applicationContext.packageName)
                    }
                    sendBroadcast(broadcastIntent)
                }
            }

            is ParseResult.Ignored -> {
                Log.d(TAG, "Notification ignored: ${parseResult.reason}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "YapeNotiListener"
        const val ACTION_YAPE_PAYMENT_PROCESSED = "com.example.ACTION_YAPE_PAYMENT_PROCESSED"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_AMOUNT = "extra_amount"

        /**
         * Paquetes de aplicaciones financieras oficiales autorizadas para generar cobros.
         * Cualquier app de SMS (Google Messages, Samsung Messages), WhatsApp, Telegram, etc.,
         * queda expresamente excluida para evitar fraudes y lecturas accidentales.
         */
        val ALLOWED_PAYMENT_PACKAGES = setOf(
            "com.bcp.innovacxion.yapeapp",   // Yape Oficial
            "com.bcp.bank.bcp",              // BCP Banca Móvil
            "pe.interbank.mobilebanking",    // Interbank (Plin)
            "com.bbva.pe",                   // BBVA Perú (Plin)
            "pe.com.scotiabank.bancamovil",  // Scotiabank (Plin)
            "com.banbif.bancamovil"          // BanBif (Plin)
        )

        fun isAllowedPaymentApp(
            packageName: String,
            captureYape: Boolean = true,
            captureBcp: Boolean = true,
            capturePlin: Boolean = true
        ): Boolean {
            val lower = packageName.lowercase()

            val isYapePkg = lower == "com.bcp.innovacxion.yapeapp" || lower.contains("yape")
            if (isYapePkg) return captureYape

            val isBcpPkg = lower == "com.bcp.bank.bcp" || lower.contains("bcp")
            if (isBcpPkg) return captureBcp

            val isPlinPkg = lower == "pe.interbank.mobilebanking" ||
                    lower == "com.bbva.pe" ||
                    lower == "pe.com.scotiabank.bancamovil" ||
                    lower == "com.banbif.bancamovil" ||
                    lower.contains("plin")
            if (isPlinPkg) return capturePlin

            return false
        }

        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (flat != null && flat.isNotEmpty()) {
                val names = flat.split(":").toTypedArray()
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == pkgName) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
