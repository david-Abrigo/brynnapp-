package com.example.data.fcm

import android.util.Log
import com.example.data.model.YapeTransaction
import com.example.data.preferences.StoreConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FcmSender {
    private const val TAG = "FcmSender"
    private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/${FcmServiceAccountConfig.PROJECT_ID}/messages:send"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun sendPaymentPush(
        transaction: YapeTransaction,
        storeConfig: StoreConfig
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = FcmServiceAccountConfig.getAccessToken()
            if (accessToken.isNullOrBlank()) {
                Log.e(TAG, "Cannot send FCM push: No OAuth2 access token available")
                return@withContext false
            }

            val topic = FcmServiceAccountConfig.sanitizeTopicName(storeConfig.storeCode)

            // High Priority Data Payload
            val dataJson = JSONObject().apply {
                put("type", "yape_payment")
                put("id", transaction.id.toString())
                put("remoteId", transaction.remoteId)
                put("amount", transaction.amount.toString())
                put("senderName", transaction.senderName)
                put("rawNotification", transaction.rawNotification)
                put("timestamp", transaction.timestamp.toString())
                put("formattedAmount", transaction.formattedAmount)
                put("formattedTime", transaction.formattedTime)
                put("storeCode", storeConfig.storeCode)
                put("storeName", storeConfig.storeName)
                put("note", transaction.note)
            }

            val androidConfig = JSONObject().apply {
                put("priority", "HIGH")
                put("direct_boot_ok", true)
            }

            val messageJson = JSONObject().apply {
                put("topic", topic)
                put("data", dataJson)
                put("android", androidConfig)
            }

            val rootPayload = JSONObject().apply {
                put("message", messageJson)
            }

            val request = Request.Builder()
                .url(FCM_SEND_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(rootPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully sent FCM push to topic $topic: $responseBody")
                return@withContext true
            } else {
                Log.e(TAG, "Failed sending FCM push: HTTP ${response.code} $responseBody")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending FCM push", e)
            return@withContext false
        }
    }

    suspend fun sendWorkerRevokedPush(
        targetUserId: String,
        storeCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = FcmServiceAccountConfig.getAccessToken()
            if (accessToken.isNullOrBlank()) {
                Log.e(TAG, "Cannot send worker_revoked push: No OAuth2 access token available")
                return@withContext false
            }

            val topic = FcmServiceAccountConfig.sanitizeTopicName(storeCode)

            val dataJson = JSONObject().apply {
                put("type", "worker_revoked")
                put("targetUserId", targetUserId)
                put("storeCode", storeCode)
                put("timestamp", System.currentTimeMillis().toString())
            }

            val androidConfig = JSONObject().apply {
                put("priority", "HIGH")
                put("direct_boot_ok", true)
            }

            val messageJson = JSONObject().apply {
                put("topic", topic)
                put("data", dataJson)
                put("android", androidConfig)
            }

            val rootPayload = JSONObject().apply {
                put("message", messageJson)
            }

            val request = Request.Builder()
                .url(FCM_SEND_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(rootPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully sent worker_revoked FCM push to topic $topic")
                return@withContext true
            } else {
                Log.e(TAG, "Failed sending worker_revoked FCM push: HTTP ${response.code} $responseBody")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending worker_revoked FCM push", e)
            return@withContext false
        }
    }
}
