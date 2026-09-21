package com.example.data.fcm

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

object FcmServiceAccountConfig {
    private const val TAG = "FcmServiceAccount"

    const val PROJECT_ID = "brynn-78e83"
    const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@brynn-78e83.iam.gserviceaccount.com"
    private const val TOKEN_URI = "https://oauth2.googleapis.com/token"
    private const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

    private const val PRIVATE_KEY_PEM = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC8qsaNpB+eLucV
GeiTNzoiGT8GsKRCVOVTKkVrDvonxzwjToHDL6lVg6oJbnPSQGyG4svAvdC5Y0Po
PY9C4ixXRkJr8UPWI/XrZMPyBzBk3BquBnKD4lru3qhRpXCW+YjysW/aL7eUF/6s
nAwbz+x4+GtoDRHaSesgbkYt8AynT6mFd9ZSBN7UIUSgc3coYOADlCvnEubiQD2P
3xfDLIp8/hMSQBXuUKJKYWhNtV4tYjvkqKc9vL+cV1VYx0fIOnwaQtQ3TApJPLHh
YzWbb2b2Ocw88wm7/tOCnMC31uyfa9up2pXj4zKaecr5I6wnAF3PCDI7uWoGb44u
Vt95rr/nAgMBAAECggEABAAkXlkuPnQaJvgPVsfOA25rnWBeK0s84vWQ1pimX0o5
5o8XRfnwakV0yTxpV6T8Es7OU+jyexaKwxWYaxT8AbcWA3nwiU2RPQuRnW6KQn6U
UOpcFUXVzcjfjX5SPoajYRoNbXVOPDqssMuiShivl/pp30v0+/RN+GMTjo0k3OCp
4wMEoEUnu5IlOknB5S/W2eHyIlaTgzmP/+9YGrThTaehKaQYo1ho5o4fSxUCubhj
66Jzm45ixLtu2xqHHI9a7TP5ZGfW0bcXv58w0oXJ0epDaARYkxs77PRKVr3QYrC7
GNtnV4EI8sY6G6Cd+RwbLGaUR8YItvpjKUfWDlrcaQKBgQDqTgTK8w6PZYuDsVVZ
R7gz3JwhoJShazQOeTN6qJc5IPZUieiGZFIXhQYLFKSrUKlH7iG8Qi1CSLIwsRbQ
3kHMuSm6aJyCKFAv91BnSb3AQHjUXiDOpT96MO5EZs0Cd3stuxVsZZt145lu6u7m
BZ3uC3+/brRw5pEypJ+IX2OkkwKBgQDOIvUbWD9xn7te+UCXxym9dVwg0f+uMsAv
Du5qb1Ntge4Mh37+O8E2xkYVlhmjUJLTKvXVThdCS1F2OcOVWt8sa80ITFcgrku5
O2K8UZTZq3vT5X+ncqU+4UT/iNQJ3we+5Sj4kewJYjooAqaxjGZgMjtCSW5BVdNH
ZYx6f6C/3QKBgCZ+YN0y5255oV0mcj4zwTiMAv6nUiPe5/OEyAdb0VRqbO3gploF
+1Kv4RlK7+7RVq5hmhtCyAq+xL+pKHVyPl8zAH8GlsfvdZUbzZLak5kVPoStXjh0
/6PEjDCirwW7uXn+5XskilDo7plBcbRKsfKc4wUBXBW8W5h1YVxXR5iPAoGAb5WU
wNygW01wDBU41fBFs/0XhdynXxCx38dhX9QrgRfYWIfwQobuLExgG8dI0ZMIJ68V
rAW9qXFEA495RBwQuucP8fIBJ/uxme9/nxk6Tin5udZ9p5LzyHoA86KJxV4pvzgl
WYkEEIBPouDsgyfRMzl9woAlYtCmsftSc+0qgzECgYEA0aKWp+d9dvoioN400Wsk
RwV0ZCLk1g0Gy8bM3/V5GcDHP9yP4KBzGYCF6r6Z84YNsXJMeaA4APfPqiSnVLaj
PORZ+ta6Pdcmvxaxq4ZW3I5Hu4WO8i/mfYsA4KIcY4ZBXw3dIi3LwfkTvbT3wJMC
9NADa1YVibJuae2qhnIt4p8=
-----END PRIVATE KEY-----"""

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var tokenExpiryTimestamp: Long = 0

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < (tokenExpiryTimestamp - 60_000)) {
            return@withContext cachedAccessToken
        }

        try {
            val jwt = createSignedJwt()
            val requestBody = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", jwt)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URI)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val token = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                cachedAccessToken = token
                tokenExpiryTimestamp = System.currentTimeMillis() + (expiresIn * 1000)
                Log.d(TAG, "Obtained new FCM OAuth2 Access Token successfully")
                return@withContext token
            } else {
                Log.e(TAG, "Failed to get access token: HTTP ${response.code} $responseBody")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OAuth2 access token for FCM", e)
            return@withContext null
        }
    }

    private fun createSignedJwt(): String {
        val nowSeconds = System.currentTimeMillis() / 1000
        val expSeconds = nowSeconds + 3600

        val headerJson = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }.toString()

        val claimsJson = JSONObject().apply {
            put("iss", CLIENT_EMAIL)
            put("scope", FCM_SCOPE)
            put("aud", TOKEN_URI)
            put("exp", expSeconds)
            put("iat", nowSeconds)
        }.toString()

        val encodedHeader = base64Url(headerJson.toByteArray(Charsets.UTF_8))
        val encodedClaims = base64Url(claimsJson.toByteArray(Charsets.UTF_8))
        val dataToSign = "$encodedHeader.$encodedClaims"

        val privateKey = getPrivateKeyFromPem(PRIVATE_KEY_PEM)
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(dataToSign.toByteArray(Charsets.UTF_8))
        }.sign()

        val encodedSignature = base64Url(signature)
        return "$dataToSign.$encodedSignature"
    }

    private fun getPrivateKeyFromPem(pem: String): PrivateKey {
        val cleanPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    private fun base64Url(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun sanitizeTopicName(storeCode: String): String {
        val clean = storeCode.trim().replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
        return "tienda_$clean"
    }
}
