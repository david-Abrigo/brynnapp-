package com.example.service.remote

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.fcm.FcmServiceAccountConfig
import com.example.data.model.DeviceRole
import com.example.data.model.YapeTransaction
import com.example.data.preferences.StorePreferences
import com.example.widget.YapeTotalWidgetProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmNotificationReceiverService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")
        val storeConfig = StorePreferences.getInstance(applicationContext).config.value
        subscribeToStoreTopic(storeConfig.storeCode)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.d(TAG, "Empty FCM data received")
            return
        }

        val messageType = data["type"]
        val context = applicationContext
        val storePreferences = StorePreferences.getInstance(context)
        val currentConfig = storePreferences.config.value

        // 1. MANEJO DE EXPULSIÓN EN TIEMPO REAL (worker_revoked)
        if (messageType == "worker_revoked") {
            val targetUserId = data["targetUserId"] ?: ""
            val eventStoreCode = data["storeCode"] ?: ""
            val authUser = com.example.data.preferences.AuthPreferences.getInstance(context).currentUser.value

            val isTargetedToMe = targetUserId.isBlank() || (authUser.isLoggedIn && authUser.id.isNotBlank() && targetUserId == authUser.id)
            val isCurrentStore = eventStoreCode.isNotBlank() && (eventStoreCode.equals(currentConfig.storeCode, ignoreCase = true) || currentConfig.storeCode.isBlank())

            if (isTargetedToMe && (isCurrentStore || eventStoreCode.isNotBlank())) {
                val storeToRevoke = eventStoreCode.ifBlank { currentConfig.storeCode }
                Log.w(TAG, "Expulsión recibida para la tienda '$storeToRevoke'. Añadiendo a blacklist y desuscribiendo.")

                // 1. Agregar a la blacklist persistente y remover de tiendas vinculadas
                if (storeToRevoke.isNotBlank()) {
                    storePreferences.addRevokedStoreCode(storeToRevoke)
                    storePreferences.setRevokedStoreCode(storeToRevoke)
                    storePreferences.removeLinkedStore(storeToRevoke)
                    unsubscribeFromStoreTopic(storeToRevoke)
                }
                if (currentConfig.storeCode.isNotBlank() && !currentConfig.storeCode.equals(storeToRevoke, ignoreCase = true)) {
                    storePreferences.addRevokedStoreCode(currentConfig.storeCode)
                    storePreferences.removeLinkedStore(currentConfig.storeCode)
                    unsubscribeFromStoreTopic(currentConfig.storeCode)
                }

                // 2. Marcar como revocado en preferencias
                storePreferences.updateWorkerSchedule(
                    scheduleEnabled = false,
                    startTime = "00:00",
                    endTime = "23:59",
                    days = "ALL",
                    isRevoked = true
                )
                val storeNameForDialog = currentConfig.storeName.ifBlank { "el negocio" }
                storePreferences.updateConfig(
                    currentConfig.copy(
                        workerIsRevoked = true,
                        storeName = storeNameForDialog
                    )
                )

                // 3. Mostrar notificación al trabajador informando la desvinculación
                RemoteAlertManager.getInstance(context).showRevokedNotification(
                    storeName = storeNameForDialog
                )
            }
            return
        }

        if (messageType != "yape_payment") {
            Log.d(TAG, "Non-payment FCM message type: $messageType")
            return
        }

        try {
            val amount = data["amount"]?.toDoubleOrNull() ?: return
            val senderName = data["senderName"] ?: "Cliente"
            val rawNotification = data["rawNotification"] ?: "Yape recibido"
            val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
            val note = data["note"] ?: ""
            val storeCode = data["storeCode"] ?: ""
            val storeName = data["storeName"] ?: "Mi Negocio"
            // Resolver remoteId con múltiples fallbacks (remoteId, transactionId, transaction_id, id si es UUID)
            val rawRemoteId = (data["remoteId"] ?: data["transactionId"] ?: data["transaction_id"] ?: "").trim()
            val candidateId = data["id"]?.trim() ?: ""
            val remoteId = if (rawRemoteId.isNotBlank()) {
                rawRemoteId
            } else if (candidateId.contains("-") && candidateId.length >= 32) {
                candidateId
            } else {
                ""
            }

            // Si el dispositivo es RECEPTOR y está revocado, descartar
            if (currentConfig.deviceRole == DeviceRole.RECEIVER && currentConfig.workerIsRevoked) {
                Log.w(TAG, "FCM Payment descartado: trabajador revocado.")
                return
            }

            val transaction = YapeTransaction(
                amount = amount,
                senderName = senderName,
                rawNotification = rawNotification,
                timestamp = timestamp,
                note = note,
                storeCode = storeCode,
                remoteId = remoteId,
                isSynced = remoteId.isNotBlank()
            )

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val existing = com.example.util.TransactionDeduplicator.findDuplicate(
                    dao = db.transactionDao(),
                    timestamp = timestamp,
                    amount = amount,
                    senderName = senderName,
                    rawNotification = rawNotification,
                    storeCode = storeCode,
                    remoteId = remoteId
                )

                if (existing != null) {
                    // YA EXISTE: Vincular remoteId si el existente no lo tenía
                    if (existing.remoteId.isBlank() && remoteId.isNotBlank()) {
                        db.transactionDao().linkRemoteId(existing.id, remoteId, System.currentTimeMillis())
                    }
                    Log.d(TAG, "FCM Yape duplicado descartado: S/ $amount de $senderName (coincide con localId=${existing.id}, remoteId=${existing.remoteId})")
                    // IMPORTANTE: NO insertar y NO disparar alerta sonora/TTS para cobros ya registrados
                    return@launch
                }

                // NUEVO COBRO: Insertar en Room y actualizar widgets
                val insertedId = db.transactionDao().insert(transaction)
                Log.d(TAG, "Inserted incoming FCM Yape transaction: S/ $amount from $senderName for store $storeCode (remoteId=$remoteId)")
                try {
                    YapeTotalWidgetProvider.updateAllWidgets(context)
                } catch (_: Exception) {}

                // Reproducir alerta sonora/TTS únicamente si coincide con la tienda activa Y es un cobro reciente (menos de 2 minutos)
                val isCurrentActiveStore = currentConfig.storeCode.trim().equals(storeCode.trim(), ignoreCase = true)
                val now = System.currentTimeMillis()
                val isRecent = (now - timestamp) < 120_000L

                Log.d(TAG, "Device role: ${currentConfig.deviceRole}, activeStore: $isCurrentActiveStore, isRecent=$isRecent (age=${(now - timestamp) / 1000}s)")
                if (currentConfig.deviceRole == DeviceRole.RECEIVER && isCurrentActiveStore) {
                    if (!currentConfig.workerIsRevoked && com.example.util.WorkerScheduleHelper.isConfigWithinSchedule(currentConfig)) {
                        if (isRecent) {
                            RemoteAlertManager.getInstance(context)
                                .triggerRemotePaymentAlert(
                                    transaction.copy(id = insertedId),
                                    currentConfig.copy(
                                        storeName = storeName.ifBlank { currentConfig.storeName },
                                        storeCode = storeCode.ifBlank { currentConfig.storeCode }
                                    )
                                )
                        } else {
                            Log.d(TAG, "Suppressed FCM alert on receiver: Push tardío recibido con más de 2 minutos de retraso (${(now - timestamp) / 1000}s). Se guardó en historial sin alertar con sonido.")
                        }
                    } else {
                        Log.d(TAG, "Suppressed FCM alert on receiver: workerIsRevoked=${currentConfig.workerIsRevoked}, withinSchedule=${com.example.util.WorkerScheduleHelper.isConfigWithinSchedule(currentConfig)}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming FCM message", e)
        }
    }

    companion object {
        private const val TAG = "FcmReceiverService"

        fun subscribeToStoreTopic(storeCode: String) {
            if (storeCode.isBlank()) return
            try {
                val topic = FcmServiceAccountConfig.sanitizeTopicName(storeCode)
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed successfully to FCM topic: $topic")
                        } else {
                            Log.e(TAG, "Failed to subscribe to FCM topic: $topic", task.exception)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating subscription to topic", e)
            }
        }

        fun unsubscribeFromStoreTopic(storeCode: String) {
            if (storeCode.isBlank()) return
            try {
                val topic = FcmServiceAccountConfig.sanitizeTopicName(storeCode)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Unsubscribed from FCM topic: $topic")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from topic", e)
            }
        }
    }
}
