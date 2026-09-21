package com.example.service.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.YapeTransaction
import java.util.Calendar
import java.util.Locale

class YapeListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return YapeListRemoteViewsFactory(applicationContext)
    }
}

class YapeListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var transactionList: List<YapeTransaction> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
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

            val storeConfig = com.example.data.preferences.StorePreferences.getInstance(context).config.value
            val dao = AppDatabase.getDatabase(context).transactionDao()
            // Direct synchronous query on the background Binder thread
            transactionList = dao.getTransactionsBetweenSync(startTime, endTime, storeConfig.storeCode)
        } catch (_: Exception) {
            transactionList = emptyList()
        }
    }

    override fun onDestroy() {
        transactionList = emptyList()
    }

    override fun getCount(): Int = transactionList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= transactionList.size) return null

        val views = RemoteViews(context.packageName, R.layout.widget_yape_item)
        val tx = transactionList.getOrNull(position) ?: return null

        views.setTextViewText(R.id.widget_item_sender, tx.senderName.ifBlank { "Cliente" })
        views.setTextViewText(R.id.widget_item_time, tx.formattedTime)
        views.setTextViewText(
            R.id.widget_item_amount,
            String.format(Locale("es", "PE"), "+ S/ %.2f", tx.amount)
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
