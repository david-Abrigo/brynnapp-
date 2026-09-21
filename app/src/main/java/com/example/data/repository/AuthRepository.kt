package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AuthProvider
import com.example.data.model.AuthUser
import com.example.data.preferences.AuthPreferences
import com.example.data.preferences.StorePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val context: Context,
    private val authPreferences: AuthPreferences = AuthPreferences.getInstance(context),
    private val storePreferences: StorePreferences = StorePreferences.getInstance(context)
) {
    val currentUser: StateFlow<AuthUser> = authPreferences.currentUser

    init {
        val user = authPreferences.currentUser.value
        if (user.provider == AuthProvider.GUEST || user.id.startsWith("guest_")) {
            Log.d(TAG, "Legacy guest session found, clearing to enforce mandatory login")
            authPreferences.clearSession()
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signInWithEmail(email: String, pass: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val config = storePreferences.config.value
            val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
            val anonKey = config.supabaseAnonKey.trim()

            val jsonBody = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }

            val request = Request.Builder()
                .url("$cleanUrl/auth/v1/token?grant_type=password")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(bodyString)
                val userObj = json.optJSONObject("user")
                val token = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userId = userObj?.optString("id") ?: UUID.randomUUID().toString()
                val userMeta = userObj?.optJSONObject("user_metadata")
                val displayName = userMeta?.optString("display_name")
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                val userPlan = fetchUserPlan(cleanUrl, anonKey, userId, token)

                val authUser = AuthUser(
                    id = userId,
                    email = email.trim(),
                    displayName = displayName,
                    provider = AuthProvider.EMAIL,
                    isLoggedIn = true,
                    token = token,
                    refreshToken = refreshToken,
                    plan = userPlan
                )
                authPreferences.saveUser(authUser)
                syncUserStoreOnLogin(cleanUrl, anonKey, userId, token)
                Result.success(authUser)
            } else {
                val errorMsg = parseErrorMessage(bodyString, "Error al iniciar sesión (${response.code})")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail failed", e)
            Result.failure(Exception("Error de conexión: ${e.localizedMessage ?: "No se pudo contactar al servidor"}"))
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String, whatsapp: String = ""): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val config = storePreferences.config.value
            val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
            val anonKey = config.supabaseAnonKey.trim()

            val jsonBody = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                put("data", JSONObject().apply {
                    put("display_name", displayName.trim())
                    put("whatsapp", whatsapp.trim())
                })
            }

            val request = Request.Builder()
                .url("$cleanUrl/auth/v1/signup")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(bodyString)
                val userObj = json.optJSONObject("user") ?: json
                val token = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userId = userObj.optString("id", UUID.randomUUID().toString())

                val authUser = AuthUser(
                    id = userId,
                    email = email.trim(),
                    displayName = displayName.trim().ifEmpty { email.substringBefore("@") },
                    whatsapp = whatsapp.trim(),
                    provider = AuthProvider.EMAIL,
                    isLoggedIn = true,
                    token = token,
                    refreshToken = refreshToken,
                    plan = "FREE"
                )
                authPreferences.saveUser(authUser)
                syncUserStoreOnLogin(cleanUrl, anonKey, userId, token)
                Result.success(authUser)
            } else {
                val errorMsg = parseErrorMessage(bodyString, "Error al registrar usuario (${response.code})")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUpWithEmail failed", e)
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    suspend fun signInWithGoogle(
        googleEmail: String,
        name: String,
        photo: String = "",
        idToken: String? = null
    ): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = googleEmail.trim().lowercase()
            val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }

            val config = storePreferences.config.value
            val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
            val anonKey = config.supabaseAnonKey.trim()

            // Si contamos con idToken de Google y configuración de Supabase, autenticamos directamente con Supabase
            if (!idToken.isNullOrBlank() && cleanUrl.isNotBlank() && anonKey.isNotBlank()) {
                val jsonBody = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", idToken)
                }

                val request = Request.Builder()
                    .url("$cleanUrl/auth/v1/token?grant_type=id_token")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val userObj = json.optJSONObject("user")
                    val token = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val userId = userObj?.optString("id") ?: UUID.randomUUID().toString()
                    val userMeta = userObj?.optJSONObject("user_metadata")
                    val resolvedDisplayName = userMeta?.optString("full_name")
                        ?: userMeta?.optString("name")
                        ?: cleanName
                    val resolvedPhoto = userMeta?.optString("avatar_url")
                        ?: userMeta?.optString("picture")
                        ?: photo

                    val userPlan = fetchUserPlan(cleanUrl, anonKey, userId, token)

                    val authUser = AuthUser(
                        id = userId,
                        email = cleanEmail,
                        displayName = resolvedDisplayName,
                        photoUrl = resolvedPhoto,
                        provider = AuthProvider.GOOGLE,
                        isLoggedIn = true,
                        token = token,
                        refreshToken = refreshToken,
                        plan = userPlan
                    )
                    authPreferences.saveUser(authUser)
                    syncUserStoreOnLogin(cleanUrl, anonKey, userId, token)
                    return@withContext Result.success(authUser)
                } else {
                    val errorMsg = parseErrorMessage(bodyString, "Error al autenticar con Supabase (${response.code})")
                    Log.e(TAG, "Supabase Google Sign-In failed: $errorMsg, body: $bodyString")
                    return@withContext Result.failure(Exception(errorMsg))
                }
            }

            // Modo fallback local (o para selección simulada / sin idToken)
            val authUser = AuthUser(
                id = "google_" + UUID.nameUUIDFromBytes(cleanEmail.toByteArray()).toString(),
                email = cleanEmail,
                displayName = cleanName,
                photoUrl = photo,
                provider = AuthProvider.GOOGLE,
                isLoggedIn = true,
                token = "local_google_${System.currentTimeMillis()}"
            )
            authPreferences.saveUser(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogle error", e)
            Result.failure(e)
        }
    }

    fun continueAsGuest(): AuthUser {
        val authUser = AuthUser(
            id = "guest_" + UUID.randomUUID().toString().substring(0, 8),
            email = "invitado@notiyape.com",
            displayName = "Modo Invitado",
            provider = AuthProvider.GUEST,
            isLoggedIn = true
        )
        authPreferences.saveUser(authUser)
        return authUser
    }

    suspend fun sendPasswordReset(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val config = storePreferences.config.value
            val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
            val anonKey = config.supabaseAnonKey.trim()

            val jsonBody = JSONObject().apply {
                put("email", email.trim())
            }

            val request = Request.Builder()
                .url("$cleanUrl/auth/v1/recover")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("No se pudo enviar el correo de recuperación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        authPreferences.clearSession(keepEmail = false)
    }

    private val refreshMutex = Mutex()

    fun isJwtExpired(token: String): Boolean {
        if (token.isBlank() || !token.contains(".")) return false
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return false
            val decodedBytes = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp == 0L) return false
            val nowSec = System.currentTimeMillis() / 1000L
            // Considerar expirado si ya venció o si vencerá en menos de 60 segundos
            nowSec >= (exp - 60)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun refreshSession(): Result<AuthUser> = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = authPreferences.currentUser.value
                if (!currentUser.isLoggedIn) {
                    return@withContext Result.failure(Exception("No hay usuario autenticado"))
                }

                val refreshToken = currentUser.refreshToken.trim()
                if (refreshToken.isBlank()) {
                    Log.w(TAG, "No refresh token available, session cannot be refreshed automatically")
                    return@withContext Result.failure(Exception("Sesión caducada sin token de renovación"))
                }

                val config = storePreferences.config.value
                val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
                val anonKey = config.supabaseAnonKey.trim()
                if (cleanUrl.isBlank() || anonKey.isBlank()) {
                    return@withContext Result.failure(Exception("Supabase no configurado"))
                }

                val jsonBody = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }

                val request = Request.Builder()
                    .url("$cleanUrl/auth/v1/token?grant_type=refresh_token")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val newAccessToken = json.optString("access_token", "")
                    val newRefreshToken = json.optString("refresh_token", refreshToken)

                    if (newAccessToken.isNotBlank()) {
                        val updatedUser = currentUser.copy(
                            token = newAccessToken,
                            refreshToken = newRefreshToken,
                            isLoggedIn = true
                        )
                        authPreferences.saveUser(updatedUser)
                        Log.d(TAG, "Supabase session refreshed successfully!")
                        return@withContext Result.success(updatedUser)
                    }
                }
                val errorMsg = parseErrorMessage(bodyString, "Error al renovar sesión (${response.code})")
                Log.w(TAG, "refreshSession failed: $errorMsg ($bodyString)")
                Result.failure(Exception(errorMsg))
            } catch (e: Exception) {
                Log.e(TAG, "refreshSession exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getFreshToken(): String? {
        val user = authPreferences.currentUser.value
        if (!user.isLoggedIn || user.token.isBlank()) return null

        if (!isJwtExpired(user.token)) {
            return user.token
        }

        // Si el token expiró, intentar renovarlo con refresh_token
        if (user.refreshToken.isNotBlank()) {
            val res = refreshSession()
            if (res.isSuccess) {
                return res.getOrNull()?.token
            }
        }

        // Si no hay refresh token o el refresco falló por red/temporalidad, retornar el token actual o null
        // sin destruir la sesión del usuario en segundo plano.
        Log.w(TAG, "JWT is expired and cannot be refreshed currently. Retaining user session.")
        return null
    }

    private fun parseErrorMessage(body: String, fallback: String): String {
        return try {
            val json = JSONObject(body)
            json.optString("error_description", json.optString("msg", json.optString("message", fallback)))
        } catch (_: Exception) {
            fallback
        }
    }

    private fun fetchUserPlan(cleanUrl: String, anonKey: String, userId: String, token: String): String {
        return try {
            val authBearer = token.ifBlank { anonKey }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/profiles?id=eq.$userId&select=plan")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArr = org.json.JSONArray(bodyStr)
                if (jsonArr.length() > 0) {
                    val obj = jsonArr.getJSONObject(0)
                    return obj.optString("plan", "FREE").uppercase()
                }
            }
            "FREE"
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user plan, defaulting to FREE", e)
            "FREE"
        }
    }

    suspend fun updateUserPlan(userId: String, newPlan: String): Boolean = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
        val anonKey = config.supabaseAnonKey.trim()
        if (cleanUrl.isBlank() || anonKey.isBlank() || userId.isBlank()) return@withContext false
        try {
            val user = currentUser.value
            val authBearer = user.token.ifBlank { anonKey }
            val jsonBody = JSONObject().apply {
                put("plan", newPlan)
                put("updated_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
            }
            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/profiles?id=eq.$userId")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $authBearer")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                authPreferences.saveUser(user.copy(plan = newPlan))
                true
            } else {
                Log.w(TAG, "Failed to update profile plan: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile plan", e)
            false
        }
    }

    private fun syncUserStoreOnLogin(cleanUrl: String, anonKey: String, userId: String, token: String) {
        if (cleanUrl.isBlank() || anonKey.isBlank() || userId.isBlank()) return
        try {
            val authBearer = token.ifBlank { anonKey }
            val currentStore = storePreferences.config.value

            // 1. Consultar si el usuario ya tiene una tienda registrada como dueño en Supabase
            val getRequest = Request.Builder()
                .url("$cleanUrl/rest/v1/stores?owner_id=eq.$userId&order=created_at.desc&limit=1")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $authBearer")
                .get()
                .build()

            val response = okHttpClient.newCall(getRequest).execute()
            var existingOwnerStoreCode: String? = null
            var existingOwnerStoreName: String? = null
            var existingOwnerBranches: String? = null

            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArr = org.json.JSONArray(bodyStr)
                if (jsonArr.length() > 0) {
                    val storeObj = jsonArr.getJSONObject(0)
                    existingOwnerStoreCode = storeObj.optString("store_code", "").ifBlank { null }
                    existingOwnerStoreName = storeObj.optString("store_name", "").ifBlank { null }
                    existingOwnerBranches = storeObj.optString("branches", "").ifBlank { null }
                }
            }

            // Si tiene tienda como dueño, guardarla en preferences
            if (!existingOwnerStoreCode.isNullOrBlank()) {
                storePreferences.saveOwnerStoreInfo(
                    code = existingOwnerStoreCode,
                    name = existingOwnerStoreName ?: "Mi Negocio",
                    branches = existingOwnerBranches ?: "Principal,Sucursal 2"
                )
            }

            // CRÍTICO: Comportamiento según el rol activo del dispositivo:
            if (currentStore.deviceRole == com.example.data.model.DeviceRole.SENDER) {
                // Modo Dueño: Si tiene tienda propia, restaurarla como tienda activa
                if (!existingOwnerStoreCode.isNullOrBlank()) {
                    Log.d(TAG, "Restoring owned store for SENDER: $existingOwnerStoreCode for user $userId")
                    val updatedConfig = currentStore.copy(
                        storeCode = existingOwnerStoreCode,
                        storeName = existingOwnerStoreName ?: currentStore.storeName,
                        storeBranches = existingOwnerBranches ?: currentStore.storeBranches
                    )
                    storePreferences.updateConfig(updatedConfig)
                    com.example.service.remote.FcmNotificationReceiverService.subscribeToStoreTopic(existingOwnerStoreCode)
                    return
                }
            } else if (currentStore.deviceRole == com.example.data.model.DeviceRole.RECEIVER) {
                // Modo Receptor / Trabajador:
                // NUNCA sobrescribir con la tienda de dueño.
                // Mantener la tienda donde trabaja actualmente.
                if (currentStore.storeCode.isNotBlank()) {
                    Log.d(TAG, "Preserving active worker store: ${currentStore.storeCode} for user $userId")
                    com.example.service.remote.FcmNotificationReceiverService.subscribeToStoreTopic(currentStore.storeCode)
                    return
                }

                // Si no tiene tienda activa en memoria, verificar si tiene worker store guardado
                val savedWorker = storePreferences.getWorkerStoreInfo()
                if (savedWorker != null && savedWorker.first.isNotBlank()) {
                    Log.d(TAG, "Restoring saved worker store: ${savedWorker.first} for user $userId")
                    val updatedConfig = currentStore.copy(
                        storeCode = savedWorker.first,
                        storeName = savedWorker.second,
                        workerBranchName = savedWorker.third
                    )
                    storePreferences.updateConfig(updatedConfig)
                    com.example.service.remote.FcmNotificationReceiverService.subscribeToStoreTopic(savedWorker.first)
                    return
                }
                return
            }

            // 2. Si no tiene tienda por owner_id, buscar si su perfil tiene una tienda activa asignada
            if (currentStore.storeCode.isBlank() && currentStore.isOnboarded) {
                val profileReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/profiles?id=eq.$userId&select=active_store_code")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $authBearer")
                    .get()
                    .build()
                val profileResp = okHttpClient.newCall(profileReq).execute()
                if (profileResp.isSuccessful) {
                    val pBody = profileResp.body?.string() ?: "[]"
                    val pArr = org.json.JSONArray(pBody)
                    if (pArr.length() > 0) {
                        val activeCode = pArr.getJSONObject(0).optString("active_store_code", "")
                        if (activeCode.isNotBlank()) {
                            val storeReq = Request.Builder()
                                .url("$cleanUrl/rest/v1/stores?store_code=eq.$activeCode&limit=1")
                                .header("apikey", anonKey)
                                .header("Authorization", "Bearer $authBearer")
                                .get()
                                .build()
                            val sResp = okHttpClient.newCall(storeReq).execute()
                            if (sResp.isSuccessful) {
                                val sBody = sResp.body?.string() ?: "[]"
                                val sArr = org.json.JSONArray(sBody)
                                if (sArr.length() > 0) {
                                    val sObj = sArr.getJSONObject(0)
                                    val code = sObj.optString("store_code", "")
                                    val name = sObj.optString("store_name", "")
                                    val branches = sObj.optString("branches", "")
                                    val updatedConfig = currentStore.copy(
                                        storeCode = code,
                                        storeName = name.ifBlank { currentStore.storeName },
                                        storeBranches = branches.ifBlank { currentStore.storeBranches }
                                    )
                                    storePreferences.updateConfig(updatedConfig)
                                    com.example.service.remote.FcmNotificationReceiverService.subscribeToStoreTopic(code)
                                    return
                                }
                            }
                        }
                    }
                }
            }

            // 3. Solo para Modo Dueño (SENDER), registrar tienda propia asegurando no adueñarse de una tienda ajena
            if (currentStore.deviceRole == com.example.data.model.DeviceRole.SENDER && currentStore.storeCode.isNotBlank()) {
                var targetStoreCode = currentStore.storeCode
                var targetStoreName = currentStore.storeName

                // Verificar si ya existe en Supabase y a quién le pertenece
                try {
                    val checkStoreReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/stores?store_code=eq.$targetStoreCode&limit=1")
                        .header("apikey", anonKey)
                        .header("Authorization", "Bearer $authBearer")
                        .get()
                        .build()
                    val checkStoreResp = okHttpClient.newCall(checkStoreReq).execute()
                    if (checkStoreResp.isSuccessful) {
                        val body = checkStoreResp.body?.string() ?: "[]"
                        val arr = org.json.JSONArray(body)
                        if (arr.length() > 0) {
                            val sObj = arr.getJSONObject(0)
                            val storeOwnerId = sObj.optString("owner_id", "")
                            if (storeOwnerId.isNotBlank() && storeOwnerId != userId) {
                                // Esta tienda le pertenece a otro dueño. Prohibido apropiarse de ella.
                                Log.w(TAG, "Tienda $targetStoreCode pertenece al dueño $storeOwnerId. Creando tienda independiente para $userId.")
                                targetStoreCode = "STR_" + java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
                                targetStoreName = "Mi Negocio"
                                storePreferences.updateConfig(currentStore.copy(storeCode = targetStoreCode, storeName = targetStoreName))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error verificando titularidad de tienda en login", e)
                }

                val upsertPayload = JSONObject().apply {
                    put("store_code", targetStoreCode)
                    put("store_name", targetStoreName)
                    put("owner_id", userId)
                    put("branches", currentStore.storeBranches)
                    put("last_active", System.currentTimeMillis())
                }

                val upsertRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/stores")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $authBearer")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(upsertPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                var upsertResp = try {
                    okHttpClient.newCall(upsertRequest).execute()
                } catch (e: Exception) { null }

                if (upsertResp == null || (!upsertResp.isSuccessful && (upsertResp.code == 401 || upsertResp.code == 403))) {
                    val anonUpsertReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/stores")
                        .header("apikey", anonKey)
                        .header("Authorization", "Bearer $anonKey")
                        .header("Content-Type", "application/json")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(upsertPayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    upsertResp = try { okHttpClient.newCall(anonUpsertReq).execute() } catch (e: Exception) { null }
                }

                Log.d(TAG, "Store registered for user $userId with code $targetStoreCode. Result: ${upsertResp?.code}")
                if (upsertResp != null && (upsertResp.isSuccessful || upsertResp.code == 201 || upsertResp.code == 200)) {
                    storePreferences.saveOwnerStoreInfo(targetStoreCode, targetStoreName, currentStore.storeBranches)
                }
                com.example.service.remote.FcmNotificationReceiverService.subscribeToStoreTopic(targetStoreCode)

                // Actualizar owned_store_code en el perfil de Supabase
                try {
                    val profilePatchPayload = JSONObject().apply {
                        put("owned_store_code", targetStoreCode)
                        put("role", "OWNER")
                    }
                    val profilePatchReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/profiles?id=eq.$userId")
                        .header("apikey", anonKey)
                        .header("Authorization", "Bearer $authBearer")
                        .header("Content-Type", "application/json")
                        .patch(profilePatchPayload.toString().toRequestBody(jsonMediaType))
                        .build()
                    val pResp = okHttpClient.newCall(profilePatchReq).execute()
                    if (!pResp.isSuccessful && pResp.code == 400) {
                        val roleOnlyPayload = JSONObject().apply { put("role", "OWNER") }
                        val pReq2 = Request.Builder()
                            .url("$cleanUrl/rest/v1/profiles?id=eq.$userId")
                            .header("apikey", anonKey)
                            .header("Authorization", "Bearer $authBearer")
                            .header("Content-Type", "application/json")
                            .patch(roleOnlyPayload.toString().toRequestBody(jsonMediaType))
                            .build()
                        okHttpClient.newCall(pReq2).execute()
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncUserStoreOnLogin error", e)
        }
    }

    suspend fun refreshUserProfile(): Result<AuthUser> = withContext(Dispatchers.IO) {
        val user = authPreferences.currentUser.value
        if (!user.isLoggedIn || user.id.isBlank()) {
            return@withContext Result.failure(Exception("No hay usuario autenticado"))
        }
        val config = storePreferences.config.value
        val cleanUrl = config.supabaseUrl.trim().removeSuffix("/")
        val anonKey = config.supabaseAnonKey.trim()

        if (cleanUrl.isBlank() || anonKey.isBlank()) {
            return@withContext Result.success(user)
        }

        try {
            val plan = fetchUserPlan(cleanUrl, anonKey, user.id, user.token)
            val updatedUser = user.copy(plan = plan)
            authPreferences.saveUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
