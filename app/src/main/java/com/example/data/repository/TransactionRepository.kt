package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.TransactionDao
import com.example.data.fcm.FcmSender
import com.example.data.model.DeviceRole
import com.example.data.model.YapeTransaction
import com.example.data.preferences.AuthPreferences
import com.example.data.preferences.StoreConfig
import com.example.data.preferences.StorePreferences
import com.example.data.supabase.ConnectionStatus
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseTransactionDto
import com.example.service.remote.RemoteAlertManager
import com.example.util.WorkerScheduleHelper
import com.example.widget.YapeTotalWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

class TransactionRepository(
    private val context: Context,
    private val dao: TransactionDao = AppDatabase.getDatabase(context).transactionDao(),
    private val supabaseClient: SupabaseClient = SupabaseClient.getInstance(),
    private val storePreferences: StorePreferences = StorePreferences.getInstance(context),
    private val authPreferences: AuthPreferences = AuthPreferences.getInstance(context),
    private val authRepository: AuthRepository = AuthRepository.getInstance(context),
    private val remoteAlertManager: RemoteAlertManager = RemoteAlertManager.getInstance(context)
) {

    val allTransactions: Flow<List<YapeTransaction>> = dao.getAllTransactions()
    val latestTransaction: Flow<YapeTransaction?> = dao.getLatestTransaction()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.UNCONFIGURED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun getTodayStartAndEndTime(): Pair<Long, Long> {
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

        return Pair(startTime, endTime)
    }

    fun getDayStartAndEndTime(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        return Pair(startTime, endTime)
    }

    fun getTodayTransactions(storeCode: String = ""): Flow<List<YapeTransaction>> {
        val (start, end) = getTodayStartAndEndTime()
        return dao.getTransactionsBetween(start, end, storeCode)
    }

    fun getTodayTotal(storeCode: String = ""): Flow<Double> {
        val (start, end) = getTodayStartAndEndTime()
        return dao.getTotalAmountBetween(start, end, storeCode)
    }

    fun getTodayCount(storeCode: String = ""): Flow<Int> {
        val (start, end) = getTodayStartAndEndTime()
        return dao.getCountBetween(start, end, storeCode)
    }

    fun getTodayExcludedTotal(storeCode: String = ""): Flow<Double> {
        val (start, end) = getTodayStartAndEndTime()
        return dao.getExcludedTotalAmountBetween(start, end, storeCode)
    }

    fun getTodayExcludedCount(storeCode: String = ""): Flow<Int> {
        val (start, end) = getTodayStartAndEndTime()
        return dao.getExcludedCountBetween(start, end, storeCode)
    }

    fun getTransactionsBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<List<YapeTransaction>> {
        return dao.getTransactionsBetween(startTime, endTime, storeCode)
    }

    fun getLast7DaysTransactions(storeCode: String = ""): Flow<List<YapeTransaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        return dao.getTransactionsBetween(startTime, endTime, storeCode)
    }

    fun getTransactionsForDay(calendar: Calendar, storeCode: String = ""): Flow<List<YapeTransaction>> {
        val (start, end) = getDayStartAndEndTime(calendar)
        return dao.getTransactionsBetween(start, end, storeCode)
    }

    fun getTotalForDay(calendar: Calendar, storeCode: String = ""): Flow<Double> {
        val (start, end) = getDayStartAndEndTime(calendar)
        return dao.getTotalAmountBetween(start, end, storeCode)
    }

    fun getExcludedTotalForDay(calendar: Calendar, storeCode: String = ""): Flow<Double> {
        val (start, end) = getDayStartAndEndTime(calendar)
        return dao.getExcludedTotalAmountBetween(start, end, storeCode)
    }

    fun getExcludedCountForDay(calendar: Calendar, storeCode: String = ""): Flow<Int> {
        val (start, end) = getDayStartAndEndTime(calendar)
        return dao.getExcludedCountBetween(start, end, storeCode)
    }

    suspend fun updateStoreTransactionStatus(id: Long, isStore: Boolean, reason: String = "") {
        dao.updateStoreTransactionStatus(id, isStore, reason)
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    suspend fun insertTransaction(transaction: YapeTransaction): Long {
        val config = storePreferences.config.value
        val persistentRemoteId = if (transaction.remoteId.isNotBlank()) {
            transaction.remoteId
        } else {
            java.util.UUID.randomUUID().toString()
        }
        val txWithStore = transaction.copy(
            remoteId = persistentRemoteId,
            storeCode = if (transaction.storeCode.isBlank()) config.storeCode else transaction.storeCode
        )

        val id = dao.insert(txWithStore)
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}

        // If Cloud is configured and enabled on SENDER mode, upload to Supabase and send FCM push immediately
        if (config.isConfigured && config.isCloudEnabled && config.deviceRole == DeviceRole.SENDER) {
            val uploadedRemoteId = uploadSingleTransaction(txWithStore.copy(id = id), config)
            val txToSend = txWithStore.copy(
                id = id,
                remoteId = if (!uploadedRemoteId.isNullOrBlank()) uploadedRemoteId else persistentRemoteId
            )
            FcmSender.sendPaymentPush(txToSend, config)
        }

        return id
    }

    suspend fun uploadSingleTransaction(tx: YapeTransaction, config: StoreConfig): String? = withContext(Dispatchers.IO) {
        try {
            val userToken = authRepository.getFreshToken()?.ifBlank { null }
            val formattedClaimedAt = if (tx.claimedAt > 0L) {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date(tx.claimedAt))
            } else null

            val dto = SupabaseTransactionDto(
                id = tx.remoteId.ifBlank { null },
                storeCode = config.storeCode,
                senderName = tx.senderName,
                amount = tx.amount,
                timestamp = tx.timestamp,
                transactionType = tx.transactionType,
                rawNotification = tx.rawNotification,
                branchName = tx.branchName.ifBlank { null },
                claimedBy = tx.claimedBy.ifBlank { null },
                claimedByName = tx.claimedByName.ifBlank { null },
                claimedAt = formattedClaimedAt,
                note = tx.note.ifBlank { null }
            )
            val result = supabaseClient.uploadTransaction(config.supabaseUrl, config.supabaseAnonKey, dto, userToken)
            if (result.isSuccess) {
                val created = result.getOrNull()
                val remoteId = created?.id?.ifBlank { null } ?: tx.remoteId
                dao.markAsSynced(tx.id, remoteId, System.currentTimeMillis())
                storePreferences.updateLastSyncTimestamp(System.currentTimeMillis())
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _lastSyncError.value = null
                remoteId
            } else {
                Log.w(TAG, "Single upload pending retry: ${result.exceptionOrNull()?.message}")
                _lastSyncError.value = result.exceptionOrNull()?.message
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadSingleTransaction failed", e)
            _lastSyncError.value = e.message
            null
        }
    }

    suspend fun syncPendingUploads(): Int = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        if (!config.isConfigured || !config.isCloudEnabled) {
            _connectionStatus.value = ConnectionStatus.UNCONFIGURED
            return@withContext 0
        }

        _isSyncing.value = true
        _connectionStatus.value = ConnectionStatus.SYNCING

        try {
            val unsynced = dao.getUnsyncedTransactions()
            var syncedCount = 0

            val user = authPreferences.currentUser.value
            val userToken = user.token.ifBlank { null }
            for (tx in unsynced) {
                val formattedClaimedAt = if (tx.claimedAt > 0L) {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date(tx.claimedAt))
                } else null

                val persistentRemoteId = if (tx.remoteId.isNotBlank()) tx.remoteId else java.util.UUID.randomUUID().toString()
                val dto = SupabaseTransactionDto(
                    id = persistentRemoteId,
                    storeCode = config.storeCode,
                    senderName = tx.senderName,
                    amount = tx.amount,
                    timestamp = tx.timestamp,
                    transactionType = tx.transactionType,
                    rawNotification = tx.rawNotification,
                    branchName = tx.branchName.ifBlank { null },
                    claimedBy = tx.claimedBy.ifBlank { null },
                    claimedByName = tx.claimedByName.ifBlank { null },
                    claimedAt = formattedClaimedAt,
                    note = tx.note.ifBlank { null }
                )
                val res = supabaseClient.uploadTransaction(config.supabaseUrl, config.supabaseAnonKey, dto, userToken)
                if (res.isSuccess) {
                    val created = res.getOrNull()
                    val remoteId = created?.id?.ifBlank { null } ?: persistentRemoteId
                    dao.markAsSynced(tx.id, remoteId, System.currentTimeMillis())
                    syncedCount++
                }
            }

            // Update heartbeat with active branches and owner
            supabaseClient.updateStoreHeartbeat(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                storeCode = config.storeCode,
                storeName = config.storeName,
                userToken = userToken,
                ownerId = user.id.ifBlank { null },
                branches = config.storeBranches.ifBlank { "Principal,Sucursal 2" }
            )

            storePreferences.updateLastSyncTimestamp(System.currentTimeMillis())
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _lastSyncError.value = null
            _isSyncing.value = false
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "syncPendingUploads failed", e)
            _lastSyncError.value = e.message
            _connectionStatus.value = ConnectionStatus.ERROR
            _isSyncing.value = false
            0
        }
    }

    suspend fun clearTransactionsForOtherStores(currentStoreCode: String) = withContext(Dispatchers.IO) {
        // Preservación intencional de caché local: No se borran ventas de otras tiendas para soporte multi-tienda offline
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    suspend fun getStoreLocalTransactionsCount(storeCode: String): Int = withContext(Dispatchers.IO) {
        if (storeCode.isBlank()) 0 else dao.getTransactionsCountForStore(storeCode)
    }

    suspend fun syncRemoteReceiver(triggerAlerts: Boolean = true, fullHistorySync: Boolean = false): Int = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        if (!config.isConfigured || !config.isCloudEnabled || config.storeCode.isBlank() || config.workerIsRevoked) {
            _connectionStatus.value = ConnectionStatus.UNCONFIGURED
            return@withContext 0
        }

        _isSyncing.value = true
        _connectionStatus.value = ConnectionStatus.SYNCING

        try {
            val (todayStart, _) = getTodayStartAndEndTime()
            // If fullHistorySync requested (or first sync), fetch last 30 days.
            // Otherwise, always fetch from start of today so today's totals are always 100% preloaded and up to date!
            val fetchSince = if (fullHistorySync || config.lastSyncTimestamp == 0L) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } else {
                todayStart
            }

            val userToken = authRepository.getFreshToken()?.ifBlank { null }
            val result = supabaseClient.fetchTransactionsSince(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                storeCode = config.storeCode,
                sinceTimestamp = fetchSince,
                userToken = userToken
            )

            var dtoList = result.getOrNull() ?: emptyList()
            if (dtoList.isEmpty() && userToken != null) {
                // Fallback con anonKey por si las políticas RLS bloquearon al usuario autenticado
                val fallback = supabaseClient.fetchTransactionsSince(
                    url = config.supabaseUrl,
                    anonKey = config.supabaseAnonKey,
                    storeCode = config.storeCode,
                    sinceTimestamp = fetchSince,
                    userToken = null
                )
                if (fallback.isSuccess) {
                    val fbList = fallback.getOrNull() ?: emptyList()
                    if (fbList.isNotEmpty()) {
                        dtoList = fbList
                        Log.d(TAG, "syncRemoteReceiver recovered ${dtoList.size} transactions via anonKey fallback")
                    }
                }
            }

            if (dtoList.isNotEmpty() || result.isSuccess) {
                var newCount = 0
                val now = System.currentTimeMillis()

                for (dto in dtoList) {
                    val existing = com.example.util.TransactionDeduplicator.findDuplicate(
                        dao = dao,
                        timestamp = dto.timestamp,
                        amount = dto.amount,
                        senderName = dto.senderName,
                        rawNotification = dto.rawNotification,
                        storeCode = dto.storeCode,
                        remoteId = dto.id ?: ""
                    )

                    if (existing != null) {
                        val remoteBranch = dto.branchName ?: ""
                        val remoteClaimedName = dto.claimedByName ?: ""
                        val remoteNote = dto.note ?: ""
                        if (existing.remoteId.isBlank() && !dto.id.isNullOrBlank()) {
                            dao.linkRemoteId(existing.id, dto.id, now)
                        }
                        if (existing.branchName != remoteBranch || existing.claimedByName != remoteClaimedName || (remoteNote.isNotBlank() && existing.note != remoteNote)) {
                            dao.updateTransactionBranch(
                                localId = existing.id,
                                remoteId = dto.id ?: "",
                                branchName = remoteBranch,
                                claimedBy = dto.claimedBy ?: "",
                                claimedByName = remoteClaimedName,
                                claimedAt = 0L,
                                note = remoteNote.ifBlank { null }
                            )
                        }
                    } else {
                        val newTx = YapeTransaction(
                            senderName = dto.senderName,
                            amount = dto.amount,
                            timestamp = dto.timestamp,
                            rawNotification = dto.rawNotification,
                            transactionType = dto.transactionType,
                            storeCode = dto.storeCode,
                            isSynced = true,
                            remoteId = dto.id ?: "",
                            syncTimestamp = now,
                            branchName = dto.branchName ?: "",
                            claimedBy = dto.claimedBy ?: "",
                            claimedByName = dto.claimedByName ?: "",
                            note = dto.note ?: ""
                        )
                        val insertedId = dao.insert(newTx)
                        newCount++

                        // Trigger remote alert and TTS ONLY for fresh real-time transactions (within last 90 seconds)
                        val isRecent = (now - dto.timestamp) < 90_000L
                        if (triggerAlerts && isRecent && config.deviceRole == DeviceRole.RECEIVER) {
                            if (!config.workerIsRevoked && WorkerScheduleHelper.isConfigWithinSchedule(config)) {
                                remoteAlertManager.triggerRemotePaymentAlert(newTx.copy(id = insertedId), config)
                            } else {
                                Log.d(TAG, "Suppressed alert on receiver: workerIsRevoked=${config.workerIsRevoked}, withinSchedule=${WorkerScheduleHelper.isConfigWithinSchedule(config)}")
                            }
                        }
                    }
                }

                // Autolimpieza preventiva de duplicados locales generados previamente
                try {
                    cleanupLocalDuplicates(config.storeCode)
                } catch (e: Exception) {
                    Log.w(TAG, "cleanupLocalDuplicates non-fatal: ${e.message}")
                }

                if (newCount > 0) {
                    try {
                        YapeTotalWidgetProvider.updateAllWidgets(context)
                    } catch (_: Exception) {}
                }

                storePreferences.updateLastSyncTimestamp(now)
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _lastSyncError.value = null
                _isSyncing.value = false
                newCount
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido"
                _lastSyncError.value = err
                _connectionStatus.value = ConnectionStatus.ERROR
                _isSyncing.value = false
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncRemoteReceiver failed", e)
            _lastSyncError.value = e.message
            _connectionStatus.value = ConnectionStatus.ERROR
            _isSyncing.value = false
            0
        }
    }

    /**
     * Importa todas las transacciones de una tienda desde la nube (SELECT * FROM yape_transactions WHERE store_code = '...'),
     * guardándolas en la base de datos local (Room) sin duplicados y sin límite restrictivo de fecha.
     */
    suspend fun syncAllStoreTransactions(targetStoreCode: String? = null): Int = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        val storeCode = targetStoreCode?.trim()?.ifBlank { null } ?: config.storeCode.trim()
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank() || storeCode.isBlank()) {
            return@withContext 0
        }

        try {
            val token = authRepository.getFreshToken()
            val user = authPreferences.currentUser.value
            val userToken = token?.ifBlank { null }

            // Solo registrar owner_id en Supabase si el dispositivo está en Modo Dueño (SENDER)
            if (user.isLoggedIn && user.id.isNotBlank() && config.deviceRole == DeviceRole.SENDER) {
                try {
                    supabaseClient.updateStoreHeartbeat(
                        url = config.supabaseUrl,
                        anonKey = config.supabaseAnonKey,
                        storeCode = storeCode,
                        storeName = config.storeName,
                        userToken = userToken,
                        ownerId = user.id,
                        branches = config.storeBranches.ifBlank { "Principal,Sucursal 2" },
                        allowWorkerHistory = config.allowWorkerHistory
                    )
                } catch (_: Exception) {}
            }

            val result = supabaseClient.fetchAllTransactionsForStore(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                storeCode = storeCode,
                userToken = userToken
            )

            val dtoList = result.getOrNull() ?: emptyList()
            var imported = 0
            val now = System.currentTimeMillis()

            for (dto in dtoList) {
                val existing = com.example.util.TransactionDeduplicator.findDuplicate(
                    dao = dao,
                    timestamp = dto.timestamp,
                    amount = dto.amount,
                    senderName = dto.senderName,
                    rawNotification = dto.rawNotification,
                    storeCode = dto.storeCode,
                    remoteId = dto.id ?: ""
                )

                if (existing != null) {
                    val remoteBranch = dto.branchName ?: ""
                    val remoteClaimedName = dto.claimedByName ?: ""
                    val remoteNote = dto.note ?: ""
                    if (existing.remoteId.isBlank() && !dto.id.isNullOrBlank()) {
                        dao.linkRemoteId(existing.id, dto.id, now)
                    }
                    if (existing.branchName != remoteBranch || existing.claimedByName != remoteClaimedName || (remoteNote.isNotBlank() && existing.note != remoteNote)) {
                        dao.updateTransactionBranch(
                            localId = existing.id,
                            remoteId = dto.id ?: "",
                            branchName = remoteBranch,
                            claimedBy = dto.claimedBy ?: "",
                            claimedByName = remoteClaimedName,
                            claimedAt = 0L,
                            note = remoteNote.ifBlank { null }
                        )
                    }
                } else {
                    val newTx = YapeTransaction(
                        senderName = dto.senderName,
                        amount = dto.amount,
                        timestamp = dto.timestamp,
                        rawNotification = dto.rawNotification,
                        transactionType = dto.transactionType,
                        storeCode = dto.storeCode,
                        isSynced = true,
                        remoteId = dto.id ?: "",
                        syncTimestamp = now,
                        branchName = dto.branchName ?: "",
                        claimedBy = dto.claimedBy ?: "",
                        claimedByName = dto.claimedByName ?: "",
                        note = dto.note ?: ""
                    )
                    dao.insert(newTx)
                    imported++
                }
            }

            // Autolimpieza preventiva de duplicados locales
            try {
                cleanupLocalDuplicates(storeCode)
            } catch (e: Exception) {
                Log.w(TAG, "cleanupLocalDuplicates non-fatal: ${e.message}")
            }

            if (imported > 0) {
                try {
                    YapeTotalWidgetProvider.updateAllWidgets(context)
                } catch (_: Exception) {}
            }
            storePreferences.updateLastSyncTimestamp(now)
            Log.d(TAG, "syncAllStoreTransactions imported $imported past transactions for $storeCode")
            imported
        } catch (e: Exception) {
            Log.e(TAG, "syncAllStoreTransactions failed for $storeCode", e)
            0
        }
    }

    /**
     * Importa las ventas históricas de la nube al iniciar sesión (para el Owner o Receiver),
     * guardándolas en la base de datos local (Room) sin duplicados y sin disparar alertas sonoras.
     */
    suspend fun syncHistoricalSalesOnLogin(): Int = syncAllStoreTransactions()

    /**
     * Limpia la base de datos local y los widgets al cerrar sesión,
     * garantizando privacidad y que no queden ventas residuales guardadas.
     */
    suspend fun clearLocalDataOnSignOut() = withContext(Dispatchers.IO) {
        try {
            dao.clearAll()
            storePreferences.updateLastSyncTimestamp(0L)
            YapeTotalWidgetProvider.updateAllWidgets(context)
            Log.d(TAG, "Local database cleared on sign out")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local data on sign out", e)
        }
    }

    suspend fun getStoreByOwner(ownerId: String): Result<com.example.data.supabase.SupabaseStoreDto?> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.getStoreByOwner(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            ownerId = ownerId,
            userToken = token.ifBlank { null }
        )
    }

    suspend fun registerNewOwnerStore(
        storeCode: String,
        storeName: String,
        ownerId: String,
        branches: String = "Principal,Sucursal 2"
    ): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.registerNewOwnerStore(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = storeCode,
            storeName = storeName,
            ownerId = ownerId,
            branches = branches,
            userToken = token.ifBlank { null }
        )
    }

    suspend fun createOrGetOwnerStore(
        storeName: String = "Mi Negocio",
        branches: String = "Principal,Sucursal 2"
    ): Result<com.example.data.supabase.SupabaseStoreDto> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.createOrGetOwnerStore(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeName = storeName,
            branches = branches,
            userToken = token.ifBlank { null }
        )
    }

    suspend fun testSupabaseConnection(url: String, anonKey: String): Result<Boolean> {
        return supabaseClient.testConnection(url, anonKey)
    }

    suspend fun redeemPairingCode(code: String): Result<com.example.data.supabase.SupabaseRedeemResponseDto> {
        val config = storePreferences.config.value
        val url = config.supabaseUrl
        val anonKey = config.supabaseAnonKey
        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(Exception("Falta configurar la URL y Anon Key de Supabase"))
        }
        val token = authRepository.getFreshToken()
        if (token.isNullOrBlank()) {
            return Result.failure(Exception("Tu sesión ha expirado. Por favor inicia sesión nuevamente."))
        }
        val result = supabaseClient.redeemPairingCode(
            url = url,
            anonKey = anonKey,
            code = code,
            userToken = token
        )
        if (result.isFailure && result.exceptionOrNull()?.message?.contains("jwt expired", ignoreCase = true) == true) {
            val refreshed = authRepository.refreshSession().getOrNull()?.token
            if (!refreshed.isNullOrBlank()) {
                return supabaseClient.redeemPairingCode(url, anonKey, code, refreshed)
            } else {
                authPreferences.clearSession(keepEmail = true)
                return Result.failure(Exception("Tu sesión ha expirado. Por favor inicia sesión nuevamente."))
            }
        }
        return result
    }

    suspend fun fetchMyLinkedStores(): Result<List<com.example.data.supabase.SupabaseLinkedStoreDto>> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val url = config.supabaseUrl
        val anonKey = config.supabaseAnonKey
        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(Exception("Falta configurar la URL y Anon Key de Supabase"))
        }
        val token = authRepository.getFreshToken() ?: user.token
        var result = supabaseClient.fetchMyLinkedStores(
            url = url,
            anonKey = anonKey,
            userToken = token
        )
        if (result.isFailure && result.exceptionOrNull()?.message?.contains("jwt expired", ignoreCase = true) == true) {
            val refreshed = authRepository.refreshSession().getOrNull()?.token
            if (!refreshed.isNullOrBlank()) {
                result = supabaseClient.fetchMyLinkedStores(url, anonKey, refreshed)
            }
        }
        if (result.isSuccess) {
            val list = result.getOrNull() ?: emptyList()
            storePreferences.saveLinkedStores(list)
        }
        return result
    }

    suspend fun generatePairingCode(storeCode: String): Result<String> {
        val config = storePreferences.config.value
        val url = config.supabaseUrl
        val anonKey = config.supabaseAnonKey
        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(Exception("Falta configurar la URL y Anon Key de Supabase"))
        }
        val token = authRepository.getFreshToken()
        val user = authPreferences.currentUser.value
        if (token.isNullOrBlank()) {
            return Result.failure(Exception("Tu sesión ha expirado. Por favor inicia sesión nuevamente."))
        }
        val result = supabaseClient.createPairingCode(
            url = url,
            anonKey = anonKey,
            storeCode = storeCode,
            userToken = token,
            userId = user.id
        )
        if (result.isFailure && result.exceptionOrNull()?.message?.contains("jwt expired", ignoreCase = true) == true) {
            val refreshed = authRepository.refreshSession().getOrNull()?.token
            if (!refreshed.isNullOrBlank()) {
                return supabaseClient.createPairingCode(url, anonKey, storeCode, refreshed, user.id)
            } else {
                authPreferences.clearSession(keepEmail = true)
                return Result.failure(Exception("Tu sesión ha expirado. Por favor inicia sesión nuevamente."))
            }
        }
        return result
    }

    suspend fun fetchStoreReceivers(storeCode: String): Result<List<com.example.data.supabase.SupabaseReceiverDto>> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return Result.failure(Exception("Supabase no está configurado"))
        }
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.fetchStoreReceivers(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = storeCode,
            userToken = token
        )
    }

    suspend fun registerStoreReceiver(
        storeCode: String,
        pairingCode: String?
    ): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return Result.failure(Exception("Supabase no está configurado"))
        }
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.registerStoreReceiver(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = storeCode,
            userId = user.id.ifBlank { null },
            userName = user.displayName.ifBlank { "Trabajador" },
            userEmail = user.email.ifBlank { null },
            pairingCode = pairingCode,
            userToken = token
        )
    }

    suspend fun updateReceiverName(receiverId: String, newName: String): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.updateReceiverName(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            receiverId = receiverId,
            newName = newName,
            userToken = token
        )
    }

    suspend fun updateReceiverSchedule(
        receiverId: String,
        scheduleEnabled: Boolean,
        startTime: String,
        endTime: String,
        days: String
    ): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        return supabaseClient.updateReceiverSchedule(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            receiverId = receiverId,
            scheduleEnabled = scheduleEnabled,
            startTime = startTime,
            endTime = endTime,
            days = days,
            userToken = token
        )
    }

    suspend fun kickReceiver(
        receiverId: String,
        targetUserId: String? = null,
        targetStoreCode: String? = null
    ): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val token = authRepository.getFreshToken() ?: user.token
        val storeCode = targetStoreCode?.trim()?.ifBlank { null } ?: config.storeCode.trim()
        val result = supabaseClient.kickReceiver(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            receiverId = receiverId,
            targetUserId = targetUserId,
            storeCode = storeCode,
            userToken = token
        )
        try {
            FcmSender.sendWorkerRevokedPush(
                targetUserId = targetUserId ?: "",
                storeCode = storeCode
            )
        } catch (_: Exception) {}
        return result
    }

    suspend fun checkMyReceiverStatus(storeCodeToCheck: String? = null): Result<com.example.data.supabase.SupabaseReceiverDto?> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val targetStoreCode = storeCodeToCheck?.trim()?.ifBlank { null } ?: config.storeCode.trim()
        if (!user.isLoggedIn || user.id.isBlank() || targetStoreCode.isBlank()) return Result.success(null)
        val token = authRepository.getFreshToken() ?: user.token
        val result = supabaseClient.fetchMyReceiverStatus(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = targetStoreCode,
            userId = user.id,
            userToken = token
        )
        result.onSuccess { receiver ->
            val isRevoked = receiver != null && receiver.status.equals("REVOKED", ignoreCase = true)
            if (isRevoked) {
                storePreferences.addRevokedStoreCode(targetStoreCode)
                storePreferences.removeLinkedStore(targetStoreCode)
                if (targetStoreCode.equals(config.storeCode, ignoreCase = true)) {
                    storePreferences.setRevokedStoreCode(targetStoreCode)
                    storePreferences.updateWorkerSchedule(
                        scheduleEnabled = false,
                        startTime = "00:00",
                        endTime = "23:59",
                        days = "1,2,3,4,5,6,7",
                        isRevoked = true
                    )
                    com.example.service.remote.FcmNotificationReceiverService.unsubscribeFromStoreTopic(targetStoreCode)
                    storePreferences.updateConfig(config.copy(storeCode = "", workerIsRevoked = true))
                    remoteAlertManager.showRevokedNotification(config.storeName)
                }
            } else if (receiver != null && targetStoreCode.equals(config.storeCode, ignoreCase = true)) {
                storePreferences.removeRevokedStoreCode(targetStoreCode)
                storePreferences.updateWorkerSchedule(
                    scheduleEnabled = receiver.scheduleEnabled,
                    startTime = receiver.scheduleStartTime,
                    endTime = receiver.scheduleEndTime,
                    days = receiver.scheduleDays,
                    isRevoked = false
                )
                if (receiver.branchName.isNotBlank()) {
                    storePreferences.updateWorkerBranch(receiver.branchName)
                }
            }
        }
        return result
    }

    suspend fun updateReceiverBranch(receiverId: String, branchName: String): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        return supabaseClient.updateReceiverBranch(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            receiverId = receiverId,
            branchName = branchName,
            userToken = user.token
        )
    }

    suspend fun claimTransactionBranch(
        transaction: YapeTransaction,
        branchName: String,
        claimedByName: String,
        note: String = ""
    ): Result<com.example.data.supabase.SupabaseClaimResponseDto> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        var remoteId = transaction.remoteId.ifBlank { "" }
        val effectiveNote = note.ifBlank { transaction.note }.trim()

        // 1. Si no tiene remoteId, intentar buscarlo primero en Supabase antes de intentar duplicar la transacción
        if (remoteId.isBlank() && config.isConfigured && config.isCloudEnabled) {
            val lookupRes = supabaseClient.findTransactionRemoteId(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                storeCode = config.storeCode,
                timestamp = transaction.timestamp,
                amount = transaction.amount,
                senderName = transaction.senderName,
                userToken = user.token.ifBlank { null }
            )
            val foundId = lookupRes.getOrNull()
            if (!foundId.isNullOrBlank()) {
                remoteId = foundId
                dao.linkRemoteId(transaction.id, remoteId, System.currentTimeMillis())
                Log.d(TAG, "Encontrado remoteId=$remoteId en Supabase para tx id=${transaction.id}")
            }
        }

        // 2. Si aún no tiene remoteId y es Dueño, subirlo para registrarlo
        if (remoteId.isBlank() && config.isConfigured && config.isCloudEnabled && config.deviceRole == DeviceRole.SENDER) {
            val formattedNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date())
            val dto = SupabaseTransactionDto(
                storeCode = config.storeCode,
                senderName = transaction.senderName,
                amount = transaction.amount,
                timestamp = transaction.timestamp,
                transactionType = transaction.transactionType,
                rawNotification = transaction.rawNotification,
                branchName = branchName,
                claimedBy = user.id.ifBlank { null },
                claimedByName = claimedByName.ifBlank { null },
                claimedAt = formattedNow,
                note = effectiveNote.ifBlank { null }
            )
            val uploadRes = supabaseClient.uploadTransaction(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                dto = dto,
                userToken = user.token.ifBlank { null }
            )
            if (uploadRes.isSuccess) {
                val created = uploadRes.getOrNull()
                remoteId = created?.id ?: ""
                dao.markAsSynced(transaction.id, remoteId, System.currentTimeMillis())
            }
        }

        if (remoteId.isNotBlank()) {
            val res = supabaseClient.claimTransactionBranch(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                transactionRemoteId = remoteId,
                branchName = branchName,
                claimedByName = claimedByName,
                userToken = user.token,
                note = effectiveNote.ifBlank { null }
            )
            if (res.isSuccess) {
                dao.updateTransactionBranch(
                    localId = transaction.id,
                    remoteId = remoteId,
                    branchName = branchName,
                    claimedBy = user.id,
                    claimedByName = claimedByName,
                    claimedAt = System.currentTimeMillis(),
                    note = effectiveNote.ifBlank { null }
                )
            }
            return res
        } else {
            dao.updateTransactionBranch(
                localId = transaction.id,
                remoteId = "",
                branchName = branchName,
                claimedBy = user.id,
                claimedByName = claimedByName,
                claimedAt = System.currentTimeMillis(),
                note = effectiveNote.ifBlank { null }
            )
            return Result.success(
                com.example.data.supabase.SupabaseClaimResponseDto(
                    success = true,
                    message = "Pago confirmado para $branchName",
                    branchName = branchName
                )
            )
        }
    }

    suspend fun syncStoreBranchesToRemote(branches: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        if (!config.isConfigured || config.storeCode.isBlank()) {
            return@withContext Result.success(true)
        }
        supabaseClient.updateStoreBranches(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = config.storeCode,
            branches = branches,
            userToken = user.token.ifBlank { null }
        )
    }

    suspend fun updateStoreAllowWorkerHistory(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        if (!config.isConfigured || config.storeCode.isBlank()) {
            return@withContext Result.success(true)
        }
        val token = authRepository.getFreshToken() ?: user.token
        supabaseClient.updateStoreAllowWorkerHistory(
            url = config.supabaseUrl,
            anonKey = config.supabaseAnonKey,
            storeCode = config.storeCode,
            allowWorkerHistory = enabled,
            userToken = token.ifBlank { null }
        )
    }

    suspend fun unclaimTransactionBranch(
        transaction: YapeTransaction,
        branchName: String
    ): Result<Boolean> {
        val config = storePreferences.config.value
        val user = authPreferences.currentUser.value
        val remoteId = transaction.remoteId.ifBlank { "" }

        if (remoteId.isNotBlank()) {
            val res = supabaseClient.unclaimTransactionBranch(
                url = config.supabaseUrl,
                anonKey = config.supabaseAnonKey,
                transactionRemoteId = remoteId,
                branchName = branchName,
                userToken = user.token,
                callerUserId = user.id
            )
            if (res.isSuccess) {
                dao.updateTransactionBranch(
                    localId = transaction.id,
                    remoteId = remoteId,
                    branchName = "",
                    claimedBy = "",
                    claimedByName = "",
                    claimedAt = 0L,
                    note = ""
                )
            }
            return res
        } else {
            dao.updateTransactionBranch(
                localId = transaction.id,
                remoteId = "",
                branchName = "",
                claimedBy = "",
                claimedByName = "",
                claimedAt = 0L,
                note = ""
            )
            return Result.success(true)
        }
    }

    suspend fun updateTransactionNote(
        transaction: YapeTransaction,
        newNote: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trimmedNote = newNote.trim()
            val remoteId = transaction.remoteId.ifBlank { "" }

            // 1. Actualizar Room localmente de inmediato para reactividad instantánea en la app
            dao.updateTransactionNote(
                localId = transaction.id,
                remoteId = remoteId,
                note = trimmedNote
            )

            // 2. Si la nube está habilitada y tiene remoteId, sincronizar con Supabase
            val config = storePreferences.config.value
            val user = authPreferences.currentUser.value
            if (config.isCloudEnabled && remoteId.isNotBlank()) {
                supabaseClient.updateTransactionNote(
                    url = config.supabaseUrl,
                    anonKey = config.supabaseAnonKey,
                    transactionRemoteId = remoteId,
                    note = trimmedNote,
                    userToken = user.token
                )
            }

            // 3. Notificar a los widgets de la pantalla de inicio
            try {
                com.example.widget.YapeTotalWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updateTransactionNote error", e)
            Result.failure(e)
        }
    }

    suspend fun applyRealtimeTransactionUpdate(record: com.example.data.supabase.RealtimeTxRecord) = withContext(Dispatchers.IO) {
        val config = storePreferences.config.value
        // Solo aplicar si coincide con la tienda activa (ignora mayúsculas/minúsculas)
        if (config.storeCode.isNotBlank() && !config.storeCode.trim().equals(record.storeCode.trim(), ignoreCase = true)) {
            Log.d(TAG, "Ignorando actualización en tiempo real para otra tienda: ${record.storeCode}")
            return@withContext
        }

        val remoteId = record.id.trim()
        val branchName = record.branchName?.trim() ?: ""
        val claimedBy = record.claimedBy?.trim() ?: ""
        val claimedByName = record.claimedByName?.trim() ?: ""
        val note = record.note?.trim()
        val isUnclaim = branchName.isBlank() && claimedBy.isBlank() && claimedByName.isBlank()

        val claimedAtMillis = try {
            if (!record.claimedAt.isNullOrBlank()) {
                val cleanDate = record.claimedAt.take(19)
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .parse(cleanDate)?.time ?: System.currentTimeMillis()
            } else 0L
        } catch (_: Exception) {
            0L
        }

        val existing = com.example.util.TransactionDeduplicator.findDuplicate(
            dao = dao,
            timestamp = record.timestamp,
            amount = record.amount,
            senderName = record.senderName,
            rawNotification = record.rawNotification ?: "",
            storeCode = record.storeCode,
            remoteId = remoteId
        )
        if (existing != null) {
            val resolvedClaimedAt = if (isUnclaim) 0L else (if (claimedAtMillis > 0L) claimedAtMillis else existing.claimedAt)
            val resolvedNote = if (isUnclaim) "" else (note ?: existing.note)

            dao.updateTransactionBranch(
                localId = existing.id,
                remoteId = remoteId,
                branchName = branchName,
                claimedBy = claimedBy,
                claimedByName = claimedByName,
                claimedAt = resolvedClaimedAt,
                note = resolvedNote
            )
            if (existing.remoteId.isBlank() && remoteId.isNotBlank()) {
                dao.linkRemoteId(existing.id, remoteId, System.currentTimeMillis())
            }
            Log.d(TAG, "Realtime update aplicado a tx id=${existing.id}: branch=$branchName, who=$claimedByName, note=$resolvedNote")
        } else {
                val newTx = YapeTransaction(
                    senderName = record.senderName,
                    amount = record.amount,
                    timestamp = record.timestamp,
                    rawNotification = record.rawNotification ?: "",
                    transactionType = record.transactionType.ifBlank { "RECEIVED" },
                    storeCode = record.storeCode,
                    isSynced = true,
                    remoteId = remoteId,
                    syncTimestamp = System.currentTimeMillis(),
                    branchName = branchName,
                    claimedBy = claimedBy,
                    claimedByName = claimedByName,
                    claimedAt = claimedAtMillis,
                    note = note ?: ""
                )
                val insertedId = dao.insert(newTx)
                Log.d(TAG, "Realtime insertado nuevo pago id=$insertedId, remoteId=$remoteId")

                val now = System.currentTimeMillis()
                val isRecent = (now - record.timestamp) < 90_000L
                if (isRecent && config.deviceRole == DeviceRole.RECEIVER) {
                    if (!config.workerIsRevoked && com.example.util.WorkerScheduleHelper.isConfigWithinSchedule(config)) {
                        remoteAlertManager.triggerRemotePaymentAlert(newTx.copy(id = insertedId), config)
                    }
                }
            }

            try {
                YapeTotalWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}
        }

    suspend fun applyRealtimeTransactionDelete(remoteId: String) = withContext(Dispatchers.IO) {
        if (remoteId.isNotBlank()) {
            dao.deleteByRemoteId(remoteId)
            try {
                YapeTotalWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}
            Log.d(TAG, "Realtime eliminada tx remota id=$remoteId")
        }
    }

    suspend fun deleteTransaction(transaction: YapeTransaction) {
        dao.delete(transaction)
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    suspend fun cleanupLocalDuplicates(storeCode: String = ""): Int = withContext(Dispatchers.IO) {
        try {
            val (todayStart, _) = getTodayStartAndEndTime()
            // Examinar transacciones de los últimos 7 días
            val sevenDaysAgo = todayStart - (7 * 24 * 60 * 60 * 1000L)
            val allRecent = dao.getTransactionsBetweenSync(sevenDaysAgo, System.currentTimeMillis() + 86400000L, storeCode)
            if (allRecent.size < 2) return@withContext 0

            val toDeleteIds = mutableListOf<Long>()
            val processed = mutableSetOf<Long>()

            for (i in 0 until allRecent.size) {
                val tx1 = allRecent[i]
                if (processed.contains(tx1.id) || toDeleteIds.contains(tx1.id)) continue

                for (j in i + 1 until allRecent.size) {
                    val tx2 = allRecent[j]
                    if (processed.contains(tx2.id) || toDeleteIds.contains(tx2.id)) continue

                    // Coincidencia de monto
                    if (kotlin.math.abs(tx1.amount - tx2.amount) >= 0.01) continue

                    // Coincidencia de tienda
                    if (tx1.storeCode.isNotBlank() && tx2.storeCode.isNotBlank() &&
                        !tx1.storeCode.trim().equals(tx2.storeCode.trim(), ignoreCase = true)) continue

                    // Criterios de deduplicación
                    val isRemoteMatch = tx1.remoteId.isNotBlank() && tx2.remoteId.isNotBlank() &&
                            tx1.remoteId.equals(tx2.remoteId, ignoreCase = true)

                    val name1 = com.example.util.TransactionDeduplicator.normalizeSenderName(tx1.senderName)
                    val name2 = com.example.util.TransactionDeduplicator.normalizeSenderName(tx2.senderName)
                    val namesMatch = com.example.util.TransactionDeduplicator.isNameMatch(name1, name2)

                    val secCode1 = tx1.securityCode ?: com.example.util.TransactionDeduplicator.extractSecurityCode(tx1.rawNotification)
                    val secCode2 = tx2.securityCode ?: com.example.util.TransactionDeduplicator.extractSecurityCode(tx2.rawNotification)
                    val secMatch = !secCode1.isNullOrBlank() && secCode1 == secCode2

                    val sameDay = com.example.util.TransactionDeduplicator.isSameCalendarDay(tx1.timestamp, tx2.timestamp)
                    val timeDiff = kotlin.math.abs(tx1.timestamp - tx2.timestamp)

                    val isDuplicate = isRemoteMatch || secMatch || (namesMatch && (timeDiff <= 2 * 60 * 60 * 1000L || sameDay))

                    if (isDuplicate) {
                        // Priorizar la transacción que ya tiene remoteId de Supabase o está confirmada
                        val keep: YapeTransaction
                        val discard: YapeTransaction
                        if (tx1.remoteId.isNotBlank() && tx2.remoteId.isBlank()) {
                            keep = tx1
                            discard = tx2
                        } else if (tx2.remoteId.isNotBlank() && tx1.remoteId.isBlank()) {
                            keep = tx2
                            discard = tx1
                        } else if (tx1.isConfirmed && !tx2.isConfirmed) {
                            keep = tx1
                            discard = tx2
                        } else if (tx2.isConfirmed && !tx1.isConfirmed) {
                            keep = tx2
                            discard = tx1
                        } else {
                            if (tx1.id < tx2.id) {
                                keep = tx1
                                discard = tx2
                            } else {
                                keep = tx2
                                discard = tx1
                            }
                        }

                        // Preservar confirmación o notas si el que se descarta las tenía
                        if (discard.isConfirmed && !keep.isConfirmed) {
                            dao.updateTransactionBranch(
                                localId = keep.id,
                                remoteId = keep.remoteId,
                                branchName = discard.branchName,
                                claimedBy = discard.claimedBy,
                                claimedByName = discard.claimedByName,
                                claimedAt = discard.claimedAt,
                                note = discard.note.ifBlank { null }
                            )
                        }

                        toDeleteIds.add(discard.id)
                        processed.add(keep.id)
                        Log.d(TAG, "cleanupLocalDuplicates: Eliminando duplicado huérfano local id=${discard.id} (conservando id=${keep.id}, remoteId=${keep.remoteId})")
                    }
                }
            }

            if (toDeleteIds.isNotEmpty()) {
                dao.deleteByIds(toDeleteIds)
                try {
                    YapeTotalWidgetProvider.updateAllWidgets(context)
                } catch (_: Exception) {}
                Log.d(TAG, "cleanupLocalDuplicates: Se eliminaron ${toDeleteIds.size} cobros duplicados locales")
            }

            toDeleteIds.size
        } catch (e: Exception) {
            Log.e(TAG, "cleanupLocalDuplicates error", e)
            0
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
        try {
            YapeTotalWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "TransactionRepo"
    }
}
