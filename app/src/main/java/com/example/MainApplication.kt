package com.example

import android.app.Application
import android.util.Log
import com.example.data.billing.SubscriptionManager
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Nivel de logs de RevenueCat (DEBUG para desarrollo)
        Purchases.logLevel = LogLevel.DEBUG

        // 2. Configuración oficial de RevenueCat con la API Key pública
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "test_bBwTQNbAysqoOynWUGxTCozKawC")
                .build()
        )

        // 3. Inicializar observador de suscripciones en tiempo real
        SubscriptionManager.initialize()

        Log.d("MainApplication", "RevenueCat SDK inicializado correctamente.")
    }
}
