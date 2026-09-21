package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DeviceRole
import com.example.data.supabase.SupabaseLinkedStoreDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class StoreConfig(
    val deviceRole: DeviceRole = DeviceRole.SENDER,
    val storeCode: String = "str_" + UUID.randomUUID().toString().replace("-", ""),
    val storeName: String = "Mi Negocio",
    val supabaseUrl: String = "https://sljznppbjxfbgzasjnyq.supabase.co",
    val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNsanpucHBianhmYmd6YXNqbnlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgzOTM1NzQsImV4cCI6MjEwMzk2OTU3NH0.bIH3x2xsPjxgU0hTPWAg4IwFjKnsUW7RtdpnRM1DB1o",
    val isCloudEnabled: Boolean = true,
    val remoteTtsEnabled: Boolean = true,
    val remoteTtsTemplate: String = "Pago en tienda: {monto} soles de {emisor}",
    val remoteVibrateEnabled: Boolean = true,
    val remoteNotificationSoundEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val appThemeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val captureYape: Boolean = true,
    val captureBcp: Boolean = true,
    val capturePlin: Boolean = true,
    val isOnboarded: Boolean = false,
    val workerScheduleEnabled: Boolean = false,
    val workerScheduleStartTime: String = "08:00",
    val workerScheduleEndTime: String = "20:00",
    val workerScheduleDays: String = "ALL",
    val workerIsRevoked: Boolean = false,
    val workerBranchName: String = "Principal",
    val storeBranches: String = "Principal,Sucursal 2",
    val allowWorkerHistory: Boolean = true
) {

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank() && storeCode.isNotBlank()
}

class StorePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yape_store_preferences", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val linkedStoreListType = Types.newParameterizedType(List::class.java, SupabaseLinkedStoreDto::class.java)
    private val linkedStoreListAdapter = moshi.adapter<List<SupabaseLinkedStoreDto>>(linkedStoreListType)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<StoreConfig> = _config.asStateFlow()

    private val _linkedStores = MutableStateFlow(loadLinkedStores())
    val linkedStores: StateFlow<List<SupabaseLinkedStoreDto>> = _linkedStores.asStateFlow()

    fun loadConfig(): StoreConfig {
        val defaultStoreCode = "str_" + UUID.randomUUID().toString().replace("-", "")
        val roleStr = prefs.getString(KEY_DEVICE_ROLE, DeviceRole.SENDER.name) ?: DeviceRole.SENDER.name
        val role = try {
            DeviceRole.valueOf(roleStr)
        } catch (_: Exception) {
            DeviceRole.SENDER
        }

        return StoreConfig(
            deviceRole = role,
            storeCode = prefs.getString(KEY_STORE_CODE, defaultStoreCode) ?: defaultStoreCode,
            storeName = prefs.getString(KEY_STORE_NAME, "Mi Negocio") ?: "Mi Negocio",
            supabaseUrl = prefs.getString(KEY_SUPABASE_URL, "https://sljznppbjxfbgzasjnyq.supabase.co") ?: "https://sljznppbjxfbgzasjnyq.supabase.co",
            supabaseAnonKey = prefs.getString(KEY_SUPABASE_ANON_KEY, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNsanpucHBianhmYmd6YXNqbnlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgzOTM1NzQsImV4cCI6MjEwMzk2OTU3NH0.bIH3x2xsPjxgU0hTPWAg4IwFjKnsUW7RtdpnRM1DB1o")
                ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNsanpucHBianhmYmd6YXNqbnlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgzOTM1NzQsImV4cCI6MjEwMzk2OTU3NH0.bIH3x2xsPjxgU0hTPWAg4IwFjKnsUW7RtdpnRM1DB1o",
            isCloudEnabled = prefs.getBoolean(KEY_IS_CLOUD_ENABLED, true),
            remoteTtsEnabled = prefs.getBoolean(KEY_REMOTE_TTS_ENABLED, true),
            remoteTtsTemplate = prefs.getString(KEY_REMOTE_TTS_TEMPLATE, "Pago en tienda: {monto} soles de {emisor}")
                ?: "Pago en tienda: {monto} soles de {emisor}",
            remoteVibrateEnabled = prefs.getBoolean(KEY_REMOTE_VIBRATE_ENABLED, true),
            remoteNotificationSoundEnabled = prefs.getBoolean(KEY_REMOTE_SOUND_ENABLED, true),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L),
            appThemeMode = prefs.getString(KEY_APP_THEME_MODE, "SYSTEM") ?: "SYSTEM",
            captureYape = prefs.getBoolean(KEY_CAPTURE_YAPE, true),
            captureBcp = prefs.getBoolean(KEY_CAPTURE_BCP, true),
            capturePlin = prefs.getBoolean(KEY_CAPTURE_PLIN, true),
            isOnboarded = prefs.getBoolean(KEY_IS_ONBOARDED, false),
            workerScheduleEnabled = prefs.getBoolean(KEY_WORKER_SCHEDULE_ENABLED, false),
            workerScheduleStartTime = prefs.getString(KEY_WORKER_START_TIME, "08:00") ?: "08:00",
            workerScheduleEndTime = prefs.getString(KEY_WORKER_END_TIME, "20:00") ?: "20:00",
            workerScheduleDays = prefs.getString(KEY_WORKER_DAYS, "ALL") ?: "ALL",
            workerIsRevoked = prefs.getBoolean(KEY_WORKER_IS_REVOKED, false),
            workerBranchName = prefs.getString(KEY_WORKER_BRANCH_NAME, "Principal") ?: "Principal",
            storeBranches = prefs.getString(KEY_STORE_BRANCHES, "Principal,Sucursal 2") ?: "Principal,Sucursal 2",
            allowWorkerHistory = prefs.getBoolean(KEY_ALLOW_WORKER_HISTORY, true)
        )
    }

    fun updateConfig(newConfig: StoreConfig) {
        prefs.edit()
            .putString(KEY_DEVICE_ROLE, newConfig.deviceRole.name)
            .putString(KEY_STORE_CODE, newConfig.storeCode.trim().uppercase())
            .putString(KEY_STORE_NAME, newConfig.storeName.trim())
            .putString(KEY_SUPABASE_URL, newConfig.supabaseUrl.trim())
            .putString(KEY_SUPABASE_ANON_KEY, newConfig.supabaseAnonKey.trim())
            .putBoolean(KEY_IS_CLOUD_ENABLED, newConfig.isCloudEnabled)
            .putBoolean(KEY_REMOTE_TTS_ENABLED, newConfig.remoteTtsEnabled)
            .putString(KEY_REMOTE_TTS_TEMPLATE, newConfig.remoteTtsTemplate)
            .putBoolean(KEY_REMOTE_VIBRATE_ENABLED, newConfig.remoteVibrateEnabled)
            .putBoolean(KEY_REMOTE_SOUND_ENABLED, newConfig.remoteNotificationSoundEnabled)
            .putLong(KEY_LAST_SYNC_TIMESTAMP, newConfig.lastSyncTimestamp)
            .putString(KEY_APP_THEME_MODE, newConfig.appThemeMode)
            .putBoolean(KEY_CAPTURE_YAPE, newConfig.captureYape)
            .putBoolean(KEY_CAPTURE_BCP, newConfig.captureBcp)
            .putBoolean(KEY_CAPTURE_PLIN, newConfig.capturePlin)
            .putBoolean(KEY_IS_ONBOARDED, newConfig.isOnboarded)
            .putBoolean(KEY_WORKER_SCHEDULE_ENABLED, newConfig.workerScheduleEnabled)
            .putString(KEY_WORKER_START_TIME, newConfig.workerScheduleStartTime)
            .putString(KEY_WORKER_END_TIME, newConfig.workerScheduleEndTime)
            .putString(KEY_WORKER_DAYS, newConfig.workerScheduleDays)
            .putBoolean(KEY_WORKER_IS_REVOKED, newConfig.workerIsRevoked)
            .putString(KEY_WORKER_BRANCH_NAME, newConfig.workerBranchName)
            .putString(KEY_STORE_BRANCHES, newConfig.storeBranches)
            .putBoolean(KEY_ALLOW_WORKER_HISTORY, newConfig.allowWorkerHistory)
            .commit()
        _config.value = newConfig
    }

    fun updateAllowWorkerHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_WORKER_HISTORY, enabled).commit()
        _config.value = _config.value.copy(allowWorkerHistory = enabled)
    }

    fun saveConfig(newConfig: StoreConfig) = updateConfig(newConfig)

    fun updateWorkerSchedule(scheduleEnabled: Boolean, startTime: String, endTime: String, days: String, isRevoked: Boolean) {
        prefs.edit()
            .putBoolean(KEY_WORKER_SCHEDULE_ENABLED, scheduleEnabled)
            .putString(KEY_WORKER_START_TIME, startTime)
            .putString(KEY_WORKER_END_TIME, endTime)
            .putString(KEY_WORKER_DAYS, days)
            .putBoolean(KEY_WORKER_IS_REVOKED, isRevoked)
            .commit()
        _config.value = _config.value.copy(
            workerScheduleEnabled = scheduleEnabled,
            workerScheduleStartTime = startTime,
            workerScheduleEndTime = endTime,
            workerScheduleDays = days,
            workerIsRevoked = isRevoked
        )
    }

    fun setWorkerRevoked(isRevoked: Boolean) {
        prefs.edit().putBoolean(KEY_WORKER_IS_REVOKED, isRevoked).commit()
        _config.value = _config.value.copy(workerIsRevoked = isRevoked)
    }

    fun setOnboarded(onboarded: Boolean = true) {
        prefs.edit().putBoolean(KEY_IS_ONBOARDED, onboarded).commit()
        _config.value = _config.value.copy(isOnboarded = onboarded)
    }

    fun updateLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).commit()
        _config.value = _config.value.copy(lastSyncTimestamp = timestamp)
    }

    fun updateWorkerBranch(branchName: String) {
        prefs.edit().putString(KEY_WORKER_BRANCH_NAME, branchName.trim()).commit()
        _config.value = _config.value.copy(workerBranchName = branchName.trim())
    }

    fun updateStoreBranches(branches: String) {
        prefs.edit().putString(KEY_STORE_BRANCHES, branches.trim()).commit()
        _config.value = _config.value.copy(storeBranches = branches.trim())
    }

    fun getRevokedStoreCodes(): Set<String> {
        val raw = prefs.getStringSet(KEY_REVOKED_STORE_CODES, emptySet()) ?: emptySet()
        return raw.map { it.trim().lowercase() }.toSet()
    }

    fun addRevokedStoreCode(storeCode: String) {
        val trimmed = storeCode.trim().lowercase()
        if (trimmed.isBlank()) return
        val current = getRevokedStoreCodes().toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_REVOKED_STORE_CODES, current).commit()
    }

    fun removeRevokedStoreCode(storeCode: String) {
        val trimmed = storeCode.trim().lowercase()
        if (trimmed.isBlank()) return
        val current = getRevokedStoreCodes().toMutableSet()
        if (current.remove(trimmed)) {
            prefs.edit().putStringSet(KEY_REVOKED_STORE_CODES, current).commit()
        }
    }

    fun isStoreRevoked(storeCode: String): Boolean {
        val trimmed = storeCode.trim().lowercase()
        if (trimmed.isBlank()) return false
        return getRevokedStoreCodes().contains(trimmed)
    }

    fun setRevokedStoreCode(storeCode: String) {
        prefs.edit().putString(KEY_REVOKED_STORE_CODE, storeCode.trim()).commit()
    }

    fun getRevokedStoreCode(): String {
        return prefs.getString(KEY_REVOKED_STORE_CODE, "") ?: ""
    }

    fun clearRevokedStoreCodes() {
        prefs.edit()
            .remove(KEY_REVOKED_STORE_CODES)
            .remove(KEY_REVOKED_STORE_CODE)
            .commit()
    }

    fun removeLinkedStore(storeCode: String) {
        if (storeCode.isBlank()) return
        addRevokedStoreCode(storeCode)
        val current = _linkedStores.value.toMutableList()
        val changed = current.removeAll { it.storeCode.trim().equals(storeCode.trim(), ignoreCase = true) }
        if (changed) {
            saveLinkedStores(current)
        }
    }

    fun resetReceiverToModeSelection(revokedStoreCode: String? = null) {
        val targetCode = revokedStoreCode?.trim()?.ifBlank { null } ?: getRevokedStoreCode().ifBlank { _config.value.storeCode }
        if (targetCode.isNotBlank()) {
            addRevokedStoreCode(targetCode)
            removeLinkedStore(targetCode)
        }
        prefs.edit()
            .putBoolean(KEY_IS_ONBOARDED, false)
            .putBoolean(KEY_WORKER_IS_REVOKED, false)
            .putString(KEY_REVOKED_STORE_CODE, "")
            .putString(KEY_DEVICE_ROLE, com.example.data.model.DeviceRole.SENDER.name)
            .putString(KEY_STORE_CODE, "")
            .putString(KEY_STORE_NAME, "Sin tienda vinculada")
            .putString(KEY_WORKER_BRANCH_NAME, "Principal")
            .commit()
        _config.value = _config.value.copy(
            isOnboarded = false,
            workerIsRevoked = false,
            deviceRole = com.example.data.model.DeviceRole.SENDER,
            storeCode = "",
            storeName = "Sin tienda vinculada",
            workerBranchName = "Principal"
        )
    }

    fun loadLinkedStores(): List<SupabaseLinkedStoreDto> {
        val raw = prefs.getString(KEY_LINKED_STORES_JSON, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val list = linkedStoreListAdapter.fromJson(raw) ?: emptyList()
            val revoked = getRevokedStoreCodes()
            list.filter { store ->
                val code = store.storeCode.trim().lowercase()
                !revoked.contains(code) && !store.status.equals("REVOKED", ignoreCase = true)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLinkedStores(stores: List<SupabaseLinkedStoreDto>) {
        try {
            val revoked = getRevokedStoreCodes()
            val filtered = stores.filter { store ->
                val code = store.storeCode.trim().lowercase()
                !revoked.contains(code) && !store.status.equals("REVOKED", ignoreCase = true)
            }
            val json = linkedStoreListAdapter.toJson(filtered)
            prefs.edit().putString(KEY_LINKED_STORES_JSON, json).commit()
            _linkedStores.value = filtered
        } catch (_: Exception) {}
    }

    fun addOrUpdateLinkedStore(store: SupabaseLinkedStoreDto) {
        removeRevokedStoreCode(store.storeCode)
        val current = _linkedStores.value.toMutableList()
        val index = current.indexOfFirst { it.storeCode.equals(store.storeCode, ignoreCase = true) }
        if (index >= 0) {
            current[index] = store
        } else {
            current.add(0, store)
        }
        saveLinkedStores(current)
    }

    fun saveOwnerStoreInfo(code: String, name: String, branches: String = "Principal,Sucursal 2") {
        if (code.isBlank()) return
        prefs.edit()
            .putString(KEY_OWNER_STORE_CODE, code.trim().uppercase())
            .putString(KEY_OWNER_STORE_NAME, name.trim().ifBlank { "Mi Negocio" })
            .putString(KEY_OWNER_STORE_BRANCHES, branches.trim().ifBlank { "Principal,Sucursal 2" })
            .commit()
    }

    fun getOwnerStoreInfo(): Triple<String, String, String>? {
        val code = prefs.getString(KEY_OWNER_STORE_CODE, "") ?: ""
        if (code.isBlank()) return null
        val name = prefs.getString(KEY_OWNER_STORE_NAME, "Mi Negocio") ?: "Mi Negocio"
        val branches = prefs.getString(KEY_OWNER_STORE_BRANCHES, "Principal,Sucursal 2") ?: "Principal,Sucursal 2"
        return Triple(code, name, branches)
    }

    fun saveWorkerStoreInfo(code: String, name: String, branch: String = "Principal") {
        if (code.isBlank()) return
        prefs.edit()
            .putString(KEY_WORKER_STORE_CODE, code.trim().uppercase())
            .putString(KEY_WORKER_STORE_NAME, name.trim())
            .putString(KEY_WORKER_BRANCH_NAME, branch.trim().ifBlank { "Principal" })
            .commit()
    }

    fun getWorkerStoreInfo(): Triple<String, String, String>? {
        val code = prefs.getString(KEY_WORKER_STORE_CODE, "") ?: ""
        if (code.isBlank()) return null
        val name = prefs.getString(KEY_WORKER_STORE_NAME, "Sin tienda vinculada") ?: "Sin tienda vinculada"
        val branch = prefs.getString(KEY_WORKER_BRANCH_NAME, "Principal") ?: "Principal"
        return Triple(code, name, branch)
    }

    companion object {
        private const val KEY_DEVICE_ROLE = "device_role"
        private const val KEY_STORE_CODE = "store_code"
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_OWNER_STORE_CODE = "owner_store_code"
        private const val KEY_OWNER_STORE_NAME = "owner_store_name"
        private const val KEY_OWNER_STORE_BRANCHES = "owner_store_branches"
        private const val KEY_WORKER_STORE_CODE = "worker_store_code"
        private const val KEY_WORKER_STORE_NAME = "worker_store_name"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_IS_CLOUD_ENABLED = "is_cloud_enabled"
        private const val KEY_REMOTE_TTS_ENABLED = "remote_tts_enabled"
        private const val KEY_REMOTE_TTS_TEMPLATE = "remote_tts_template"
        private const val KEY_REMOTE_VIBRATE_ENABLED = "remote_vibrate_enabled"
        private const val KEY_REMOTE_SOUND_ENABLED = "remote_sound_enabled"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_CAPTURE_YAPE = "capture_yape"
        private const val KEY_CAPTURE_BCP = "capture_bcp"
        private const val KEY_CAPTURE_PLIN = "capture_plin"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_WORKER_SCHEDULE_ENABLED = "worker_schedule_enabled"
        private const val KEY_WORKER_START_TIME = "worker_start_time"
        private const val KEY_WORKER_END_TIME = "worker_end_time"
        private const val KEY_WORKER_DAYS = "worker_days"
        private const val KEY_WORKER_IS_REVOKED = "worker_is_revoked"
        private const val KEY_REVOKED_STORE_CODE = "revoked_store_code"
        private const val KEY_REVOKED_STORE_CODES = "revoked_store_codes"
        private const val KEY_WORKER_BRANCH_NAME = "worker_branch_name"
        private const val KEY_STORE_BRANCHES = "store_branches"
        private const val KEY_LINKED_STORES_JSON = "linked_stores_json"
        private const val KEY_ALLOW_WORKER_HISTORY = "allow_worker_history"


        @Volatile
        private var INSTANCE: StorePreferences? = null

        fun getInstance(context: Context): StorePreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = StorePreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
