package com.example.service.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.preferences.StorePreferences
import com.example.service.remote.FcmNotificationReceiverService
import com.example.widget.YapeTotalWidgetProvider
import kotlinx.coroutines.launch

/**
 * Receiver that listens to BOOT_COMPLETED and QUICKBOOT_POWERON to restore services
 * and ensure Brynn starts listening to transactions right after device boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Boot event received: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action ||
            "android.intent.action.QUICKBOOT_POWERON" == action ||
            "com.htc.intent.action.QUICKBOOT_POWERON" == action
        ) {
            try {
                val storePreferences = StorePreferences.getInstance(context)
                val config = storePreferences.config.value
                if (config.storeCode.isNotBlank()) {
                    FcmNotificationReceiverService.subscribeToStoreTopic(config.storeCode)
                }

                YapeTotalWidgetProvider.updateAllWidgets(context)

                Log.d(TAG, "Boot initialization completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing on boot", e)
            }
        } else if ("com.example.ACTION_INSERT_MOCK_DATA" == action) {
            try {
                val db = com.example.data.db.AppDatabase.getDatabase(context)
                val storePreferences = StorePreferences.getInstance(context)
                val config = storePreferences.config.value
                val cal = java.util.Calendar.getInstance()

                val mockData = listOf(
                    Triple("Carlos Mendoza", 12.50, 9),
                    Triple("Lucía Ramos", 35.00, 10),
                    Triple("Jorge Quintana", 8.00, 11),
                    Triple("Rosa Huamán", 65.00, 12),
                    Triple("Miguel Quispe", 18.00, 14),
                    Triple("Fiorella Castro", 85.50, 16),
                    Triple("David Paredes", 22.00, 17)
                )

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    mockData.forEachIndexed { index, (name, amount, hour) ->
                        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                        cal.set(java.util.Calendar.MINUTE, 10 + (index * 7) % 50)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)

                        val tx = com.example.data.model.YapeTransaction(
                            senderName = name,
                            amount = amount,
                            timestamp = cal.timeInMillis,
                            rawNotification = "¡Te yapearon! $name te envió S/ ${String.format(java.util.Locale.US, "%.2f", amount)}",
                            transactionType = "RECEIVED",
                            storeCode = config.storeCode,
                            isStoreTransaction = true
                        )
                        db.transactionDao().insert(tx)
                    }
                    YapeTotalWidgetProvider.updateAllWidgets(context)
                    Log.d(TAG, "Successfully inserted mock transactions for today")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting mock transactions", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

