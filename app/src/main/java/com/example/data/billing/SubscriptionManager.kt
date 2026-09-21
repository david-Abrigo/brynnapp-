package com.example.data.billing

import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SubscriptionManager {

    private const val TAG = "SubscriptionManager"
    const val ENTITLEMENT_PRO = "brynn_parlante_de_pagos_pro"

    private val _isProActive = MutableStateFlow(false)
    val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    fun initialize() {
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
            handleCustomerInfo(info)
        }
        refreshCustomerInfo()
    }

    fun refreshCustomerInfo(onResult: ((Boolean) -> Unit)? = null) {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error: PurchasesError ->
                Log.e(TAG, "Error al consultar CustomerInfo: ${error.message}")
                onResult?.invoke(_isProActive.value)
            },
            onSuccess = { info: CustomerInfo ->
                val hasAccess = handleCustomerInfo(info)
                onResult?.invoke(hasAccess)
            }
        )
    }

    private fun handleCustomerInfo(info: CustomerInfo): Boolean {
        _customerInfo.value = info
        val isPro = info.entitlements.active[ENTITLEMENT_PRO] != null ||
                info.entitlements[ENTITLEMENT_PRO]?.isActive == true
        _isProActive.value = isPro
        Log.d(TAG, "Estado de suscripción Pro ($ENTITLEMENT_PRO): $isPro")
        return isPro
    }

    /**
     * Vincula el ID de usuario de Supabase con RevenueCat para que sus compras queden asociadas a su cuenta.
     */
    fun loginUser(supabaseUserId: String, onResult: ((Boolean) -> Unit)? = null) {
        if (supabaseUserId.isBlank()) return
        Log.d(TAG, "Iniciando sesión en RevenueCat con ID: $supabaseUserId")
        Purchases.sharedInstance.logInWith(
            appUserID = supabaseUserId,
            onError = { error: PurchasesError ->
                Log.e(TAG, "Error al iniciar sesión en RevenueCat: ${error.message}")
                onResult?.invoke(false)
            },
            onSuccess = { info: CustomerInfo, created: Boolean ->
                Log.d(TAG, "Sesión iniciada en RevenueCat. Nuevo usuario creado: $created")
                val isPro = handleCustomerInfo(info)
                onResult?.invoke(isPro)
            }
        )
    }

    /**
     * Cierra la sesión en RevenueCat al salir de Supabase.
     */
    fun logoutUser(onComplete: (() -> Unit)? = null) {
        Purchases.sharedInstance.logOutWith(
            onError = { error: PurchasesError ->
                Log.e(TAG, "Error al cerrar sesión en RevenueCat: ${error.message}")
                _isProActive.value = false
                onComplete?.invoke()
            },
            onSuccess = { info: CustomerInfo ->
                Log.d(TAG, "Sesión cerrada en RevenueCat.")
                handleCustomerInfo(info)
                onComplete?.invoke()
            }
        )
    }

    /**
     * Restaura compras anteriores si el usuario reinstaló la app o cambió de dispositivo.
     */
    fun restorePurchases(
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error: PurchasesError ->
                Log.e(TAG, "Error restaurando compras: ${error.message}")
                onError(error.message)
            },
            onSuccess = { info: CustomerInfo ->
                val hasAccess = handleCustomerInfo(info)
                Log.d(TAG, "Compras restauradas. Pro activo: $hasAccess")
                onSuccess(hasAccess)
            }
        )
    }
}
