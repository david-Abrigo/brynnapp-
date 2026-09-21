package com.example.data.supabase

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Registro de transacción emitido directamente por el WebSocket Realtime de Supabase.
 */
data class RealtimeTxRecord(
    val id: String,
    val storeCode: String,
    val senderName: String,
    val amount: Double,
    val timestamp: Long,
    val transactionType: String = "RECEIVED",
    val rawNotification: String? = null,
    val branchName: String? = null,
    val claimedBy: String? = null,
    val claimedByName: String? = null,
    val claimedAt: String? = null,
    val note: String? = null
)

/**
 * Cliente Supabase Realtime usando OkHttp WebSocket con protocolo Phoenix Channels.
 *
 * Escucha cambios en la tabla `store_receivers` para un store_code especifico
 * y emite un evento a traves de [receiverChangeEvent] para que el ViewModel
 * recargue la lista completa via REST.
 *
 * Ciclo de vida:
 *  - Llamar [connect] cuando el dueno activa el rol SENDER con storeCode valido
 *  - Llamar [disconnect] en onCleared() del ViewModel
 */
class SupabaseRealtimeClient(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val getAuthToken: suspend () -> String?
) {

    private val TAG = "SupabaseRealtime"

    // Emite timestamp cada vez que hay un cambio en store_receivers
    // El ViewModel lo observa y llama loadStoreReceivers() o checkMyReceiverStatus()
    private val _receiverChangeEvent = MutableStateFlow(0L)
    val receiverChangeEvent: StateFlow<Long> = _receiverChangeEvent.asStateFlow()

    // Emite timestamp cada vez que hay un cambio (INSERT, UPDATE) en yape_transactions
    // El ViewModel lo observa y sincroniza transacciones en tiempo real
    private val _transactionChangeEvent = MutableStateFlow(0L)
    val transactionChangeEvent: StateFlow<Long> = _transactionChangeEvent.asStateFlow()

    // Emite el registro completo de la transacción recibido en el payload de postgres_changes
    // Permite actualizar la UI y Room en 0ms sin esperar una petición HTTP adicional
    private val _realtimeTxUpdate = MutableSharedFlow<RealtimeTxRecord>(extraBufferCapacity = 64)
    val realtimeTxUpdate: SharedFlow<RealtimeTxRecord> = _realtimeTxUpdate.asSharedFlow()

    private val _realtimeTxDelete = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val realtimeTxDelete: SharedFlow<String> = _realtimeTxDelete.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket requiere timeout 0
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val isConnected = AtomicBoolean(false)
    private val shouldReconnect = AtomicBoolean(false)
    private val msgRef = AtomicInteger(1)
    private val reconnectAttempts = AtomicInteger(0)

    private var currentStoreCode: String = ""
    private var currentToken: String? = null

    // ─── API publica ───────────────────────────────────────────────────────────

    fun connect(storeCode: String) {
        if (storeCode.isBlank()) return
        if (currentStoreCode == storeCode && isConnected.get()) {
            Log.d(TAG, "Ya conectado a $storeCode, omitiendo")
            return
        }
        // Si habia una conexion anterior con otro storeCode, cerrarla limpiamente
        if (isConnected.get()) disconnect()
        currentStoreCode = storeCode
        shouldReconnect.set(true)
        reconnectAttempts.set(0)
        scope.launch { openConnection() }
    }

    fun disconnect() {
        shouldReconnect.set(false)
        isConnected.set(false)
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
        Log.d(TAG, "Desconectado de Supabase Realtime")
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }

    // ─── Conexion interna ──────────────────────────────────────────────────────

    private suspend fun openConnection() {
        currentToken = getAuthToken()

        val base = supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        val wsUrl = "$base/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"

        Log.d(TAG, "Conectando WebSocket -> $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .header("apikey", anonKey)
            .build()

        webSocket = okHttpClient.newWebSocket(request, PhoenixListener())
    }

    // ─── Phoenix Protocol ──────────────────────────────────────────────────────

    private fun sendMsg(topic: String, event: String, payload: JSONObject = JSONObject()) {
        val ref = msgRef.getAndIncrement().toString()
        val msg = JSONObject().apply {
            put("topic", topic)
            put("event", event)
            put("payload", payload)
            put("ref", ref)
        }
        webSocket?.send(msg.toString())
        Log.v(TAG, "-> $event [$topic]")
    }

    private fun joinChannel(token: String?) {
        val cleanCode = currentStoreCode.trim()
        val topic = "realtime:store_$cleanCode"
        val storeVariants = listOf(cleanCode, cleanCode.lowercase(), cleanCode.uppercase())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")

        val pgChanges = JSONArray().apply {
            put(JSONObject().apply {
                put("event", "*")
                put("schema", "public")
                put("table", "store_receivers")
                put("filter", "store_code=in.($storeVariants)")
            })
            put(JSONObject().apply {
                put("event", "*")
                put("schema", "public")
                put("table", "yape_transactions")
                put("filter", "store_code=in.($storeVariants)")
            })
        }
        val config = JSONObject().apply {
            put("broadcast", JSONObject().apply { put("self", false) })
            put("presence", JSONObject().apply { put("key", "") })
            put("postgres_changes", pgChanges)
        }
        val payload = JSONObject().apply {
            put("config", config)
            if (!token.isNullOrBlank()) put("access_token", token)
        }
        sendMsg(topic, "phx_join", payload)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected.get()) {
                delay(30_000L)
                if (isConnected.get()) {
                    sendMsg("phoenix", "heartbeat")
                    Log.v(TAG, "heartbeat enviado")
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val attempts = reconnectAttempts.getAndIncrement()
            val delayMs = minOf(1000L * (1L shl attempts.coerceAtMost(5)), 60_000L)
            Log.d(TAG, "Reconectando en ${delayMs}ms (intento ${attempts + 1})")
            delay(delayMs)
            if (shouldReconnect.get()) openConnection()
        }
    }

    // ─── WebSocket Listener ────────────────────────────────────────────────────

    inner class PhoenixListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket abierto OK")
            isConnected.set(true)
            reconnectAttempts.set(0)
            joinChannel(currentToken)
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                val event = json.optString("event")
                val topic = json.optString("topic")

                when (event) {
                    "phx_reply" -> {
                        val status = json.optJSONObject("payload")?.optString("status")
                        if (status == "ok") {
                            Log.d(TAG, "Suscripcion Realtime confirmada para $topic")
                        } else if (status == "error") {
                            Log.w(TAG, "Error phx_reply: ${json.optJSONObject("payload")}")
                        }
                    }
                    "postgres_changes" -> {
                        val payload = json.optJSONObject("payload")
                        val data = payload?.optJSONObject("data")
                        val table = data?.optString("table") ?: ""
                        val changeType = data?.optString("type") ?: "UNKNOWN"

                        Log.d(TAG, "Cambio Realtime en tabla: $table ($changeType)")
                        when (table) {
                            "store_receivers" -> {
                                _receiverChangeEvent.value = System.currentTimeMillis()
                            }
                            "yape_transactions" -> {
                                if (changeType.equals("DELETE", ignoreCase = true)) {
                                    val oldRecord = data?.optJSONObject("old_record")
                                    val oldId = oldRecord?.optString("id")
                                    if (!oldId.isNullOrBlank()) {
                                        _realtimeTxDelete.tryEmit(oldId)
                                    }
                                } else {
                                    val recordObj = data?.optJSONObject("record")
                                    if (recordObj != null) {
                                        val id = recordObj.optString("id")
                                        if (id.isNotBlank()) {
                                            val amount = recordObj.optDouble("amount", 0.0).takeIf { it > 0.0 }
                                                ?: (recordObj.optString("amount").toDoubleOrNull() ?: 0.0)
                                            val rec = RealtimeTxRecord(
                                                id = id,
                                                storeCode = recordObj.optString("store_code"),
                                                senderName = recordObj.optString("sender_name"),
                                                amount = amount,
                                                timestamp = recordObj.optLong("timestamp"),
                                                transactionType = recordObj.optString("transaction_type", "RECEIVED"),
                                                rawNotification = recordObj.optString("raw_notification").ifBlank { null },
                                                branchName = recordObj.optString("branch_name").ifBlank { null },
                                                claimedBy = recordObj.optString("claimed_by").ifBlank { null },
                                                claimedByName = recordObj.optString("claimed_by_name").ifBlank { null },
                                                claimedAt = recordObj.optString("claimed_at").ifBlank { null },
                                                note = recordObj.optString("note").ifBlank { null }
                                            )
                                            _realtimeTxUpdate.tryEmit(rec)
                                        }
                                    }
                                }
                                _transactionChangeEvent.value = System.currentTimeMillis()
                            }
                            else -> {
                                _receiverChangeEvent.value = System.currentTimeMillis()
                                _transactionChangeEvent.value = System.currentTimeMillis()
                            }
                        }
                    }
                    "phx_error" -> {
                        Log.w(TAG, "phx_error recibido, reconectando...")
                        isConnected.set(false)
                        scheduleReconnect()
                    }
                    // phx_close, heartbeat_reply, etc. -> ignorar
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando mensaje Realtime: ${e.message}")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket fallo: ${t.message}")
            isConnected.set(false)
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket cerrado: $code $reason")
            isConnected.set(false)
            if (shouldReconnect.get() && code != 1000) scheduleReconnect()
        }
    }
}
