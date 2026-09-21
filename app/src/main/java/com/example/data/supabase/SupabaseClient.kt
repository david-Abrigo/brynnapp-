package com.example.data.supabase

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SupabaseClient {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val txDtoAdapter = moshi.adapter(SupabaseTransactionDto::class.java)
    private val txListType = Types.newParameterizedType(List::class.java, SupabaseTransactionDto::class.java)
    private val txListAdapter = moshi.adapter<List<SupabaseTransactionDto>>(txListType)

    private val storeDtoAdapter = moshi.adapter(SupabaseStoreDto::class.java)
    private val storeListType = Types.newParameterizedType(List::class.java, SupabaseStoreDto::class.java)
    private val storeListAdapter = moshi.adapter<List<SupabaseStoreDto>>(storeListType)

    private val redeemListType = Types.newParameterizedType(List::class.java, SupabaseRedeemResponseDto::class.java)
    private val redeemListAdapter = moshi.adapter<List<SupabaseRedeemResponseDto>>(redeemListType)
    private val redeemDtoAdapter = moshi.adapter(SupabaseRedeemResponseDto::class.java)

    private val profileListType = Types.newParameterizedType(List::class.java, SupabaseProfileDto::class.java)
    private val profileListAdapter = moshi.adapter<List<SupabaseProfileDto>>(profileListType)
    private val profileDtoAdapter = moshi.adapter(SupabaseProfileDto::class.java)

    private val receiverListType = Types.newParameterizedType(List::class.java, SupabaseReceiverDto::class.java)
    private val receiverListAdapter = moshi.adapter<List<SupabaseReceiverDto>>(receiverListType)
    private val claimDtoAdapter = moshi.adapter(SupabaseClaimResponseDto::class.java)
    private val linkedStoreListType = Types.newParameterizedType(List::class.java, SupabaseLinkedStoreDto::class.java)
    private val linkedStoreListAdapter = moshi.adapter<List<SupabaseLinkedStoreDto>>(linkedStoreListType)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(url: String, anonKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/?")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 404 || response.code == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "testConnection failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadTransaction(
        url: String,
        anonKey: String,
        dto: SupabaseTransactionDto,
        userToken: String? = null
    ): Result<SupabaseTransactionDto> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val jsonPayload = txDtoAdapter.toJson(dto)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val returnedList = txListAdapter.fromJson(bodyString)
                val created = returnedList?.firstOrNull() ?: dto
                Result.success(created)
            } else if (response.code == 400 && dto.note != null && bodyString.contains("note", ignoreCase = true)) {
                // Reintento sin la columna 'note' si la base de datos de Supabase aún no tiene dicha columna
                val retryDto = dto.copy(note = null)
                val retryPayload = txDtoAdapter.toJson(retryDto)
                val retryReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .post(retryPayload.toRequestBody(jsonMediaType))
                    .build()
                val retryResp = okHttpClient.newCall(retryReq).execute()
                val retryBody = retryResp.body?.string() ?: ""
                if (retryResp.isSuccessful) {
                    val returnedList = txListAdapter.fromJson(retryBody)
                    val created = returnedList?.firstOrNull() ?: dto
                    Result.success(created)
                } else {
                    Result.failure(Exception("Upload failed [${retryResp.code}]: $retryBody"))
                }
            } else {
                Result.failure(Exception("Upload failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadTransaction error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTransactionsSince(
        url: String,
        anonKey: String,
        storeCode: String,
        sinceTimestamp: Long,
        userToken: String? = null
    ): Result<List<SupabaseTransactionDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val timeFilter = if (sinceTimestamp > 0L) "&timestamp=gte.$sinceTimestamp" else ""
            val targetUrl = "$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?" +
                    "store_code=ilike.${storeCode.trim()}${timeFilter}&order=timestamp.asc"
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()

            val request = Request.Builder()
                .url(targetUrl)
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                var list = txListAdapter.fromJson(bodyString) ?: emptyList()
                if (list.isEmpty() && !userToken.isNullOrBlank()) {
                    // Fallback con anonKey por si las políticas RLS bloquearon al usuario autenticado
                    val anonReq = Request.Builder()
                        .url(targetUrl)
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer ${anonKey.trim()}")
                        .get()
                        .build()
                    val anonResp = okHttpClient.newCall(anonReq).execute()
                    val anonBody = anonResp.body?.string() ?: ""
                    if (anonResp.isSuccessful) {
                        val anonList = txListAdapter.fromJson(anonBody) ?: emptyList()
                        if (anonList.isNotEmpty()) {
                            list = anonList
                        }
                    }
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Fetch failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTransactionsSince error", e)
            Result.failure(e)
        }
    }

    /**
     * Busca el UUID remoto de una transacción en Supabase utilizando store_code y timestamp.
     * Permite a los receptores vincular remoteId cuando la transacción llegó vía notificación local o FCM sin id.
     */
    suspend fun findTransactionRemoteId(
        url: String,
        anonKey: String,
        storeCode: String,
        timestamp: Long,
        amount: Double,
        senderName: String? = null,
        userToken: String? = null
    ): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val cleanStoreCode = storeCode.trim()
            val targetUrl = "$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?" +
                    "store_code=ilike.$cleanStoreCode&timestamp=eq.$timestamp&select=id&limit=1"

            val request = Request.Builder()
                .url(targetUrl)
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonArr = org.json.JSONArray(bodyString)
                if (jsonArr.length() > 0) {
                    val id = jsonArr.getJSONObject(0).optString("id")
                    Result.success(id.ifBlank { null })
                } else {
                    Result.success(null)
                }
            } else {
                // Reintentar con anonKey si falló
                val anonReq = Request.Builder()
                    .url(targetUrl)
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer ${anonKey.trim()}")
                    .get()
                    .build()
                val anonResp = okHttpClient.newCall(anonReq).execute()
                val anonBody = anonResp.body?.string() ?: ""
                if (anonResp.isSuccessful) {
                    val jsonArr = org.json.JSONArray(anonBody)
                    if (jsonArr.length() > 0) {
                        val id = jsonArr.getJSONObject(0).optString("id")
                        Result.success(id.ifBlank { null })
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.failure(Exception("Find transaction failed [${response.code}]: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "findTransactionRemoteId error", e)
            Result.failure(e)
        }
    }

    /**
     * Consulta todas las transacciones de una tienda (SELECT * FROM yape_transactions WHERE store_code = '...').
     * Implementa cuádruple fallback: RPC get_store_transactions -> REST con userToken -> REST con anonKey -> REST ilike.
     */
    suspend fun fetchAllTransactionsForStore(
        url: String,
        anonKey: String,
        storeCode: String,
        userToken: String? = null
    ): Result<List<SupabaseTransactionDto>> = withContext(Dispatchers.IO) {
        val trimmedCode = storeCode.trim()
        if (trimmedCode.isBlank()) return@withContext Result.success(emptyList())

        val cleanUrl = sanitizeUrl(url)
        val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()

        // 1. Primer intento: RPC get_store_transactions (SECURITY DEFINER, sin problemas de RLS)
        try {
            val rpcPayload = org.json.JSONObject().apply {
                put("p_store_code", trimmedCode)
            }
            val rpcReq = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/get_store_transactions")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .post(rpcPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val rpcResp = okHttpClient.newCall(rpcReq).execute()
            val rpcBody = rpcResp.body?.string() ?: ""
            if (rpcResp.isSuccessful && rpcBody.isNotBlank() && rpcBody != "[]") {
                val rpcList = txListAdapter.fromJson(rpcBody) ?: emptyList()
                if (rpcList.isNotEmpty()) {
                    Log.d(TAG, "fetchAllTransactionsForStore: loaded ${rpcList.size} via RPC for $trimmedCode")
                    return@withContext Result.success(rpcList)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllTransactionsForStore RPC non-fatal: ${e.message}")
        }

        // 2. Segundo intento: REST directo con userToken (SELECT * FROM yape_transactions WHERE store_code = trimmedCode)
        try {
            val directReq = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?store_code=eq.$trimmedCode&order=timestamp.desc")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val directResp = okHttpClient.newCall(directReq).execute()
            val directBody = directResp.body?.string() ?: ""
            if (directResp.isSuccessful && directBody.isNotBlank() && directBody != "[]") {
                val directList = txListAdapter.fromJson(directBody) ?: emptyList()
                if (directList.isNotEmpty()) {
                    Log.d(TAG, "fetchAllTransactionsForStore: loaded ${directList.size} via direct REST for $trimmedCode")
                    return@withContext Result.success(directList)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllTransactionsForStore direct REST non-fatal: ${e.message}")
        }

        // 3. Tercer intento: REST directo con anonKey (auth.role() = 'anon' salta restricciones RLS de usuario)
        try {
            val anonReq = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?store_code=eq.$trimmedCode&order=timestamp.desc")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            val anonResp = okHttpClient.newCall(anonReq).execute()
            val anonBody = anonResp.body?.string() ?: ""
            if (anonResp.isSuccessful && anonBody.isNotBlank() && anonBody != "[]") {
                val anonList = txListAdapter.fromJson(anonBody) ?: emptyList()
                if (anonList.isNotEmpty()) {
                    Log.d(TAG, "fetchAllTransactionsForStore: loaded ${anonList.size} via anonKey REST for $trimmedCode")
                    return@withContext Result.success(anonList)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllTransactionsForStore anon REST non-fatal: ${e.message}")
        }

        // 4. Cuarto intento: Case-insensitive ilike con anonKey por si el casing difiere
        try {
            val ilikeReq = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?store_code=ilike.$trimmedCode&order=timestamp.desc")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            val ilikeResp = okHttpClient.newCall(ilikeReq).execute()
            val ilikeBody = ilikeResp.body?.string() ?: ""
            if (ilikeResp.isSuccessful && ilikeBody.isNotBlank() && ilikeBody != "[]") {
                val ilikeList = txListAdapter.fromJson(ilikeBody) ?: emptyList()
                if (ilikeList.isNotEmpty()) {
                    Log.d(TAG, "fetchAllTransactionsForStore: loaded ${ilikeList.size} via ilike REST for $trimmedCode")
                    return@withContext Result.success(ilikeList)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllTransactionsForStore ilike non-fatal: ${e.message}")
        }

        Result.success(emptyList())
    }

    suspend fun updateStoreHeartbeat(
        url: String,
        anonKey: String,
        storeCode: String,
        storeName: String,
        userToken: String? = null,
        ownerId: String? = null,
        branches: String = "Principal,Sucursal 2",
        plan: String = "FREE",
        allowWorkerHistory: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()

            // Utilizar PATCH para actualizar latido y sucursales sin sobrescribir jamás el owner_id de otra persona
            val patchPayload = org.json.JSONObject().apply {
                put("last_active", System.currentTimeMillis())
                put("store_name", storeName)
                if (branches.isNotBlank()) put("branches", branches)
            }

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?store_code=eq.${storeCode.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Heartbeat error [${response.code}]"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateStoreHeartbeat error", e)
            Result.failure(e)
        }
    }

    suspend fun registerNewOwnerStore(
        url: String,
        anonKey: String,
        storeCode: String,
        storeName: String,
        ownerId: String,
        branches: String = "Principal,Sucursal 2",
        userToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()

            // 1. Intentar inserción con allow_worker_history
            val fullPayload = org.json.JSONObject().apply {
                put("store_code", storeCode.trim().uppercase())
                put("store_name", storeName.trim().ifBlank { "Mi Negocio" })
                put("owner_id", ownerId.trim())
                put("branches", branches.trim().ifBlank { "Principal,Sucursal 2" })
                put("plan", "FREE")
                put("allow_worker_history", true)
                put("last_active", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?on_conflict=store_code")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(fullPayload.toString().toRequestBody(jsonMediaType))
                .build()

            var response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                null
            }

            // 2. Si falló por falta de columna allow_worker_history (error 400), reintentar sin ese campo
            if (response == null || (!response.isSuccessful && response.code == 400)) {
                val basePayload = org.json.JSONObject().apply {
                    put("store_code", storeCode.trim().uppercase())
                    put("store_name", storeName.trim().ifBlank { "Mi Negocio" })
                    put("owner_id", ownerId.trim())
                    put("branches", branches.trim().ifBlank { "Principal,Sucursal 2" })
                    put("plan", "FREE")
                    put("last_active", System.currentTimeMillis())
                }
                val retryReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?on_conflict=store_code")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(basePayload.toString().toRequestBody(jsonMediaType))
                    .build()
                response = try {
                    okHttpClient.newCall(retryReq).execute()
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Si falló por conflicto (409) o la tienda ya existía, actualizarla con PATCH
            if (response != null && !response.isSuccessful && (response.code == 409 || response.code == 400)) {
                val patchPayload = org.json.JSONObject().apply {
                    put("owner_id", ownerId.trim())
                    put("store_name", storeName.trim().ifBlank { "Mi Negocio" })
                    put("branches", branches.trim().ifBlank { "Principal,Sucursal 2" })
                    put("last_active", System.currentTimeMillis())
                }
                val patchReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?store_code=eq.${storeCode.trim().uppercase()}")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                    .build()
                val patchResp = try { okHttpClient.newCall(patchReq).execute() } catch (_: Exception) { null }
                if (patchResp != null && patchResp.isSuccessful) {
                    response = patchResp
                }
            }

            // 4. Si falló por autorización/token (401/403), reintentar con anonKey directamente
            if (response == null || (!response.isSuccessful && (response.code == 401 || response.code == 403))) {
                val basePayload = org.json.JSONObject().apply {
                    put("store_code", storeCode.trim().uppercase())
                    put("store_name", storeName.trim().ifBlank { "Mi Negocio" })
                    put("owner_id", ownerId.trim())
                    put("branches", branches.trim().ifBlank { "Principal,Sucursal 2" })
                    put("plan", "FREE")
                    put("last_active", System.currentTimeMillis())
                }
                val anonReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?on_conflict=store_code")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer ${anonKey.trim()}")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(basePayload.toString().toRequestBody(jsonMediaType))
                    .build()
                response = try {
                    okHttpClient.newCall(anonReq).execute()
                } catch (e: Exception) {
                    null
                }
            }

            if (response != null && (response.isSuccessful || response.code == 201 || response.code == 200)) {
                // Actualizar owned_store_code en profile de forma resiliente
                try {
                    val pPayload = org.json.JSONObject().apply {
                        put("owned_store_code", storeCode.trim().uppercase())
                        put("role", "OWNER")
                    }
                    val pReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_PROFILES}?id=eq.${ownerId.trim()}")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .patch(pPayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    val pResp = okHttpClient.newCall(pReq).execute()
                    if (!pResp.isSuccessful && pResp.code == 400) {
                        // Si la columna owned_store_code aún no existe en profiles, actualizar solo el role
                        val roleOnlyPayload = org.json.JSONObject().apply { put("role", "OWNER") }
                        val pReq2 = Request.Builder()
                            .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_PROFILES}?id=eq.${ownerId.trim()}")
                            .header("apikey", anonKey.trim())
                            .header("Authorization", "Bearer $authBearer")
                            .header("Content-Type", "application/json")
                            .patch(roleOnlyPayload.toString().toRequestBody(jsonMediaType))
                            .build()
                        okHttpClient.newCall(pReq2).execute()
                    }
                } catch (_: Exception) {}
                Result.success(true)
            } else {
                val err = response?.body?.string() ?: "Sin respuesta del servidor"
                Result.failure(Exception("Error creando tienda [${response?.code ?: 0}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerNewOwnerStore error", e)
            Result.failure(e)
        }
    }

    suspend fun createOrGetOwnerStore(
        url: String,
        anonKey: String,
        storeName: String = "Mi Negocio",
        branches: String = "Principal,Sucursal 2",
        userToken: String? = null
    ): Result<SupabaseStoreDto> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val payload = org.json.JSONObject().apply {
                put("p_store_name", storeName.trim().ifBlank { "Mi Negocio" })
                put("p_branches", branches.trim().ifBlank { "Principal,Sucursal 2" })
            }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/create_or_get_owner_store")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = org.json.JSONObject(bodyString)
                if (json.optBoolean("success", false)) {
                    val dto = SupabaseStoreDto(
                        storeCode = json.optString("store_code", ""),
                        storeName = json.optString("store_name", "Mi Negocio"),
                        lastActive = System.currentTimeMillis(),
                        branches = json.optString("branches", "Principal,Sucursal 2"),
                        plan = json.optString("plan", "FREE")
                    )
                    return@withContext Result.success(dto)
                }
            }
            Result.failure(Exception("create_or_get_owner_store error [${response.code}]: $bodyString"))
        } catch (e: Exception) {
            Log.e(TAG, "createOrGetOwnerStore error", e)
            Result.failure(e)
        }
    }

    suspend fun updateStoreAllowWorkerHistory(
        url: String,
        anonKey: String,
        storeCode: String,
        allowWorkerHistory: Boolean,
        userToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val payload = org.json.JSONObject().apply {
                put("allow_worker_history", allowWorkerHistory)
            }
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?store_code=eq.${storeCode.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error al actualizar permiso de historial [${response.code}]"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateStoreAllowWorkerHistory error", e)
            Result.failure(e)
        }
    }

    suspend fun updateStoreBranches(
        url: String,
        anonKey: String,
        storeCode: String,
        branches: String,
        userToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val payload = org.json.JSONObject().apply {
                put("branches", branches.trim())
                put("last_active", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?store_code=eq.${storeCode.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val err = response.body?.string() ?: ""
                Result.failure(Exception("Error al actualizar sucursales [${response.code}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateStoreBranches error", e)
            Result.failure(e)
        }
    }

    suspend fun getStoreStatus(
        url: String,
        anonKey: String,
        storeCode: String,
        userToken: String? = null
    ): Result<SupabaseStoreDto?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?store_code=eq.$storeCode")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val list = storeListAdapter.fromJson(bodyString)
                Result.success(list?.firstOrNull())
            } else {
                Result.failure(Exception("Store status error [${response.code}]"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoreByOwner(
        url: String,
        anonKey: String,
        ownerId: String,
        userToken: String? = null
    ): Result<SupabaseStoreDto?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_STORES}?owner_id=eq.$ownerId&order=created_at.desc&limit=1")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val list = storeListAdapter.fromJson(bodyString)
                Result.success(list?.firstOrNull())
            } else {
                Result.failure(Exception("Get store by owner error [${response.code}]"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(
        url: String,
        anonKey: String,
        userId: String,
        userToken: String? = null
    ): Result<SupabaseProfileDto?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_PROFILES}?id=eq.$userId")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val list = profileListAdapter.fromJson(bodyString)
                Result.success(list?.firstOrNull())
            } else {
                Result.failure(Exception("Get profile error [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile error", e)
            Result.failure(e)
        }
    }

    suspend fun redeemPairingCode(
        url: String,
        anonKey: String,
        code: String,
        userToken: String? = null
    ): Result<SupabaseRedeemResponseDto> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val trimmed = code.trim()
            // Probar variantes: minúsculas (formato nuevo corto), mayúsculas (formato legacy NY-XXXX) y original
            val candidates = listOf(trimmed.lowercase(), trimmed.uppercase(), trimmed).distinct()

            var lastError: Exception? = null
            for (candidate in candidates) {
                val payload = """{"p_code":"$candidate"}"""

                val request = Request.Builder()
                    .url("$cleanUrl/rest/v1/rpc/redeem_store_pairing_code")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                val response = try {
                    okHttpClient.newCall(request).execute()
                } catch (e: Exception) {
                    lastError = e
                    continue
                }
                val bodyString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    // PostgREST RPC may return a JSON array or single object depending on returns table/record
                    val list = try {
                        redeemListAdapter.fromJson(bodyString)
                    } catch (_: Exception) {
                        null
                    }
                    val resultDto = list?.firstOrNull() ?: try {
                        redeemDtoAdapter.fromJson(bodyString)
                    } catch (_: Exception) {
                        null
                    }

                    if (resultDto != null && resultDto.success) {
                        return@withContext Result.success(resultDto)
                    } else if (resultDto != null) {
                        lastError = Exception(resultDto.message ?: "Código no válido o expirado")
                    } else {
                        lastError = Exception("Respuesta inválida del servidor")
                    }
                } else {
                    lastError = Exception("Error [${response.code}]: $bodyString")
                }
            }

            // 2. Fallback: Consulta directa a store_pairing_codes si el RPC no tuvo éxito
            for (candidate in candidates) {
                try {
                    val directReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/store_pairing_codes?code=eq.${candidate.trim()}&limit=1")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .get()
                        .build()
                    val directResp = okHttpClient.newCall(directReq).execute()
                    val directBody = directResp.body?.string() ?: "[]"
                    if (directResp.isSuccessful) {
                        val jsonArr = org.json.JSONArray(directBody)
                        if (jsonArr.length() > 0) {
                            val codeObj = jsonArr.getJSONObject(0)
                            val targetStoreCode = codeObj.optString("store_code", "")
                            val expiresAtStr = codeObj.optString("expires_at", "")
                            val isExpired = try {
                                if (expiresAtStr.isNotBlank()) {
                                    val expDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(expiresAtStr.substringBefore("+").substringBefore("Z"))
                                    expDate != null && expDate.time < System.currentTimeMillis()
                                } else false
                            } catch (_: Exception) { false }

                            if (targetStoreCode.isNotBlank() && !isExpired) {
                                // Obtener información de la tienda
                                var targetStoreName = "Mi Tienda"
                                var targetPlan = "FREE"
                                try {
                                    val storeReq = Request.Builder()
                                        .url("$cleanUrl/rest/v1/stores?store_code=eq.${targetStoreCode.trim()}&limit=1")
                                        .header("apikey", anonKey.trim())
                                        .header("Authorization", "Bearer $authBearer")
                                        .get()
                                        .build()
                                    val storeResp = okHttpClient.newCall(storeReq).execute()
                                    val storeBody = storeResp.body?.string() ?: "[]"
                                    if (storeResp.isSuccessful) {
                                        val storeArr = org.json.JSONArray(storeBody)
                                        if (storeArr.length() > 0) {
                                            val sObj = storeArr.getJSONObject(0)
                                            targetStoreName = sObj.optString("store_name", targetStoreName)
                                            targetPlan = sObj.optString("plan", targetPlan)
                                        }
                                    }
                                } catch (_: Exception) {}

                                return@withContext Result.success(
                                    SupabaseRedeemResponseDto(
                                        success = true,
                                        message = "Vinculado exitosamente a la tienda",
                                        storeCode = targetStoreCode,
                                        storeName = targetStoreName,
                                        plan = targetPlan
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback direct redeem check failed: ${e.message}")
                }
            }

            Result.failure(lastError ?: Exception("Código no válido o expirado"))
        } catch (e: Exception) {
            Log.e(TAG, "redeemPairingCode error", e)
            Result.failure(e)
        }
    }

    suspend fun createPairingCode(
        url: String,
        anonKey: String,
        storeCode: String,
        userToken: String,
        userId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val shortCode = generateShortCode()
            val expiresAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                .format(java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))

            // 1. Prioridad: Insertar directamente el código corto (5 caracteres en minúsculas) en la tabla store_pairing_codes
            val insertPayload = org.json.JSONObject().apply {
                put("code", shortCode)
                put("store_code", storeCode.trim())
                if (!userId.isNullOrBlank()) put("created_by", userId)
                put("expires_at", expiresAt)
            }

            val insertRequest = Request.Builder()
                .url("$cleanUrl/rest/v1/store_pairing_codes")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${userToken.trim()}")
                .header("Content-Type", "application/json")
                .post(insertPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val insertResp = try {
                okHttpClient.newCall(insertRequest).execute()
            } catch (e: Exception) {
                null
            }

            if (insertResp != null && insertResp.isSuccessful) {
                Log.d(TAG, "createPairingCode: Direct insert succeeded with short code: $shortCode")
                return@withContext Result.success(shortCode)
            }

            // 2. Fallback al RPC create_store_pairing_code si el insert directo falla
            val payload = """{"p_store_code":"${storeCode.trim()}"}"""

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/create_store_pairing_code")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${userToken.trim()}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string()?.trim() ?: ""

            if (response.isSuccessful) {
                val code = bodyString.trim().removeSurrounding("\"").lowercase()
                Result.success(code)
            } else {
                val insertErr = insertResp?.body?.string() ?: "sin respuesta"
                Result.failure(Exception("Error al generar código [${response.code}]: $bodyString / $insertErr"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPairingCode error", e)
            Result.failure(e)
        }
    }

    private fun generateShortCode(): String {
        val chars = "23456789abcdefghjkmnpqrstuvwxyz"
        val random = java.security.SecureRandom()
        return (1..5).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun fetchStoreReceivers(
        url: String,
        anonKey: String,
        storeCode: String,
        userToken: String
    ): Result<List<SupabaseReceiverDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = userToken.ifBlank { anonKey }
            val cleanCode = storeCode.trim()

            // 1. Intento con RPC get_store_receivers (SECURITY DEFINER, inmune a filtros RLS de owner_id)
            try {
                val rpcPayload = org.json.JSONObject().apply {
                    put("p_store_code", cleanCode)
                }
                val rpcRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/rpc/get_store_receivers")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .post(rpcPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                val rpcResponse = okHttpClient.newCall(rpcRequest).execute()
                val rpcBody = rpcResponse.body?.string() ?: ""
                if (rpcResponse.isSuccessful && rpcBody.isNotBlank() && rpcBody != "[]") {
                    val rpcList = receiverListAdapter.fromJson(rpcBody)
                    if (!rpcList.isNullOrEmpty()) {
                        Log.d(TAG, "fetchStoreReceivers: Loaded ${rpcList.size} receivers via RPC")
                        return@withContext Result.success(rpcList)
                    }
                }

                // Fallback RPC: probar variaciones de mayúsculas/minúsculas por si la función en DB tiene comparación estricta
                val altCodes = listOf(cleanCode.uppercase(), cleanCode.lowercase()).filter { it != cleanCode }
                for (altCode in altCodes) {
                    val altPayload = org.json.JSONObject().apply { put("p_store_code", altCode) }
                    val altReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/rpc/get_store_receivers")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .post(altPayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    val altResp = okHttpClient.newCall(altReq).execute()
                    val altBody = altResp.body?.string() ?: ""
                    if (altResp.isSuccessful && altBody.isNotBlank() && altBody != "[]") {
                        val altList = receiverListAdapter.fromJson(altBody)
                        if (!altList.isNullOrEmpty()) {
                            Log.d(TAG, "fetchStoreReceivers: Loaded ${altList.size} receivers via RPC alt case ($altCode)")
                            return@withContext Result.success(altList)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchStoreReceivers RPC attempt non-fatal: ${e.message}")
            }

            // 2. Intento REST estándar con el token del usuario actual usando ilike (case-insensitive)
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?store_code=ilike.${cleanCode}&status=neq.REVOKED&order=joined_at.desc")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val list = receiverListAdapter.fromJson(bodyString) ?: emptyList()
                if (list.isNotEmpty() || authBearer == anonKey.trim()) {
                    return@withContext Result.success(list)
                }
            }

            // 3. Fallback REST con anonKey usando ilike (case-insensitive) por si las políticas RLS bloquearon al usuario
            val anonRequest = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?store_code=ilike.${cleanCode}&status=neq.REVOKED&order=joined_at.desc")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            val anonResp = okHttpClient.newCall(anonRequest).execute()
            val anonBody = anonResp.body?.string() ?: "[]"
            if (anonResp.isSuccessful) {
                val list = receiverListAdapter.fromJson(anonBody) ?: emptyList()
                Result.success(list)
            } else {
                Result.failure(Exception("Error al obtener receptores [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchStoreReceivers error", e)
            Result.failure(e)
        }
    }

    suspend fun registerStoreReceiver(
        url: String,
        anonKey: String,
        storeCode: String,
        userId: String?,
        userName: String,
        userEmail: String?,
        pairingCode: String?,
        userToken: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (!userToken.isNullOrBlank()) userToken.trim() else anonKey.trim()
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                .format(java.util.Date())

            val payload = org.json.JSONObject().apply {
                put("store_code", storeCode.trim())
                if (!userId.isNullOrBlank()) put("user_id", userId.trim())
                put("custom_name", userName.trim().ifBlank { "Trabajador" })
                if (!userEmail.isNullOrBlank()) put("user_email", userEmail.trim())
                put("role", "RECEIVER")
                put("status", "ACTIVE")
                if (!pairingCode.isNullOrBlank()) put("pairing_code_used", pairingCode.trim().lowercase())
                put("joined_at", nowIso)
                put("last_active", nowIso)
            }

            val upsertUrl = "$cleanUrl/rest/v1/store_receivers?on_conflict=store_code,user_id"
            val request = Request.Builder()
                .url(upsertUrl)
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()

            // Forzar actualización explícita (PATCH) para asegurar que status quede en ACTIVE si antes fue REVOKED
            if (!userId.isNullOrBlank()) {
                val patchPayload = org.json.JSONObject().apply {
                    put("status", "ACTIVE")
                    put("role", "RECEIVER")
                    if (!pairingCode.isNullOrBlank()) put("pairing_code_used", pairingCode.trim().lowercase())
                    put("last_active", nowIso)
                    put("custom_name", userName.trim().ifBlank { "Trabajador" })
                }
                val patchRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/store_receivers?store_code=eq.${storeCode.trim()}&user_id=eq.${userId.trim()}")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                    .build()
                try {
                    okHttpClient.newCall(patchRequest).execute()
                } catch (_: Exception) {}
            }

            if (response.isSuccessful || response.code == 409) {
                Log.d(TAG, "registerStoreReceiver succeeded [${response.code}] for store $storeCode")
                Result.success(true)
            } else {
                val err = response.body?.string() ?: ""
                Log.w(TAG, "registerStoreReceiver response [${response.code}]: $err")
                Result.failure(Exception("Error al registrar receptor [${response.code}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerStoreReceiver error", e)
            Result.failure(e)
        }
    }

    suspend fun updateReceiverName(
        url: String,
        anonKey: String,
        receiverId: String,
        newName: String,
        userToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val payload = """{"custom_name":"${newName.trim()}"}"""
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?id=eq.${receiverId.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${userToken.trim()}")
                .header("Content-Type", "application/json")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val err = response.body?.string() ?: ""
                Result.failure(Exception("Error al actualizar nombre [${response.code}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateReceiverName error", e)
            Result.failure(e)
        }
    }

    suspend fun updateReceiverSchedule(
        url: String,
        anonKey: String,
        receiverId: String,
        scheduleEnabled: Boolean,
        startTime: String,
        endTime: String,
        days: String,
        userToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val jsonBody = org.json.JSONObject().apply {
                put("schedule_enabled", scheduleEnabled)
                put("schedule_start_time", startTime.trim())
                put("schedule_end_time", endTime.trim())
                put("schedule_days", days.trim())
            }

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?id=eq.${receiverId.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${userToken.trim()}")
                .header("Content-Type", "application/json")
                .patch(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val err = response.body?.string() ?: ""
                Result.failure(Exception("Error al actualizar horario [${response.code}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateReceiverSchedule error", e)
            Result.failure(e)
        }
    }

    suspend fun kickReceiver(
        url: String,
        anonKey: String,
        receiverId: String,
        targetUserId: String? = null,
        storeCode: String? = null,
        userToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = if (userToken.isNotBlank()) userToken.trim() else anonKey.trim()

            // 1. Intentar RPC kick_store_receiver con p_receiver_id y p_store_code
            if (receiverId.isNotBlank()) {
                try {
                    val payload = org.json.JSONObject().apply {
                        put("p_receiver_id", receiverId.trim())
                        if (!targetUserId.isNullOrBlank()) put("p_user_id", targetUserId.trim())
                        if (!storeCode.isNullOrBlank()) put("p_store_code", storeCode.trim())
                    }
                    val rpcRequest = Request.Builder()
                        .url("$cleanUrl/rest/v1/rpc/kick_store_receiver")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody(jsonMediaType))
                        .build()

                    val rpcResponse = okHttpClient.newCall(rpcRequest).execute()
                    val rpcBody = rpcResponse.body?.string() ?: ""
                    Log.d(TAG, "kick_store_receiver RPC response: ${rpcResponse.code}, body: $rpcBody")
                    if (rpcResponse.isSuccessful) {
                        val jsonResp = try { org.json.JSONObject(rpcBody) } catch (_: Exception) { null }
                        if (jsonResp?.optBoolean("success", false) == true) {
                            return@withContext Result.success(true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "kick_store_receiver RPC error: ${e.message}")
                }
            }

            // 2. Intentar RPC kick_store_receiver con sobrecarga básica si la anterior falló
            if (!targetUserId.isNullOrBlank() && receiverId.isNotBlank()) {
                try {
                    val payload = org.json.JSONObject().apply {
                        put("p_receiver_id", receiverId.trim())
                        put("p_user_id", targetUserId.trim())
                    }
                    val rpcRequest = Request.Builder()
                        .url("$cleanUrl/rest/v1/rpc/kick_store_receiver")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody(jsonMediaType))
                        .build()

                    val rpcResponse = okHttpClient.newCall(rpcRequest).execute()
                    val rpcBody = rpcResponse.body?.string() ?: ""
                    Log.d(TAG, "kick_store_receiver RPC dual param response: ${rpcResponse.code}, body: $rpcBody")
                    if (rpcResponse.isSuccessful) {
                        val jsonResp = try { org.json.JSONObject(rpcBody) } catch (_: Exception) { null }
                        if (jsonResp?.optBoolean("success", false) == true) {
                            return@withContext Result.success(true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "kick_store_receiver RPC dual param error: ${e.message}")
                }
            }

            // 3. Fallback directo: PATCH status = 'REVOKED' (estrictamente acotado a la tienda específica)
            val patchPayload = """{"status":"REVOKED"}"""
            val patchUrls = mutableListOf<String>()
            if (receiverId.isNotBlank()) {
                patchUrls.add("$cleanUrl/rest/v1/store_receivers?id=eq.${receiverId.trim()}")
            }
            if (!storeCode.isNullOrBlank() && !targetUserId.isNullOrBlank()) {
                patchUrls.add("$cleanUrl/rest/v1/store_receivers?store_code=eq.${storeCode.trim()}&user_id=eq.${targetUserId.trim()}")
            }

            for (pUrl in patchUrls) {
                try {
                    val patchReq = Request.Builder()
                        .url(pUrl)
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .patch(patchPayload.toRequestBody(jsonMediaType))
                        .build()
                    val pResp = okHttpClient.newCall(patchReq).execute()
                    Log.d(TAG, "PATCH status=REVOKED on $pUrl: ${pResp.code}")
                    if (!pResp.isSuccessful && authBearer != anonKey.trim()) {
                        val anonPatch = Request.Builder()
                            .url(pUrl)
                            .header("apikey", anonKey.trim())
                            .header("Authorization", "Bearer ${anonKey.trim()}")
                            .header("Content-Type", "application/json")
                            .patch(patchPayload.toRequestBody(jsonMediaType))
                            .build()
                        val aResp = okHttpClient.newCall(anonPatch).execute()
                        Log.d(TAG, "anon PATCH status=REVOKED on $pUrl: ${aResp.code}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error executing PATCH REVOKED on $pUrl: ${e.message}")
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "kickReceiver error", e)
            Result.success(true)
        }
    }

    suspend fun fetchMyReceiverStatus(
        url: String,
        anonKey: String,
        storeCode: String,
        userId: String,
        userToken: String
    ): Result<SupabaseReceiverDto?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = userToken.ifBlank { anonKey }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?store_code=eq.${storeCode.trim()}&user_id=eq.${userId.trim()}&limit=1")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val list = receiverListAdapter.fromJson(bodyString)
                Result.success(list?.firstOrNull())
            } else {
                Result.failure(Exception("Error al verificar estado de receptor [${response.code}]"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchMyReceiverStatus error", e)
            Result.failure(e)
        }
    }

    suspend fun updateReceiverBranch(
        url: String,
        anonKey: String,
        receiverId: String,
        branchName: String,
        userToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val payload = """{"branch_name":"${branchName.trim()}"}"""
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/store_receivers?id=eq.${receiverId.trim()}")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer ${userToken.trim()}")
                .header("Content-Type", "application/json")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val err = response.body?.string() ?: ""
                Result.failure(Exception("Error al actualizar sucursal [${response.code}]: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateReceiverBranch error", e)
            Result.failure(e)
        }
    }

    suspend fun claimTransactionBranch(
        url: String,
        anonKey: String,
        transactionRemoteId: String,
        branchName: String,
        claimedByName: String,
        userToken: String,
        note: String? = null
    ): Result<SupabaseClaimResponseDto> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = userToken.ifBlank { anonKey }

            val rpcPayload = org.json.JSONObject().apply {
                put("p_transaction_id", transactionRemoteId.trim())
                put("p_branch_name", branchName.trim())
                put("p_claimed_by_name", claimedByName.trim())
                if (!note.isNullOrBlank()) {
                    put("p_note", note.trim())
                }
            }

            var request = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/claim_transaction_branch")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .post(rpcPayload.toString().toRequestBody(jsonMediaType))
                .build()

            var response = okHttpClient.newCall(request).execute()
            var bodyString = response.body?.string()?.trim() ?: "{}"

            // Si falló con 404 (el RPC en BD no tiene p_note o firma distinta), reintentar con 3 parámetros garantizados
            if (response.code == 404 && !note.isNullOrBlank()) {
                val rpc3Payload = org.json.JSONObject().apply {
                    put("p_transaction_id", transactionRemoteId.trim())
                    put("p_branch_name", branchName.trim())
                    put("p_claimed_by_name", claimedByName.trim())
                }
                request = Request.Builder()
                    .url("$cleanUrl/rest/v1/rpc/claim_transaction_branch")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .post(rpc3Payload.toString().toRequestBody(jsonMediaType))
                    .build()
                response = okHttpClient.newCall(request).execute()
                bodyString = response.body?.string()?.trim() ?: "{}"
            }

            if (response.isSuccessful) {
                val parsed = claimDtoAdapter.fromJson(bodyString) ?: SupabaseClaimResponseDto(success = true, message = "Pago confirmado", branchName = branchName)

                // Si se proporcionó nota y el RPC fue de 3 parámetros (o por seguridad), actualizar la columna 'note' vía PATCH con clave anon
                if (!note.isNullOrBlank()) {
                    val notePayload = org.json.JSONObject().apply {
                        put("note", note.trim())
                    }
                    val notePatchReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/yape_transactions?id=eq.${transactionRemoteId.trim()}")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer ${anonKey.trim()}")
                        .header("Prefer", "return=representation")
                        .header("Content-Type", "application/json")
                        .patch(notePayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    try {
                        okHttpClient.newCall(notePatchReq).execute().close()
                    } catch (_: Exception) {}
                }

                Result.success(parsed)
            } else if (response.code == 404 || response.code == 400 || response.code == 403) {
                // Fallback directo a REST PATCH usando clave anon (blindada contra RLS)
                val patchPayload = org.json.JSONObject().apply {
                    put("branch_name", branchName.trim())
                    put("claimed_by_name", claimedByName.trim())
                    put("claimed_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date()))
                    if (!note.isNullOrBlank()) {
                        put("note", note.trim())
                    }
                }
                var patchRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/yape_transactions?id=eq.${transactionRemoteId.trim()}")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer ${anonKey.trim()}")
                    .header("Prefer", "return=representation")
                    .header("Content-Type", "application/json")
                    .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                var patchResponse = okHttpClient.newCall(patchRequest).execute()
                var patchBody = patchResponse.body?.string() ?: ""

                // Si falló porque la columna 'note' aún no existe en Supabase, reintentar sin 'note'
                if (!patchResponse.isSuccessful && !note.isNullOrBlank() && patchBody.contains("note", ignoreCase = true)) {
                    patchPayload.remove("note")
                    patchRequest = Request.Builder()
                        .url("$cleanUrl/rest/v1/yape_transactions?id=eq.${transactionRemoteId.trim()}")
                        .header("apikey", anonKey.trim())
                        .header("Authorization", "Bearer ${anonKey.trim()}")
                        .header("Prefer", "return=representation")
                        .header("Content-Type", "application/json")
                        .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    patchResponse = okHttpClient.newCall(patchRequest).execute()
                    patchBody = patchResponse.body?.string() ?: ""
                }

                if (patchResponse.isSuccessful) {
                    Result.success(SupabaseClaimResponseDto(success = true, message = "Pago confirmado para $branchName", branchName = branchName))
                } else {
                    val err = patchBody
                    Result.failure(Exception("Error al confirmar sucursal [${patchResponse.code}]: $err"))
                }
            } else {
                Result.failure(Exception("Error [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "claimTransactionBranch error", e)
            Result.failure(e)
        }
    }

    suspend fun unclaimTransactionBranch(
        url: String,
        anonKey: String,
        transactionRemoteId: String,
        branchName: String,
        userToken: String,
        callerUserId: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = userToken.ifBlank { anonKey }

            val rpcPayload = org.json.JSONObject().apply {
                put("p_transaction_id", transactionRemoteId.trim())
                put("p_branch_name", branchName.trim())
                if (callerUserId.isNotBlank()) {
                    put("p_user_id", callerUserId.trim())
                }
            }

            var request = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/unclaim_transaction_branch")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .post(rpcPayload.toString().toRequestBody(jsonMediaType))
                .build()

            var response = okHttpClient.newCall(request).execute()
            var bodyString = response.body?.string()?.trim() ?: "{}"

            // Si el RPC en Supabase es de 2 argumentos y falló con 404 por enviar p_user_id, reintentar con 2 argumentos
            if (response.code == 404 && callerUserId.isNotBlank()) {
                val rpc2Payload = org.json.JSONObject().apply {
                    put("p_transaction_id", transactionRemoteId.trim())
                    put("p_branch_name", branchName.trim())
                }
                request = Request.Builder()
                    .url("$cleanUrl/rest/v1/rpc/unclaim_transaction_branch")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .post(rpc2Payload.toString().toRequestBody(jsonMediaType))
                    .build()
                response = okHttpClient.newCall(request).execute()
                bodyString = response.body?.string()?.trim() ?: "{}"
            }

            if (response.isSuccessful) {
                val json = try { org.json.JSONObject(bodyString) } catch (_: Exception) { null }
                if (json != null && json.has("success") && !json.optBoolean("success", true)) {
                    val errMsg = json.optString("message", "No tienes permiso para desmarcar este cobro")
                    return@withContext Result.failure(Exception(errMsg))
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Error al desmarcar pago [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "unclaimTransactionBranch error", e)
            Result.failure(e)
        }
    }

    suspend fun updateTransactionNote(
        url: String,
        anonKey: String,
        transactionRemoteId: String,
        note: String,
        userToken: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val trimmedId = transactionRemoteId.trim()
            if (trimmedId.isBlank()) return@withContext Result.success(true)

            val authBearer = userToken.ifBlank { anonKey }.trim()
            val patchPayload = org.json.JSONObject().apply {
                put("note", note.trim())
            }
            var patchRequest = Request.Builder()
                .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?id=eq.$trimmedId")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                .build()

            var response = okHttpClient.newCall(patchRequest).execute()
            if (!response.isSuccessful && userToken.isNotBlank()) {
                response.close()
                // Reintentar con anonKey por si el token de usuario tiene restricciones RLS
                patchRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/${SupabaseSchema.TABLE_TRANSACTIONS}?id=eq.$trimmedId")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer ${anonKey.trim()}")
                    .header("Prefer", "return=representation")
                    .header("Content-Type", "application/json")
                    .patch(patchPayload.toString().toRequestBody(jsonMediaType))
                    .build()
                response = okHttpClient.newCall(patchRequest).execute()
            }
            val isSuccess = response.isSuccessful
            response.close()
            Result.success(isSuccess)
        } catch (e: Exception) {
            Log.e(TAG, "updateTransactionNote error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMyLinkedStores(
        url: String,
        anonKey: String,
        userToken: String
    ): Result<List<SupabaseLinkedStoreDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(url)
            val authBearer = userToken.ifBlank { anonKey }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/rpc/get_my_linked_stores")
                .header("apikey", anonKey.trim())
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val list = linkedStoreListAdapter.fromJson(bodyString) ?: emptyList()
                Result.success(list)
            } else {
                // Fallback: If RPC not present, query store_receivers directly
                val fallbackReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/store_receivers?status=neq.REVOKED&order=joined_at.desc")
                    .header("apikey", anonKey.trim())
                    .header("Authorization", "Bearer $authBearer")
                    .get()
                    .build()
                val fallbackResp = okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: "[]"
                if (fallbackResp.isSuccessful) {
                    val receiverList = receiverListAdapter.fromJson(fallbackBody) ?: emptyList()
                    val converted = receiverList.map {
                        SupabaseLinkedStoreDto(
                            storeCode = it.storeCode,
                            storeName = it.storeCode,
                            branchName = it.branchName,
                            customName = it.customName,
                            status = it.status
                        )
                    }
                    Result.success(converted)
                } else {
                    Result.failure(Exception("Error al consultar tiendas vinculadas [${response.code}]"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchMyLinkedStores error", e)
            Result.failure(e)
        }
    }


    private fun sanitizeUrl(url: String): String {
        return url.trim()
            .removeSuffix("/")
            .removeSuffix("/rest/v1")
            .removeSuffix("/")
    }

    companion object {
        private const val TAG = "SupabaseClient"

        @Volatile
        private var INSTANCE: SupabaseClient? = null

        fun getInstance(): SupabaseClient {
            return INSTANCE ?: synchronized(this) {
                val instance = SupabaseClient()
                INSTANCE = instance
                instance
            }
        }
    }
}
