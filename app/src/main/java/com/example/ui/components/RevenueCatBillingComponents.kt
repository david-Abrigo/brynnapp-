package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.billing.SubscriptionManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * Pantalla / Modal de Paywall oficial de RevenueCat en Jetpack Compose.
 * Los textos, precios y diseño visual se configuran en el Dashboard de RevenueCat.
 */
@Composable
fun RevenueCatPaywallDialog(
    onDismiss: () -> Unit,
    onPurchaseSuccess: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Paywall(
            options = PaywallOptions.Builder(dismissRequest = onDismiss)
                .setShouldDisplayDismissButton(true)
                .setListener(object : PaywallListener {
                    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                        SubscriptionManager.refreshCustomerInfo { isPro ->
                            if (isPro) {
                                onPurchaseSuccess?.invoke()
                                onDismiss()
                            }
                        }
                    }

                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        SubscriptionManager.refreshCustomerInfo { isPro ->
                            if (isPro) {
                                onPurchaseSuccess?.invoke()
                                onDismiss()
                            }
                        }
                    }
                })
                .build()
        )
    }
}

/**
 * Pantalla oficial de Centro de Clientes (Customer Center) de RevenueCat.
 * Permite a los usuarios gestionar su suscripción, solicitar soporte y gestionar cancelaciones.
 */
@Composable
fun RevenueCatCustomerCenterDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        CustomerCenter(
            modifier = Modifier.fillMaxSize(),
            onDismiss = onDismiss
        )
    }
}
