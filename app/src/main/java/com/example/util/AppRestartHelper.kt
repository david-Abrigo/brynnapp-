package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.MainActivity

object AppRestartHelper {
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
        } else {
            val fallbackIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(fallbackIntent)
        }
        if (context is Activity) {
            context.finish()
        }
        Runtime.getRuntime().exit(0)
    }
}
