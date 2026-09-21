package com.example.service.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.VoiceSettings
import com.example.data.preferences.VoicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class YapeTtsManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val voicePreferences = VoicePreferences.getInstance(context)
    private var pendingSpeech: String? = null

    private var activeFocusRequest: AudioFocusRequest? = null
    private val ttsScope = CoroutineScope(Dispatchers.Default)
    private var focusTimeoutJob: Job? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
        }
    }

    @Synchronized
    private fun acquireAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { }
                    .build()
                activeFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus for TTS", e)
        }
    }

    @Synchronized
    fun releaseAudioFocus() {
        focusTimeoutJob?.cancel()
        focusTimeoutJob = null
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                    activeFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            Log.d(TAG, "Audio focus released after TTS playback")
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus for TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "PE"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to generic Spanish
                tts?.setLanguage(Locale("es", "ES"))
            }
            isInitialized = true
            Log.d(TAG, "TTS Initialized successfully")

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS playback started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS playback finished: $utteranceId")
                    releaseAudioFocus()
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS playback error: $utteranceId")
                    releaseAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS playback error code $errorCode for: $utteranceId")
                    releaseAudioFocus()
                }
            })

            pendingSpeech?.let { text ->
                speakText(text, voicePreferences.loadSettings())
                pendingSpeech = null
            }
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    fun speakPayment(senderName: String, amount: Double, isRemote: Boolean = false) {
        val settings = voicePreferences.loadSettings()
        if (!settings.voiceEnabled) {
            Log.d(TAG, "Voice announcements are disabled globally")
            return
        }

        if (isRemote && !settings.announceRemoteNotiyape) {
            Log.d(TAG, "Voice announcements for remote payments are disabled in Brynn settings")
            return
        }

        if (!isRemote && !settings.announceLocalYape) {
            Log.d(TAG, "Voice announcements for local Yape notifications are disabled in settings")
            return
        }

        val speechText = buildSpeechMessage(senderName, amount, settings.messageTemplate)
        speakText(speechText, settings)
    }

    fun speakPreview(customText: String? = null) {
        val settings = voicePreferences.loadSettings()
        val textToSpeak = customText ?: buildSpeechMessage("Juan Pérez", 35.50, settings.messageTemplate)
        speakText(textToSpeak, settings)
    }

    fun speakText(text: String, settings: VoiceSettings) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not ready yet, queuing speech: $text")
            pendingSpeech = text
            initTts()
            return
        }

        try {
            acquireAudioFocus()

            // Safety timeout to guarantee audio focus is released even if TTS engine stalls
            focusTimeoutJob?.cancel()
            focusTimeoutJob = ttsScope.launch {
                delay(12000L)
                Log.w(TAG, "TTS safety timeout reached: force releasing audio focus")
                releaseAudioFocus()
            }

            tts?.apply {
                setSpeechRate(settings.speechRate)
                setPitch(settings.pitch)

                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume.coerceIn(0.1f, 1.0f))
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
                }

                val utteranceId = UUID.randomUUID().toString()
                val result = speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS speak failed with code: $result")
                    releaseAudioFocus()
                } else {
                    Log.d(TAG, "TTS speak requested for: '$text' (result=$result)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: $text", e)
            releaseAudioFocus()
        }
    }

    private fun buildSpeechMessage(senderName: String, amount: Double, template: String): String {
        val amountInWords = formatAmountForSpeech(amount)
        val spokenSender = cleanSenderForSpeech(senderName)
        val timeNow = SimpleDateFormat("hh:mm a", Locale("es", "PE")).format(Date())
        val storeName = try {
            com.example.data.preferences.StorePreferences(context).loadConfig().storeName.ifBlank { "Mi Negocio" }
        } catch (_: Exception) {
            "Mi Negocio"
        }

        var message = template
            .replace("{monto} soles", amountInWords, ignoreCase = true)
            .replace("{monto} sol", amountInWords, ignoreCase = true)
            .replace("{monto}", amountInWords)
            .replace("{emisor}", spokenSender)
            .replace("{hora}", timeNow)
            .replace("{negocio}", storeName, ignoreCase = true)
            .replace("{tienda}", storeName, ignoreCase = true)
            .replace("soles soles", "soles", ignoreCase = true)
            .replace("sol sol", "sol", ignoreCase = true)

        // If template doesn't contain placeholders, append them logically
        if (!template.contains("{monto}") && !template.contains("{emisor}")) {
            message = "$template. $amountInWords de $spokenSender"
        }

        return message.trim()
    }

    companion object {
        private const val TAG = "YapeTtsManager"

        @Volatile
        private var INSTANCE: YapeTtsManager? = null

        fun getInstance(context: Context): YapeTtsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = YapeTtsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun formatAmountForSpeech(amount: Double): String {
            val wholePart = amount.toLong()
            val cents = Math.round((amount - wholePart) * 100).toInt()

            return when {
                // Caso 1: Solo céntimos (ej. 0.50 -> "50 céntimos", 0.05 -> "5 céntimos", 0.01 -> "1 céntimo")
                wholePart == 0L -> {
                    if (cents == 1) {
                        "1 céntimo"
                    } else if (cents > 0) {
                        "$cents céntimos"
                    } else {
                        "0 soles"
                    }
                }
                // Caso 2: 1 sol (o 1 sol con céntimos -> "un sol con 50 céntimos" / "un sol")
                wholePart == 1L -> {
                    if (cents == 1) {
                        "un sol con 1 céntimo"
                    } else if (cents > 0) {
                        "un sol con $cents céntimos"
                    } else {
                        "un sol"
                    }
                }
                // Caso 3: Mayor a 1 sol (ej. 2 soles, 21 soles, etc.)
                else -> {
                    val wholeStr = if (wholePart % 100L == 21L || (wholePart % 10L == 1L && wholePart % 100L != 11L)) {
                        // Opcional para 21 soles -> veintiún soles
                        wholePart.toString()
                    } else {
                        wholePart.toString()
                    }
                    if (cents == 1) {
                        "$wholeStr soles con 1 céntimo"
                    } else if (cents > 0) {
                        "$wholeStr soles con $cents céntimos"
                    } else {
                        "$wholeStr soles"
                    }
                }
            }
        }

        /**
         * Limpia el nombre del emisor para la locución TTS:
         * 1. Corta hasta donde empiece cualquier asterisco '*', sin pronunciar el asterisco ni lo que sigue.
         * 2. Si hay dos apellidos, mantiene únicamente el nombre (o primer nombre) y el primer apellido.
         */
        fun cleanSenderForSpeech(rawName: String): String {
            if (rawName.isBlank()) return "Cliente"

            // 1. Cortar en el primer asterisco
            var text = rawName.substringBefore("*").trim()
            if (text.isBlank()) return "Cliente"

            // Limpiar puntuación residual al final
            text = text.trimEnd('.', ',', '-', '_')

            // 2. Dividir por palabras
            val tokens = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return "Cliente"

            // Si solo tiene 1 o 2 palabras (ej: "Juan", "Juan Pérez"), devolver tal cual
            if (tokens.size <= 2) {
                return tokens.joinToString(" ")
            }

            // Lista de conectores de apellidos compuestos en español
            val lowercasePrepositions = setOf("de", "del", "la", "las", "los", "y", "san", "santa")

            // Si tiene 3 palabras (ej: "Juan Pérez García"):
            // nombre = "Juan", apellido1 = "Pérez" -> resultado: "Juan Pérez"
            // Caso con conector (ej: "Juan De La Cruz"):
            // Si la segunda palabra es conector, agrupamos: "Juan De La Cruz"
            if (tokens.size == 3) {
                return if (lowercasePrepositions.contains(tokens[1].lowercase())) {
                    tokens.joinToString(" ")
                } else {
                    "${tokens[0]} ${tokens[1]}"
                }
            }

            // Si tiene 4 o más palabras (ej: "Juan Carlos Pérez Quispe" o "María Elena Flores Santos"):
            // Tomamos los dos primeros tokens (ej: "Juan Carlos" o "Juan Pérez")
            // Si la tercera palabra es conector (ej: "Juan Carlos De La Vega"), mantenemos hasta el apellido principal
            return if (lowercasePrepositions.contains(tokens[1].lowercase())) {
                // ej: "María Del Carmen Flores ..."
                tokens.take(3).joinToString(" ")
            } else if (lowercasePrepositions.contains(tokens[2].lowercase())) {
                // ej: "Juan Carlos De La Cruz"
                tokens.take(2).joinToString(" ")
            } else {
                // Caso estándar 4 palabras: "Juan Carlos Pérez García" o "Juan Pérez Gómez Díaz" -> "Juan Carlos" o "Juan Pérez"
                // Tomar primer nombre y primer apellido:
                "${tokens[0]} ${tokens[tokens.size - 2]}"
            }
        }
    }

    fun stop() {
        releaseAudioFocus()
        tts?.stop()
    }

    fun shutdown() {
        releaseAudioFocus()
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}
