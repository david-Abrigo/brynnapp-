package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthUser
import com.example.data.model.DeviceRole
import com.example.data.model.VoiceSettings
import com.example.data.model.YapeTransaction
import com.example.data.preferences.StoreConfig
import com.example.data.preferences.StorePreferences
import com.example.data.preferences.VoicePreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.TransactionRepository
import com.example.data.supabase.ConnectionStatus
import com.example.data.supabase.SupabaseRealtimeClient
import com.example.service.YapeNotificationListenerService
import com.example.service.parser.ParseResult
import com.example.service.parser.YapeNotificationParser
import com.example.service.remote.FcmNotificationReceiverService
import com.example.data.supabase.SupabaseLinkedStoreDto
import com.example.service.sound.NotificationSoundManager
import com.example.service.sound.NotificationSoundType
import com.example.service.sound.SoundAlertMode
import com.example.service.tts.YapeTtsManager
import com.example.widget.YapeTotalWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)
    private val voicePreferences = VoicePreferences.getInstance(application)
    private val storePreferences = StorePreferences.getInstance(application)
    private val authRepository = AuthRepository.getInstance(application)
    private val ttsManager = YapeTtsManager.getInstance(application)
    private val soundManager = NotificationSoundManager.getInstance(application)

    // User Authentication
    val currentUser: StateFlow<AuthUser> = authRepository.currentUser

    // Store & Cloud Configuration
    val storeConfig: StateFlow<StoreConfig> = storePreferences.config
    val linkedStores: StateFlow<List<SupabaseLinkedStoreDto>> = storePreferences.linkedStores
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val lastSyncError: StateFlow<String?> = repository.lastSyncError

    // Test connection response message
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    private val _fcmTestResult = MutableStateFlow<String?>(null)
    val fcmTestResult: StateFlow<String?> = _fcmTestResult.asStateFlow()

    // Filtro activo de sucursal para el Dueño ("ALL" o nombre de la sucursal)
    private val _selectedBranchFilter = MutableStateFlow("ALL")
    val selectedBranchFilter: StateFlow<String> = _selectedBranchFilter.asStateFlow()

    // RevenueCat Billing & Suscripciones Oficiales
    val isProSubscribed: StateFlow<Boolean> = com.example.data.billing.SubscriptionManager.isProActive

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _showCustomerCenter = MutableStateFlow(false)
    val showCustomerCenter: StateFlow<Boolean> = _showCustomerCenter.asStateFlow()

    fun openPaywall() { _showPaywall.value = true }
    fun closePaywall() { _showPaywall.value = false }
    fun openCustomerCenter() { _showCustomerCenter.value = true }
    fun closeCustomerCenter() { _showCustomerCenter.value = false }

    fun onProSubscriptionPurchased() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user.isLoggedIn && user.id.isNotBlank()) {
                authRepository.updateUserPlan(user.id, "PREMIUM")
            }
            activateTrialPlan { success, _ ->
                if (success) {
                    Log.d("MainViewModel", "Tienda y perfil sincronizados con suscripción Premium.")
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user.isLoggedIn && user.id.isNotBlank()) {
                    com.example.data.billing.SubscriptionManager.loginUser(user.id)
                } else {
                    com.example.data.billing.SubscriptionManager.logoutUser()
                }
            }
        }
    }

    fun setBranchFilter(branch: String) {
        _selectedBranchFilter.value = branch.trim()
    }

    // Flujo de todas las transacciones filtradas según la tienda activa y permisos de historial
    val allTransactions: StateFlow<List<YapeTransaction>> = combine(
        repository.allTransactions,
        storePreferences.config
    ) { list, config ->
        val currentStoreCode = config.storeCode.trim()
        val (todayStart, _) = repository.getTodayStartAndEndTime()
        val storeFiltered = if (currentStoreCode.isNotBlank()) {
            list.filter { it.storeCode.trim().equals(currentStoreCode, ignoreCase = true) || it.storeCode.isBlank() }
        } else {
            list
        }
        if (config.deviceRole == DeviceRole.RECEIVER && !config.allowWorkerHistory) {
            storeFiltered.filter { it.timestamp >= todayStart }
        } else {
            storeFiltered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's reactive streams filtrados según Tienda activa, Rol y Sucursal
    val todayTransactions: StateFlow<List<YapeTransaction>> = combine(
        repository.allTransactions,
        storePreferences.config,
        _selectedBranchFilter
    ) { list, config, branchFilter ->
        val (todayStart, todayEnd) = repository.getTodayStartAndEndTime()
        val currentStoreCode = config.storeCode.trim()
        val storeFiltered = list.filter { tx ->
            tx.timestamp in todayStart..todayEnd &&
            (currentStoreCode.isBlank() || tx.storeCode.trim().equals(currentStoreCode, ignoreCase = true))
        }
        when (config.deviceRole) {
            DeviceRole.RECEIVER -> storeFiltered
            DeviceRole.SENDER -> {
                if (branchFilter == "ALL") {
                    storeFiltered
                } else {
                    storeFiltered.filter { it.branchName.trim().equals(branchFilter, ignoreCase = true) }
                }
            }
            DeviceRole.LOCAL_SPEAKER -> storeFiltered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotal: StateFlow<Double> = todayTransactions.map { list ->
        list.filter { it.isStoreTransaction }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayCount: StateFlow<Int> = todayTransactions.map { list ->
        list.count { it.isStoreTransaction }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayExcludedTotal: StateFlow<Double> = todayTransactions.map { list ->
        list.filter { !it.isStoreTransaction }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExcludedCount: StateFlow<Int> = todayTransactions.map { list ->
        list.count { !it.isStoreTransaction }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestTransaction: StateFlow<YapeTransaction?> = combine(
        repository.allTransactions,
        storePreferences.config
    ) { list, config ->
        val currentStoreCode = config.storeCode.trim()
        val storeList = if (currentStoreCode.isNotBlank()) {
            list.filter { it.storeCode.trim().equals(currentStoreCode, ignoreCase = true) }
        } else {
            list
        }
        storeList.maxByOrNull { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ID del último pago que el usuario descartó manualmente de la card de "Último Pago"
    // -1L = nada descartado. Si coincide con latestTransaction.id, la card se oculta.
    private val _dismissedLastPaymentId = MutableStateFlow(-1L)
    val dismissedLastPaymentId: StateFlow<Long> = _dismissedLastPaymentId.asStateFlow()

    fun dismissLastPayment() {
        _dismissedLastPaymentId.value = latestTransaction.value?.id ?: -1L
    }

    // Last 7 days transactions for trends, filtrados por tienda, rol y sucursal
    val weeklyTransactions: StateFlow<List<YapeTransaction>> = combine(
        repository.allTransactions,
        storePreferences.config,
        _selectedBranchFilter
    ) { list, config, branchFilter ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        val currentStoreCode = config.storeCode.trim()
        val weeklyList = list.filter { tx ->
            tx.timestamp in startTime..endTime &&
            (currentStoreCode.isBlank() || tx.storeCode.trim().equals(currentStoreCode, ignoreCase = true))
        }
        when (config.deviceRole) {
            DeviceRole.RECEIVER -> {
                if (!config.allowWorkerHistory) {
                    val (todayStart, _) = repository.getTodayStartAndEndTime()
                    weeklyList.filter { it.timestamp >= todayStart }
                } else {
                    weeklyList
                }
            }
            DeviceRole.SENDER -> {
                if (branchFilter == "ALL") {
                    weeklyList
                } else {
                    weeklyList.filter { it.branchName.trim().equals(branchFilter, ignoreCase = true) }
                }
            }
            DeviceRole.LOCAL_SPEAKER -> weeklyList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Settings
    val voiceSettings: StateFlow<VoiceSettings> = voicePreferences.settings

    // Permission states
    private val _isNotificationListenerGranted = MutableStateFlow(false)
    val isNotificationListenerGranted: StateFlow<Boolean> = _isNotificationListenerGranted.asStateFlow()

    private val _isPostNotificationGranted = MutableStateFlow(true)
    val isPostNotificationGranted: StateFlow<Boolean> = _isPostNotificationGranted.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(true)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    // History selection state
    private val _selectedHistoryDate = MutableStateFlow(Calendar.getInstance())
    val selectedHistoryDate: StateFlow<Calendar> = _selectedHistoryDate.asStateFlow()

    val historyTransactions: StateFlow<List<YapeTransaction>> = combine(
        _selectedHistoryDate,
        repository.allTransactions,
        storePreferences.config,
        _selectedBranchFilter
    ) { cal, list, config, branchFilter ->
        val (dayStart, dayEnd) = repository.getDayStartAndEndTime(cal)
        val currentStoreCode = config.storeCode.trim()
        val dayList = list.filter { tx ->
            tx.timestamp in dayStart..dayEnd &&
            (currentStoreCode.isBlank() || tx.storeCode.trim().equals(currentStoreCode, ignoreCase = true))
        }
        when (config.deviceRole) {
            DeviceRole.RECEIVER -> {
                if (!config.allowWorkerHistory) {
                    val (todayStart, _) = repository.getTodayStartAndEndTime()
                    dayList.filter { it.timestamp >= todayStart }
                } else {
                    dayList
                }
            }
            DeviceRole.SENDER -> {
                if (branchFilter == "ALL") {
                    dayList
                } else {
                    dayList.filter { it.branchName.trim().equals(branchFilter, ignoreCase = true) }
                }
            }
            DeviceRole.LOCAL_SPEAKER -> dayList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyTotal: StateFlow<Double> = historyTransactions.map { list ->
        list.filter { it.isStoreTransaction }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val historyExcludedTotal: StateFlow<Double> = historyTransactions.map { list ->
        list.filter { !it.isStoreTransaction }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val historyExcludedCount: StateFlow<Int> = historyTransactions.map { list ->
        list.count { !it.isStoreTransaction }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Simulation feedback message
    private val _simulationFeedback = MutableStateFlow<String?>(null)
    val simulationFeedback: StateFlow<String?> = _simulationFeedback.asStateFlow()

    private var syncJob: Job? = null
    private var realtimeJob: Job? = null

    // Cliente Supabase Realtime (WebSocket Phoenix) — reemplaza el polling en modo Dueño
    private val realtimeClient: SupabaseRealtimeClient by lazy {
        val cfg = storePreferences.config.value
        SupabaseRealtimeClient(
            supabaseUrl = cfg.supabaseUrl,
            anonKey = cfg.supabaseAnonKey,
            getAuthToken = { authRepository.getFreshToken() }
        )
    }

    init {
        checkPermission()
        val current = storePreferences.config.value
        FcmNotificationReceiverService.subscribeToStoreTopic(current.storeCode)
        startSyncWorker()

        viewModelScope.launch(Dispatchers.IO) {
            if (current.isConfigured && current.isCloudEnabled) {
                if (current.deviceRole == DeviceRole.RECEIVER) {
                    repository.syncRemoteReceiver(triggerAlerts = false, fullHistorySync = true)
                } else {
                    repository.syncPendingUploads()
                    repository.syncHistoricalSalesOnLogin()
                }
            }
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user.isLoggedIn) {
                    authRepository.getFreshToken()
                    loadMyLinkedStores()
                }
            }
        }

        // Supabase Realtime: conectar tanto Dueño como Trabajadores para sincronización instantánea
        if (current.storeCode.isNotBlank()) {
            realtimeClient.connect(current.storeCode)
            startObservingRealtimeEvents()
        }
    }

    fun checkPermission() {
        val context = getApplication<Application>()
        _isNotificationListenerGranted.value =
            com.example.util.PermissionHelper.isNotificationListenerEnabled(context)
        _isPostNotificationGranted.value =
            com.example.util.PermissionHelper.isPostNotificationGranted(context)
        _isBatteryOptimizationIgnored.value =
            com.example.util.PermissionHelper.isBatteryOptimizationIgnored(context)
    }

    private fun startSyncWorker() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            var loopCount = 0
            while (isActive) {
                val config = storePreferences.config.value
                if (config.isConfigured && config.isCloudEnabled) {
                    if (config.deviceRole == DeviceRole.RECEIVER) {
                        // Respaldo pasivo de reconciliación cada ~30s (FCM se encarga del tiempo real)
                        repository.syncRemoteReceiver(triggerAlerts = true, fullHistorySync = false)
                        // Verificación de estado de receptor cada ~60s (FCM worker_revoked actúa al instante)
                        if (loopCount % 2 == 0) {
                            repository.checkMyReceiverStatus()
                        }
                    } else {
                        // En Modo Dueño, los pagos se suben y difunden por FCM de inmediato al ocurrir la notificación.
                        // syncPendingUploads actúa como reintento de contingencia si hubo pagos offline.
                        repository.syncPendingUploads()
                    }
                }
                loopCount++
                // Intervalo de 30 segundos con jitter aleatorio (±3s) para desincronizar peticiones y prevenir colisiones
                val jitter = kotlin.random.Random.nextLong(-3000L, 3001L)
                delay((30_000L + jitter).coerceAtLeast(15_000L))
            }
        }
    }

    /**
     * Observa los eventos de cambio de Supabase Realtime para:
     * 1. store_receivers: recarga trabajadores en Dueño y verifica estado en Trabajadores.
     * 2. yape_transactions: sincroniza cobros, confirmaciones y notas en tiempo real para todos.
     */
    private fun startObservingRealtimeEvents() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            // 1. Cambios en store_receivers
            launch {
                var lastReceiverEvent = 0L
                realtimeClient.receiverChangeEvent.collect { timestamp ->
                    if (timestamp > 0L && timestamp != lastReceiverEvent) {
                        lastReceiverEvent = timestamp
                        val cfg = storePreferences.config.value
                        if (cfg.deviceRole == DeviceRole.SENDER) {
                            android.util.Log.d("MainViewModel", "Realtime: cambio en trabajadores -> recargando lista")
                            loadStoreReceivers(silent = true)
                        } else if (cfg.deviceRole == DeviceRole.RECEIVER) {
                            android.util.Log.d("MainViewModel", "Realtime: cambio en receptores -> verificando mi estado")
                            checkMyReceiverStatus()
                        }
                    }
                }
            }

            // 2. Registro directo en tiempo real de yape_transactions (0ms latencia para confirmaciones y producto vendido)
            launch {
                realtimeClient.realtimeTxUpdate.collect { record ->
                    android.util.Log.d("MainViewModel", "Realtime Tx Record recibido: id=${record.id}, branch=${record.branchName}, who=${record.claimedByName}, note=${record.note}")
                    repository.applyRealtimeTransactionUpdate(record)
                }
            }

            // 3. Eliminación directa en tiempo real de yape_transactions
            launch {
                realtimeClient.realtimeTxDelete.collect { remoteId ->
                    android.util.Log.d("MainViewModel", "Realtime Tx Delete recibido: remoteId=$remoteId")
                    repository.applyRealtimeTransactionDelete(remoteId)
                }
            }

            // 4. Sincronización de respaldo periódica por eventos en yape_transactions
            launch {
                var lastTxEvent = 0L
                realtimeClient.transactionChangeEvent.collect { timestamp ->
                    if (timestamp > 0L && timestamp != lastTxEvent) {
                        lastTxEvent = timestamp
                        android.util.Log.d("MainViewModel", "Realtime: cambio en transacciones/cobros -> sincronizando")
                        repository.syncRemoteReceiver(triggerAlerts = false, fullHistorySync = false)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeClient.destroy()
    }

    fun updateStoreConfig(newConfig: StoreConfig) {
        val oldConfig = storePreferences.config.value
        val codeChanged = oldConfig.storeCode != newConfig.storeCode
        val roleChanged = oldConfig.deviceRole != newConfig.deviceRole

        val configToSave = if (codeChanged || (roleChanged && newConfig.deviceRole == DeviceRole.RECEIVER)) {
            newConfig.copy(lastSyncTimestamp = 0L)
        } else {
            newConfig
        }

        storePreferences.updateConfig(configToSave)
        if (codeChanged) {
            if (oldConfig.storeCode.isNotBlank()) {
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(oldConfig.storeCode)
            }
            if (newConfig.storeCode.isNotBlank()) {
                FcmNotificationReceiverService.subscribeToStoreTopic(newConfig.storeCode)
            }
        }
        try {
            YapeTotalWidgetProvider.updateAllWidgets(getApplication())
        } catch (_: Exception) {}

        viewModelScope.launch(Dispatchers.IO) {
            if (newConfig.deviceRole == DeviceRole.RECEIVER) {
                if (newConfig.storeCode.isNotBlank()) {
                    val localCount = repository.getStoreLocalTransactionsCount(newConfig.storeCode)
                    val needFullSync = localCount == 0
                    val count = repository.syncRemoteReceiver(triggerAlerts = false, fullHistorySync = needFullSync)
                    _simulationFeedback.value = if (count > 0) "¡$count pagos de la tienda precargados con éxito!" else "Conectado a la tienda (al día)"
                }
            } else {
                repository.syncPendingUploads()
            }
        }

        // Reconectar Realtime si cambió el storeCode o rol (aplica tanto a Dueño como a Trabajador)
        if (newConfig.storeCode.isNotBlank()) {
            if (codeChanged || roleChanged) {
                realtimeClient.connect(newConfig.storeCode)
                startObservingRealtimeEvents()
            }
        } else {
            realtimeClient.disconnect()
        }
    }

    fun prepareSwitchToReceiver() {
        val current = storePreferences.config.value
        if (current.storeCode.isNotBlank()) {
            FcmNotificationReceiverService.unsubscribeFromStoreTopic(current.storeCode)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
        val updated = current.copy(
            deviceRole = DeviceRole.RECEIVER,
            storeCode = "",
            storeName = "Sin tienda vinculada",
            workerBranchName = "",
            isOnboarded = false,
            lastSyncTimestamp = 0L,
            workerIsRevoked = false,
            isCloudEnabled = true
        )
        storePreferences.updateConfig(updated)
        storePreferences.setOnboarded(false)
    }

    fun updateStoreName(newName: String) {
        val trimmed = newName.trim().ifBlank { "Mi Negocio" }
        val current = storePreferences.config.value
        updateStoreConfig(current.copy(storeName = trimmed))
    }

    fun setDeviceRole(role: DeviceRole) {
        if (role == DeviceRole.RECEIVER) {
            prepareSwitchToReceiver()
            return
        }
        val current = storePreferences.config.value
        updateStoreConfig(current.copy(deviceRole = role))
    }

    fun generateNewStoreCode() {
        val current = storePreferences.config.value
        val newCode = "TIENDA-${(1000..9999).random()}"
        updateStoreConfig(current.copy(storeCode = newCode))
    }

    fun syncNow() {
        viewModelScope.launch {
            val config = storePreferences.config.value
            if (config.deviceRole == DeviceRole.RECEIVER) {
                val count = repository.syncRemoteReceiver(triggerAlerts = false, fullHistorySync = true)
                _simulationFeedback.value = if (count > 0) "¡$count pagos sincronizados desde la nube!" else "Sincronizado con la nube (al día)"
            } else {
                val count = repository.syncPendingUploads()
                _simulationFeedback.value = if (count > 0) "¡$count pagos pendientes subidos a Supabase!" else "Sincronizado con la nube"
            }
        }
    }

    fun testSupabaseCredentials(url: String, anonKey: String) {
        viewModelScope.launch {
            _testConnectionResult.value = "Probando conexión con Supabase..."
            val result = repository.testSupabaseConnection(url, anonKey)
            if (result.isSuccess) {
                _testConnectionResult.value = "✅ ¡Conexión con Supabase exitosa!"
            } else {
                _testConnectionResult.value = "❌ Error al conectar: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun testFcmPush() {
        viewModelScope.launch {
            val config = storePreferences.config.value
            _fcmTestResult.value = "Enviando mensaje push de prueba a FCM..."
            val dummyTx = YapeTransaction(
                id = System.currentTimeMillis(),
                amount = 15.50,
                senderName = "Prueba FCM",
                rawNotification = "¡Te yapearon! Prueba FCM te envió S/ 15.50",
                timestamp = System.currentTimeMillis(),
                storeCode = config.storeCode,
                note = "Test Push"
            )
            val success = com.example.data.fcm.FcmSender.sendPaymentPush(dummyTx, config)
            if (success) {
                _fcmTestResult.value = "✅ ¡Push FCM enviado con éxito a '${com.example.data.fcm.FcmServiceAccountConfig.sanitizeTopicName(config.storeCode)}'! Si hay dispositivos Receptores activos, sonarán de inmediato."
            } else {
                _fcmTestResult.value = "❌ Error enviando a FCM. Verifica la conexión a Internet o el token."
            }
        }
    }

    fun clearTestConnectionResult() {
        _testConnectionResult.value = null
        _fcmTestResult.value = null
    }

    fun updateVoiceSettings(newSettings: VoiceSettings) {
        voicePreferences.updateSettings(newSettings)
        val currentStore = storePreferences.config.value
        if (currentStore.remoteTtsTemplate != newSettings.messageTemplate) {
            storePreferences.updateConfig(currentStore.copy(remoteTtsTemplate = newSettings.messageTemplate))
        }
    }

    fun testTts(customText: String? = null) {
        val voiceSettings = voicePreferences.loadSettings()
        val soundType = NotificationSoundType.fromKey(voiceSettings.notificationSound)
        val soundAlertMode = SoundAlertMode.fromKey(voiceSettings.soundAlertMode)
        val willSpeakVoice = voiceSettings.voiceEnabled

        val shouldPlaySound = soundAlertMode == SoundAlertMode.ALWAYS || (soundAlertMode == SoundAlertMode.WHEN_VOICE_DISABLED && !willSpeakVoice)

        if (shouldPlaySound) {
            soundManager.playSound(
                soundType = soundType,
                volume = voiceSettings.soundVolume,
                vibrate = voiceSettings.vibrateWithSound,
                keepFocusForCallback = willSpeakVoice,
                onFinished = {
                    if (willSpeakVoice) {
                        ttsManager.speakPreview(customText)
                    }
                }
            )
        } else if (willSpeakVoice) {
            ttsManager.speakPreview(customText)
        }
    }

    fun repeatTransactionAnnouncement(transaction: YapeTransaction) {
        ttsManager.speakPayment(
            senderName = transaction.senderName,
            amount = transaction.amount,
            isRemote = transaction.isRemote
        )
    }

    fun playNotificationSoundPreview(soundTypeKey: String? = null) {
        val current = voicePreferences.loadSettings()
        val key = soundTypeKey ?: current.notificationSound
        val soundType = NotificationSoundType.fromKey(key)
        soundManager.playSound(
            soundType = soundType,
            volume = current.soundVolume,
            vibrate = current.vibrateWithSound
        )
    }

    fun selectHistoryDate(calendar: Calendar) {
        val newCal = calendar.clone() as Calendar
        _selectedHistoryDate.value = newCal
    }

    fun changeHistoryDay(daysOffset: Int) {
        val current = _selectedHistoryDate.value.clone() as Calendar
        current.add(Calendar.DAY_OF_MONTH, daysOffset)
        _selectedHistoryDate.value = current
    }

    fun resetHistoryToToday() {
        _selectedHistoryDate.value = Calendar.getInstance()
    }

    fun simulateNotification(senderName: String, amount: Double, customNotificationText: String? = null) {
        viewModelScope.launch {
            val config = storePreferences.config.value
            val textToParse = customNotificationText
                ?: "¡Te yapearon! $senderName te envió S/ ${String.format(Locale.US, "%.2f", amount)}"

            val parseResult = YapeNotificationParser.parse(
                title = "Yape",
                text = textToParse,
                packageName = "com.bcp.innovacxion.yapeapp"
            )

            when (parseResult) {
                is ParseResult.Success -> {
                    val tx = YapeTransaction(
                        senderName = parseResult.senderName,
                        amount = parseResult.amount,
                        timestamp = System.currentTimeMillis(),
                        rawNotification = parseResult.rawNotification,
                        transactionType = "RECEIVED",
                        storeCode = config.storeCode
                    )
                    repository.insertTransaction(tx)

                    val voiceSettings = voicePreferences.loadSettings()
                    val soundType = NotificationSoundType.fromKey(voiceSettings.notificationSound)
                    val soundAlertMode = SoundAlertMode.fromKey(voiceSettings.soundAlertMode)
                    val willSpeakVoice = voiceSettings.voiceEnabled && voiceSettings.announceLocalYape

                    val shouldPlaySound = soundAlertMode == SoundAlertMode.ALWAYS || (soundAlertMode == SoundAlertMode.WHEN_VOICE_DISABLED && !willSpeakVoice)
                    if (shouldPlaySound) {
                        soundManager.playSound(
                            soundType = soundType,
                            volume = voiceSettings.soundVolume,
                            vibrate = voiceSettings.vibrateWithSound,
                            keepFocusForCallback = willSpeakVoice,
                            onFinished = {
                                if (willSpeakVoice) {
                                    ttsManager.speakPayment(parseResult.senderName, parseResult.amount)
                                }
                            }
                        )
                    } else if (willSpeakVoice) {
                        ttsManager.speakPayment(parseResult.senderName, parseResult.amount)
                    }

                    YapeTotalWidgetProvider.updateAllWidgets(getApplication())
                    _simulationFeedback.value = "¡Yape de S/ ${parseResult.amount} registrado y sincronizado con éxito!"
                }

                is ParseResult.Ignored -> {
                    _simulationFeedback.value = "Ignorado: ${parseResult.reason}"
                }
            }
        }
    }

    fun clearFeedback() {
        _simulationFeedback.value = null
    }

    fun deleteTransaction(transaction: YapeTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun toggleStoreTransactionStatus(transaction: YapeTransaction, reason: String = "") {
        if (transaction.isConfirmed && !canCurrentUserUnclaim(transaction)) {
            val author = transaction.claimedByName.ifBlank { "el trabajador que lo confirmó" }
            _simulationFeedback.value = "⚠️ Solo $author puede modificar este cobro"
            return
        }
        viewModelScope.launch {
            val newStatus = !transaction.isStoreTransaction
            repository.updateStoreTransactionStatus(transaction.id, newStatus, reason)
            _simulationFeedback.value = if (newStatus) {
                "✅ Pago marcado como parte de la tienda"
            } else {
                "⚠️ Pago marcado como desconocido (Excluido de caja)"
            }
        }
    }

    fun markTransactionAsNonStore(transaction: YapeTransaction, reason: String = "Desconocido / No del negocio") {
        if (transaction.isConfirmed && !canCurrentUserUnclaim(transaction)) {
            val author = transaction.claimedByName.ifBlank { "el trabajador que lo confirmó" }
            _simulationFeedback.value = "⚠️ Solo $author puede excluir este cobro"
            return
        }
        viewModelScope.launch {
            if (transaction.isConfirmed) {
                val branch = transaction.branchName.ifBlank { storeConfig.value.workerBranchName.ifBlank { "Principal" } }
                try {
                    repository.unclaimTransactionBranch(transaction, branch)
                } catch (_: Exception) {}
            }
            repository.updateStoreTransactionStatus(transaction.id, isStore = false, reason = reason)
            _simulationFeedback.value = "⚠️ Yape de ${transaction.formattedAmount} excluido de las ventas de la tienda"
        }
    }

    fun markTransactionAsStore(transaction: YapeTransaction) {
        viewModelScope.launch {
            repository.updateStoreTransactionStatus(transaction.id, isStore = true, reason = "")
            _simulationFeedback.value = "✅ Yape de ${transaction.formattedAmount} reintegrado a las ventas de la tienda"
        }
    }

    fun insertMockTransactionsForToday() {
        viewModelScope.launch {
            val config = storePreferences.config.value
            val cal = Calendar.getInstance()

            val mockData = listOf(
                Triple("Carlos Mendoza", 12.50, 9),  // 9:15 AM
                Triple("Lucía Ramos", 35.00, 10),   // 10:30 AM
                Triple("Jorge Quintana", 8.00, 11),  // 11:15 AM
                Triple("Rosa Huamán", 65.00, 12),   // 12:45 PM
                Triple("Miguel Quispe", 18.00, 14),  // 2:20 PM
                Triple("Fiorella Castro", 85.50, 16),// 4:10 PM
                Triple("David Paredes", 22.00, 17)   // 5:05 PM
            )

            mockData.forEachIndexed { index, (name, amount, hour) ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, 10 + (index * 7) % 50)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val tx = YapeTransaction(
                    senderName = name,
                    amount = amount,
                    timestamp = cal.timeInMillis,
                    rawNotification = "¡Te yapearon! $name te envió S/ ${String.format(Locale.US, "%.2f", amount)}",
                    transactionType = "RECEIVED",
                    storeCode = config.storeCode,
                    isStoreTransaction = true
                )
                repository.insertTransaction(tx)
            }
            YapeTotalWidgetProvider.updateAllWidgets(getApplication())
            _simulationFeedback.value = "✅ Se insertaron 7 pagos de prueba para el día de hoy"
        }
    }

    fun generateDailySummaryShareText(): String {
        val total = todayTotal.value
        val count = todayCount.value
        val txs = todayTransactions.value
        val storeTxs = txs.filter { it.isStoreTransaction }
        val excludedTxs = txs.filter { !it.isStoreTransaction }
        val excludedTotal = todayExcludedTotal.value

        val config = storePreferences.config.value
        val dateStr = SimpleDateFormat("EEEE dd 'de' MMMM, yyyy", Locale("es", "PE")).format(Date())

        val averageTicket = if (count > 0) total / count else 0.0

        val sb = StringBuilder()
        sb.append("📊 *REPORTE DE CAJA*\n")
        sb.append("🏪 *Tienda:* ${config.storeName} (${config.storeCode})\n")
        sb.append("📅 *Fecha:* $dateStr\n")
        sb.append("💰 *Total Ventas Tienda:* S/ ${String.format(Locale("es", "PE"), "%.2f", total)}\n")
        sb.append("🔢 *Ventas Registradas:* $count pagos\n")
        sb.append("🎯 *Ticket Promedio:* S/ ${String.format(Locale("es", "PE"), "%.2f", averageTicket)}\n\n")

        if (storeTxs.isNotEmpty()) {
            sb.append("📝 *Ventas de Tienda:*\n")
            storeTxs.forEachIndexed { index, tx ->
                val noteStr = if (tx.note.isNotBlank()) " [${tx.note}]" else ""
                val workerStr = if (tx.claimedByName.isNotBlank()) " (Atendió: ${tx.claimedByName})" else ""
                sb.append("${index + 1}. ${tx.formattedTime} - ${tx.senderName}$noteStr: *${tx.formattedAmount}*$workerStr\n")
            }
        } else {
            sb.append("No se registraron ventas de tienda hoy.\n")
        }

        if (excludedTxs.isNotEmpty()) {
            sb.append("\n🚫 *YAPES EXCLUIDOS / DESCONOCIDOS (NO SUMADOS):*\n")
            sb.append("Total Excluido: S/ ${String.format(Locale("es", "PE"), "%.2f", excludedTotal)} (${excludedTxs.size} pagos)\n")
            excludedTxs.forEachIndexed { index, tx ->
                val reasonTag = if (tx.exclusionReason.isNotBlank()) " [${tx.exclusionReason}]" else " [No tienda]"
                sb.append("• ${tx.formattedTime} - ${tx.senderName}: ${tx.formattedAmount}$reasonTag\n")
            }
        }

        val genTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date())
        sb.append("\n_Reporte oficial emitido el ${genTime}_")
        return sb.toString()
    }

    // --- Authentication Actions ---

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.signInWithEmail(email, pass)
            if (res.isSuccess) {
                _simulationFeedback.value = "¡Bienvenido ${res.getOrNull()?.displayName ?: ""}!"
                // Mostrar siempre la pantalla de bienvenida con los 3 modos al iniciar sesión
                storePreferences.setOnboarded(false)
                loadMyLinkedStores()
                repository.syncHistoricalSalesOnLogin()
                onResult(true, null)
            } else {
                val errorMsg = res.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                onResult(false, errorMsg)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String, whatsapp: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.signUpWithEmail(email, pass, displayName, whatsapp)
            if (res.isSuccess) {
                _simulationFeedback.value = "¡Cuenta creada exitosamente!"
                storePreferences.setOnboarded(false)
                loadMyLinkedStores()
                repository.syncHistoricalSalesOnLogin()
                onResult(true, null)
            } else {
                val errorMsg = res.exceptionOrNull()?.message ?: "Error al registrar cuenta"
                onResult(false, errorMsg)
            }
        }
    }

    fun signInWithGoogle(
        email: String,
        name: String,
        photo: String = "",
        idToken: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = authRepository.signInWithGoogle(email, name, photo, idToken)
            if (res.isSuccess) {
                _simulationFeedback.value = "¡Conectado con Google!"
                storePreferences.setOnboarded(false)
                loadMyLinkedStores()
                repository.syncHistoricalSalesOnLogin()
                onResult(true, null)
            } else {
                val errorMsg = res.exceptionOrNull()?.message ?: "No se pudo conectar con Google"
                onResult(false, errorMsg)
            }
        }
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            authRepository.refreshUserProfile()
        }
    }

    fun continueAsGuest() {
        val user = authRepository.continueAsGuest()
        storePreferences.setOnboarded(true)
        _simulationFeedback.value = "Ingresado en modo invitado"
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.sendPasswordReset(email)
            if (res.isSuccess) {
                onResult(true, "Enlace de recuperación enviado al correo")
            } else {
                onResult(false, "No se pudo enviar el correo de recuperación")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val currentStore = storeConfig.value.storeCode
            if (currentStore.isNotBlank()) {
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(currentStore)
            }
            // Eliminar transacciones locales y limpiar estado al cerrar sesión
            repository.clearLocalDataOnSignOut()
            authRepository.signOut()
            storePreferences.setOnboarded(false)
            storePreferences.clearRevokedStoreCodes()
            _simulationFeedback.value = "Sesión cerrada"
        }
    }

    fun resetOnboarding() {
        storePreferences.setOnboarded(false)
    }

    fun resetToModeSelection() {
        viewModelScope.launch {
            val current = storeConfig.value
            if (current.deviceRole == DeviceRole.RECEIVER && current.storeCode.isNotBlank()) {
                storePreferences.saveWorkerStoreInfo(
                    code = current.storeCode,
                    name = current.storeName,
                    branch = current.workerBranchName
                )
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(current.storeCode)
                repository.clearAll()
            } else if (current.deviceRole == DeviceRole.SENDER && current.storeCode.isNotBlank()) {
                storePreferences.saveOwnerStoreInfo(
                    code = current.storeCode,
                    name = current.storeName,
                    branches = current.storeBranches
                )
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(current.storeCode)
                repository.clearAll()
            }
            val updated = current.copy(
                isOnboarded = false,
                storeCode = "",
                storeName = if (current.deviceRole == DeviceRole.RECEIVER) "Sin tienda vinculada" else current.storeName,
                workerBranchName = "",
                workerIsRevoked = false
            )
            storePreferences.updateConfig(updated)
            storePreferences.setOnboarded(false)
            _simulationFeedback.value = "Redirigiendo a selección de modo..."
        }
    }

    fun activateSoloLocal() {
        viewModelScope.launch {
            updateStoreConfig(
                storeConfig.value.copy(
                    deviceRole = DeviceRole.LOCAL_SPEAKER,
                    isOnboarded = true
                )
            )
            storePreferences.setOnboarded(true)
            _simulationFeedback.value = "Modo Solo Local activado"
        }
    }

    fun activateTrialPlan(onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val user = currentUser.value
            val currentConfig = storeConfig.value
            val isComingFromReceiver = currentConfig.deviceRole == DeviceRole.RECEIVER
            val previousStoreCode = currentConfig.storeCode

            // 1. Si venía de modo receptor, guardar sus datos de trabajador para preservarlos
            if (isComingFromReceiver && previousStoreCode.isNotBlank()) {
                storePreferences.saveWorkerStoreInfo(
                    code = previousStoreCode,
                    name = currentConfig.storeName,
                    branch = currentConfig.workerBranchName
                )
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(previousStoreCode)
                repository.clearAll()
            }

            var targetStoreCode = ""
            var targetStoreName = "Mi Negocio"
            var targetBranches = "Principal,Sucursal 2"

            // 2. Verificar si ya tenemos en caché local la tienda de dueño
            val cachedOwner = storePreferences.getOwnerStoreInfo()
            if (cachedOwner != null && cachedOwner.first.isNotBlank()) {
                targetStoreCode = cachedOwner.first
                targetStoreName = cachedOwner.second
                targetBranches = cachedOwner.third
            }

            if (user.isLoggedIn && user.id.isNotBlank()) {
                // 3. Consultar o crear la tienda propia en Supabase usando createOrGetOwnerStore (RPC)
                val rpcRes = repository.createOrGetOwnerStore(targetStoreName, targetBranches)
                val ownerStore = rpcRes.getOrNull()

                if (ownerStore != null && ownerStore.storeCode.isNotBlank()) {
                    targetStoreCode = ownerStore.storeCode
                    targetStoreName = ownerStore.storeName.ifBlank { targetStoreName }
                    targetBranches = ownerStore.branches.ifBlank { targetBranches }
                } else {
                    // Fallback 1: Verificar si ya existe una tienda en la tabla stores registrada para este usuario
                    val storeRes = repository.getStoreByOwner(user.id)
                    val existingOwnerStore = storeRes.getOrNull()

                    if (existingOwnerStore != null && existingOwnerStore.storeCode.isNotBlank()) {
                        targetStoreCode = existingOwnerStore.storeCode
                        targetStoreName = existingOwnerStore.storeName.ifBlank { "Mi Negocio" }
                        targetBranches = existingOwnerStore.branches.ifBlank { "Principal,Sucursal 2" }
                    } else {
                        // El usuario NO tiene tienda persistida en Supabase.
                        // Debemos registrarla activamente en la nube vía REST.
                        val newCode = if (targetStoreCode.isNotBlank()) targetStoreCode else "STR_" + java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
                        val newName = if (user.displayName.isNotBlank()) "Tienda de ${user.displayName}" else targetStoreName

                        val regRes = repository.registerNewOwnerStore(
                            storeCode = newCode,
                            storeName = newName,
                            ownerId = user.id,
                            branches = targetBranches
                        )

                        if (regRes.isSuccess) {
                            targetStoreCode = newCode
                            targetStoreName = newName
                        } else {
                            // Si falló (ej. colisión con código previo), reintentar con código limpio
                            val freshCode = "STR_" + java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
                            val retryRes = repository.registerNewOwnerStore(
                                storeCode = freshCode,
                                storeName = newName,
                                ownerId = user.id,
                                branches = targetBranches
                            )
                            if (retryRes.isSuccess) {
                                targetStoreCode = freshCode
                                targetStoreName = newName
                            } else {
                                val errorMsg = retryRes.exceptionOrNull()?.message 
                                    ?: regRes.exceptionOrNull()?.message 
                                    ?: "No se pudo conectar a la base de datos para registrar la tienda"
                                Log.e("MainViewModel", "activateTrialPlan error: $errorMsg")
                                _simulationFeedback.value = "Error al registrar tienda: $errorMsg"
                                onResult?.invoke(false, errorMsg)
                                return@launch
                            }
                        }
                    }
                }
            } else if (targetStoreCode.isBlank()) {
                targetStoreCode = "STR_" + java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
            }

            // Guardar permanentemente la tienda de dueño en preferencias
            storePreferences.saveOwnerStoreInfo(targetStoreCode, targetStoreName, targetBranches)

            // Si la tienda asignada es distinta a la previa, asegurar limpieza de Room y FCM
            if (previousStoreCode.isNotBlank() && previousStoreCode != targetStoreCode) {
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(previousStoreCode)
                repository.clearAll()
            }

            val updated = currentConfig.copy(
                deviceRole = DeviceRole.SENDER,
                storeCode = targetStoreCode,
                storeName = targetStoreName,
                storeBranches = targetBranches,
                isCloudEnabled = true,
                isOnboarded = true,
                workerBranchName = "",
                workerIsRevoked = false
            )
            updateStoreConfig(updated)
            storePreferences.setOnboarded(true)

            if (targetStoreCode.isNotBlank()) {
                FcmNotificationReceiverService.subscribeToStoreTopic(targetStoreCode)
                val count = repository.syncAllStoreTransactions(targetStoreCode)
                _simulationFeedback.value = if (count > 0) "¡$count ventas sincronizadas!" else "🎉 ¡Modo Dueño activado!"
            } else {
                _simulationFeedback.value = "🎉 ¡Modo Dueño activado!"
            }
            onResult?.invoke(true, null)
        }
    }

    fun loadMyLinkedStores(onComplete: (List<SupabaseLinkedStoreDto>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val res = repository.fetchMyLinkedStores()
                val list = res.getOrNull() ?: storePreferences.linkedStores.value
                onComplete(list)
            } catch (_: Exception) {
                onComplete(storePreferences.linkedStores.value)
            }
        }
    }

    fun isStoreRevoked(storeCode: String): Boolean = storePreferences.isStoreRevoked(storeCode)

    fun selectActiveStore(
        storeCode: String,
        storeName: String,
        branchName: String = "Principal",
        skipRevocationCheck: Boolean = false,
        onValidationResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val trimmedCode = storeCode.trim()
            // 0. Bloqueo inmediato si la tienda está en la blacklist local de revocadas
            if (storePreferences.isStoreRevoked(trimmedCode)) {
                storePreferences.removeLinkedStore(trimmedCode)
                val errMsg = "No tienes acceso a '$storeName'. Tu vinculación fue cancelada por el dueño."
                _simulationFeedback.value = "❌ $errMsg"
                onValidationResult?.invoke(false, errMsg)
                return@launch
            }

            // 1. Validar en Supabase si este usuario sigue teniendo acceso a la tienda
            if (!skipRevocationCheck) {
                val authUser = currentUser.value
                if (authUser.isLoggedIn && authUser.id.isNotBlank()) {
                    val statusRes = repository.checkMyReceiverStatus(trimmedCode)
                    if (statusRes.isSuccess) {
                        val receiver = statusRes.getOrNull()
                        val isRevoked = receiver != null && receiver.status.equals("REVOKED", ignoreCase = true)
                        if (isRevoked) {
                            storePreferences.addRevokedStoreCode(trimmedCode)
                            storePreferences.removeLinkedStore(trimmedCode)
                            val errMsg = "No tienes acceso a '$storeName'. Tu vinculación fue cancelada por el dueño."
                            _simulationFeedback.value = "❌ $errMsg"
                            onValidationResult?.invoke(false, errMsg)
                            return@launch
                        }
                    }
                }
            }

            val current = storeConfig.value
            val previousStoreCode = current.storeCode
            val isComingFromSender = current.deviceRole == DeviceRole.SENDER

            // Si venía de modo dueño, preservar los datos de la tienda propia
            if (isComingFromSender && previousStoreCode.isNotBlank()) {
                storePreferences.saveOwnerStoreInfo(
                    code = previousStoreCode,
                    name = current.storeName,
                    branches = current.storeBranches
                )
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(previousStoreCode)
                repository.clearAll()
            }

            // Guardar permanentemente la tienda de trabajador en preferencias
            storePreferences.saveWorkerStoreInfo(
                code = trimmedCode,
                name = storeName,
                branch = branchName
            )
            storePreferences.setWorkerRevoked(false)
            storePreferences.removeRevokedStoreCode(trimmedCode)

            val updated = current.copy(
                storeCode = trimmedCode,
                storeName = storeName,
                workerBranchName = branchName,
                deviceRole = DeviceRole.RECEIVER,
                isCloudEnabled = true,
                isOnboarded = true,
                workerIsRevoked = false
            )
            updateStoreConfig(updated)
            storePreferences.setOnboarded(true)

            if (previousStoreCode.isNotBlank() && previousStoreCode != trimmedCode) {
                FcmNotificationReceiverService.unsubscribeFromStoreTopic(previousStoreCode)
                if (!isComingFromSender) repository.clearAll()
            }
            FcmNotificationReceiverService.subscribeToStoreTopic(trimmedCode)

            // Importar siempre todas las ventas de la nube por defecto (SELECT * FROM yape_transactions WHERE store_code = ...)
            val imported = repository.syncAllStoreTransactions(trimmedCode)
            repository.syncRemoteReceiver(triggerAlerts = false, fullHistorySync = updated.allowWorkerHistory)

            _simulationFeedback.value = if (imported > 0) "Conectado a $storeName ($imported ventas cargadas)" else "Conectado a $storeName ($branchName)"
            onValidationResult?.invoke(true, null)
        }
    }

    fun updateAllowWorkerHistory(enabled: Boolean) {
        storePreferences.updateAllowWorkerHistory(enabled)
        viewModelScope.launch {
            repository.updateStoreAllowWorkerHistory(enabled)
        }
    }

    fun redeemPairingCode(
        code: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = repository.redeemPairingCode(code)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    val storeCode = data?.storeCode
                    val storeName = data?.storeName
                    if (!storeCode.isNullOrBlank()) {
                        // Asegurar registro explícito en store_receivers para visualización inmediata en pantalla del dueño
                        try {
                            repository.registerStoreReceiver(storeCode, code)
                        } catch (e: Exception) {
                            Log.w("MainViewModel", "registerStoreReceiver non-fatal error: ${e.message}")
                        }

                        // Limpiar cualquier estado previo de revocación
                        storePreferences.setWorkerRevoked(false)
                        storePreferences.removeRevokedStoreCode(storeCode)

                        val newStore = SupabaseLinkedStoreDto(
                            storeCode = storeCode,
                            storeName = storeName ?: "Mi Tienda",
                            branchName = "Principal",
                            customName = currentUser.value.displayName.ifBlank { "Trabajador" },
                            status = "ACTIVE"
                        )
                        storePreferences.addOrUpdateLinkedStore(newStore)
                        selectActiveStore(
                            storeCode = storeCode,
                            storeName = storeName ?: "Mi Tienda",
                            branchName = "Principal",
                            skipRevocationCheck = true
                        )
                        loadMyLinkedStores()
                        onResult(true, "¡Vinculado exitosamente con ${storeName ?: storeCode}!")
                    } else {
                        onResult(false, data?.message ?: "Código no encontrado")
                    }
                } else {
                    val rawErr = result.exceptionOrNull()?.message ?: "Código no válido o expirado"
                    val err = if (rawErr.contains("jwt expired", ignoreCase = true) || rawErr.contains("PGRST301", ignoreCase = true)) {
                        "Tu sesión ha expirado. Por favor inicia sesión nuevamente."
                    } else rawErr
                    onResult(false, err)
                }
            } catch (e: Exception) {
                val rawErr = e.message ?: "Error al vincular"
                val err = if (rawErr.contains("jwt expired", ignoreCase = true) || rawErr.contains("PGRST301", ignoreCase = true)) {
                    "Tu sesión ha expirado. Por favor inicia sesión nuevamente."
                } else rawErr
                onResult(false, err)
            }
        }
    }

    fun setOnboarded(onboarded: Boolean = true) {
        storePreferences.setOnboarded(onboarded)
    }

    fun generatePairingCode(
        storeCode: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (storeConfig.value.deviceRole != DeviceRole.SENDER) {
            onResult(false, "Solo el dueño puede generar códigos de vinculación")
            return
        }
        val user = currentUser.value
        viewModelScope.launch {
            var activeStoreCode = storeCode.trim()
            if (user.isLoggedIn && user.id.isNotBlank()) {
                // 1. Consultar RPC de tienda de dueño primero (inmune a RLS de tablas)
                val rpcRes = repository.createOrGetOwnerStore(
                    storeName = storeConfig.value.storeName.ifBlank { "Mi Negocio" },
                    branches = storeConfig.value.storeBranches.ifBlank { "Principal,Sucursal 2" }
                )
                var ownerStore = rpcRes.getOrNull()

                // 2. Si no retornó por RPC, consultar tabla stores
                if (ownerStore == null) {
                    val storeRes = repository.getStoreByOwner(user.id)
                    ownerStore = storeRes.getOrNull()
                }

                // 3. Si aún no existe, registrarla
                if (ownerStore == null) {
                    val regRes = repository.registerNewOwnerStore(
                        storeCode = activeStoreCode,
                        storeName = storeConfig.value.storeName.ifBlank { "Mi Negocio" },
                        ownerId = user.id,
                        branches = storeConfig.value.storeBranches.ifBlank { "Principal,Sucursal 2" }
                    )
                    if (regRes.isSuccess) {
                        ownerStore = repository.getStoreByOwner(user.id).getOrNull()
                    }
                }

                // 4. Si la tienda registrada tiene otro código en Supabase, sincronizar
                if (ownerStore != null && !ownerStore.storeCode.equals(activeStoreCode, ignoreCase = true)) {
                    activeStoreCode = ownerStore.storeCode
                    storePreferences.saveOwnerStoreInfo(ownerStore.storeCode, ownerStore.storeName, ownerStore.branches)
                    val updated = storeConfig.value.copy(
                        storeCode = ownerStore.storeCode,
                        storeName = ownerStore.storeName,
                        storeBranches = ownerStore.branches
                    )
                    updateStoreConfig(updated)
                }
            }
            try {
                val result = repository.generatePairingCode(activeStoreCode)
                if (result.isSuccess) {
                    val code = result.getOrNull()
                    onResult(true, code)
                } else {
                    val rawErr = result.exceptionOrNull()?.message ?: "Error al generar código"
                    val err = if (rawErr.contains("jwt expired", ignoreCase = true) || rawErr.contains("PGRST301", ignoreCase = true)) {
                        "Tu sesión ha expirado. Por favor inicia sesión nuevamente."
                    } else rawErr
                    onResult(false, err)
                }
            } catch (e: Exception) {
                val rawErr = e.message ?: "Error al generar código"
                val err = if (rawErr.contains("jwt expired", ignoreCase = true) || rawErr.contains("PGRST301", ignoreCase = true)) {
                    "Tu sesión ha expirado. Por favor inicia sesión nuevamente."
                } else rawErr
                onResult(false, err)
            }
        }
    }

    // ─── Gestión de Receptores / Trabajadores (Panel de Dueño) ───

    private val _storeReceivers = MutableStateFlow<List<com.example.data.supabase.SupabaseReceiverDto>>(emptyList())
    val storeReceivers: StateFlow<List<com.example.data.supabase.SupabaseReceiverDto>> = _storeReceivers.asStateFlow()

    private val kickedWorkerIds = mutableSetOf<String>()
    private val kickedUserIds = mutableSetOf<String>()

    private val _isLoadingReceivers = MutableStateFlow(false)
    val isLoadingReceivers: StateFlow<Boolean> = _isLoadingReceivers.asStateFlow()

    private var loadReceiversJob: Job? = null

    fun loadStoreReceivers(silent: Boolean = false) {
        val currentStore = storeConfig.value.storeCode
        if (currentStore.isBlank()) return
        if (storeConfig.value.deviceRole != DeviceRole.SENDER) return

        loadReceiversJob?.cancel()
        loadReceiversJob = viewModelScope.launch {
            if (!silent) _isLoadingReceivers.value = true
            try {
                val result = repository.fetchStoreReceivers(currentStore)
                if (result.isSuccess) {
                    val rawList = result.getOrNull() ?: emptyList()
                    val filtered = rawList.filter {
                        it.id !in kickedWorkerIds &&
                        (it.userId == null || it.userId !in kickedUserIds) &&
                        !it.status.equals("REVOKED", ignoreCase = true)
                    }
                    // Si el sondeo silencioso devolvió vacío pero antes teníamos trabajadores activos,
                    // no borrar inmediatamente la lista para evitar falsos negativos por parpadeos de red
                    if (filtered.isNotEmpty() || !silent || _storeReceivers.value.isEmpty()) {
                        _storeReceivers.value = filtered
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading receivers: ${e.message}")
            } finally {
                if (!silent) _isLoadingReceivers.value = false
            }
        }
    }

    fun updateReceiverName(receiverId: String, newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.updateReceiverName(receiverId, newName)
                if (result.isSuccess) {
                    loadStoreReceivers(silent = true)
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Error al actualizar nombre")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun updateReceiverSchedule(
        receiverId: String,
        scheduleEnabled: Boolean,
        startTime: String,
        endTime: String,
        days: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = repository.updateReceiverSchedule(receiverId, scheduleEnabled, startTime, endTime, days)
                if (result.isSuccess) {
                    loadStoreReceivers(silent = true)
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Error al actualizar horario")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun kickReceiver(
        receiverId: String,
        targetUserId: String? = null,
        storeCode: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        kickedWorkerIds.add(receiverId)
        if (!targetUserId.isNullOrBlank()) kickedUserIds.add(targetUserId)

        // Eliminar inmediatamente de la lista local para reflejar instantáneamente en pantalla del dueño
        _storeReceivers.value = _storeReceivers.value.filter {
            it.id !in kickedWorkerIds && (it.userId == null || it.userId !in kickedUserIds)
        }

        viewModelScope.launch {
            try {
                val result = repository.kickReceiver(receiverId, targetUserId, storeCode)
                if (result.isSuccess) {
                    kickedWorkerIds.remove(receiverId)
                    if (!targetUserId.isNullOrBlank()) kickedUserIds.remove(targetUserId)
                }
                loadStoreReceivers(silent = true)
                _storeReceivers.value = _storeReceivers.value.filter {
                    it.id !in kickedWorkerIds && (it.userId == null || it.userId !in kickedUserIds)
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(true, null)
            }
        }
    }

    fun kickReceiver(worker: com.example.data.supabase.SupabaseReceiverDto, onResult: (Boolean, String?) -> Unit) {
        val id = worker.id ?: return
        kickReceiver(id, worker.userId, worker.storeCode, onResult)
    }

    fun exitRevokedWorkerToModeSelection() {
        val revokedCode = storePreferences.getRevokedStoreCode()
        val currentStoreCode = storeConfig.value.storeCode
        val storeToRemove = if (revokedCode.isNotBlank()) revokedCode else currentStoreCode

        if (storeToRemove.isNotBlank()) {
            storePreferences.addRevokedStoreCode(storeToRemove)
            storePreferences.removeLinkedStore(storeToRemove)
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(com.example.data.fcm.FcmServiceAccountConfig.sanitizeTopicName(storeToRemove))
            } catch (_: Exception) {}
        }
        storePreferences.resetReceiverToModeSelection(storeToRemove)
        loadMyLinkedStores()
    }

    fun updateReceiverBranch(receiverId: String, branchName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.updateReceiverBranch(receiverId, branchName)
                if (result.isSuccess) {
                    loadStoreReceivers()
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Error al asignar sucursal")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun claimTransactionBranch(
        transaction: YapeTransaction,
        branchName: String,
        note: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = currentUser.value
                val claimedByName = user.displayName.ifBlank { user.email.ifBlank { "Receptor" } }
                val result = repository.claimTransactionBranch(transaction, branchName, claimedByName, note)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.success == true) {
                        _simulationFeedback.value = "✅ Pago asignado a $branchName"
                        onResult(true, null)
                    } else {
                        val errMsg = resp?.message ?: "Este pago ya fue asignado a otra sucursal"
                        _simulationFeedback.value = "⚠️ $errMsg"
                        onResult(false, errMsg)
                    }
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error al reclamar pago para la sucursal"
                    _simulationFeedback.value = "⚠️ $errMsg"
                    onResult(false, errMsg)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun canCurrentUserUnclaim(transaction: YapeTransaction): Boolean {
        val user = currentUser.value
        return transaction.isClaimedByUser(
            userId = user.id,
            userName = user.displayName,
            userEmail = user.email
        )
    }

    fun unclaimTransactionBranch(
        transaction: YapeTransaction,
        branchName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!canCurrentUserUnclaim(transaction)) {
            val author = transaction.claimedByName.ifBlank { "el trabajador que lo confirmó" }
            val errMsg = "Solo $author puede desmarcar este cobro"
            _simulationFeedback.value = "⚠️ $errMsg"
            onResult(false, errMsg)
            return
        }

        viewModelScope.launch {
            try {
                val result = repository.unclaimTransactionBranch(transaction, branchName)
                if (result.isSuccess) {
                    _simulationFeedback.value = "Pago desmarcado de la sucursal"
                    onResult(true, null)
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error al desmarcar pago"
                    _simulationFeedback.value = "⚠️ $errMsg"
                    onResult(false, errMsg)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun confirmTransaction(
        transaction: YapeTransaction,
        note: String = "",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (!transaction.isStoreTransaction) {
            viewModelScope.launch {
                repository.updateStoreTransactionStatus(transaction.id, isStore = true, reason = "")
            }
        }
        val branch = storeConfig.value.workerBranchName.ifBlank { "Principal" }
        claimTransactionBranch(transaction, branch, note) { success, err ->
            onResult?.invoke(success, err)
        }
    }

    fun unconfirmTransaction(
        transaction: YapeTransaction,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val branch = transaction.branchName.ifBlank { storeConfig.value.workerBranchName.ifBlank { "Principal" } }
        unclaimTransactionBranch(transaction, branch) { success, err ->
            onResult?.invoke(success, err)
        }
    }

    fun updateStoreBranches(branches: String) {
        storePreferences.updateStoreBranches(branches)
        viewModelScope.launch {
            repository.syncStoreBranchesToRemote(branches)
        }
    }

    fun updateWorkerBranch(branchName: String) {
        storePreferences.updateWorkerBranch(branchName)
    }

    fun checkMyReceiverStatus() {
        if (storeConfig.value.deviceRole == DeviceRole.RECEIVER) {
            viewModelScope.launch {
                try {
                    repository.checkMyReceiverStatus()
                } catch (_: Exception) {}
            }
        }
    }

    fun updateTransactionNote(
        transaction: YapeTransaction,
        newNote: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (transaction.isConfirmed && !canCurrentUserUnclaim(transaction)) {
            val author = transaction.claimedByName.ifBlank { "el trabajador que lo confirmó" }
            val errMsg = "Solo $author puede modificar el detalle de este cobro"
            _simulationFeedback.value = "⚠️ $errMsg"
            onResult?.invoke(false, errMsg)
            return
        }

        viewModelScope.launch {
            try {
                val result = repository.updateTransactionNote(transaction, newNote)
                if (result.isSuccess) {
                    _simulationFeedback.value = "Detalle del producto actualizado"
                    onResult?.invoke(true, null)
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error al actualizar detalle"
                    _simulationFeedback.value = "⚠️ $errMsg"
                    onResult?.invoke(false, errMsg)
                }
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }
}


