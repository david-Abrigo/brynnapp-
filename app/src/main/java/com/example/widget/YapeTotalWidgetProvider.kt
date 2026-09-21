package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.preferences.StorePreferences
import com.example.service.widget.YapeListWidgetService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class YapeTotalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_WIDGET || 
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, YapeTotalWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_transactions_list)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.widget.ACTION_UPDATE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_yape_total)

            // Click entire widget to open MainActivity
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            // Click refresh button to broadcast update
            val refreshIntent = Intent(context, YapeTotalWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_hint, refreshPendingIntent)

            // Bind List Adapter with unique data URI for widget instance
            val serviceIntent = Intent(context, YapeListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("content://com.example.widget/widget_$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_transactions_list, serviceIntent)
            views.setEmptyView(R.id.widget_transactions_list, R.id.widget_empty_text)

            // Immediate initial push to ensure widget renders without waiting
            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {}

            // Load data in background IO thread and refresh view
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTime = calendar.timeInMillis

                    val storeConfig = StorePreferences.getInstance(context).config.value
                    val currentStoreCode = storeConfig.storeCode
                    val dao = AppDatabase.getDatabase(context).transactionDao()
                    val total = dao.getTotalAmountBetweenSync(startTime, endTime, currentStoreCode)
                    val count = dao.getCountBetweenSync(startTime, endTime, currentStoreCode)

                    views.setTextViewText(
                        R.id.widget_total_amount,
                        String.format(Locale("es", "PE"), "S/ %.2f", total)
                    )
                    views.setTextViewText(
                        R.id.widget_store_name,
                        storeConfig.storeName.ifBlank { "NOTIYAPE" }.uppercase()
                    )
                    views.setTextViewText(
                        R.id.widget_tx_count,
                        "$count yapes"
                    )

                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_transactions_list)
                    }
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_total_amount, "S/ 0.00")
                    views.setTextViewText(R.id.widget_tx_count, "0 yapes")
                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, YapeTotalWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
