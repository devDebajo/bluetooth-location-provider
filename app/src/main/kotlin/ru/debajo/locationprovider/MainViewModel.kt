package ru.debajo.locationprovider

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoint
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoints
import ru.debajo.locationprovider.location.ProviderLocationForegroundService
import ru.debajo.locationprovider.location.ReceiverLocationForegroundService
import ru.debajo.locationprovider.utils.Di
import ru.debajo.locationprovider.utils.Preferences

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
internal class MainViewModel : ViewModel() {

    private val permissionsEvents: MutableSharedFlow<PermissionChangeEvent> = MutableSharedFlow()

    private val appServiceState: AppServiceState by lazy { Di.appServiceState }
    private val bluetoothEndpoints: BluetoothEndpoints by lazy { Di.bluetoothEndpoints }
    private val preferences: Preferences by lazy { Di.preferences }

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState.Receiver())
    private val _news: MutableSharedFlow<MainNews> = MutableSharedFlow()
    val state: StateFlow<MainState> = _state.asStateFlow()
    val news: SharedFlow<MainNews> = _news.asSharedFlow()

    init {
        viewModelScope.launch {
            state.flatMapLatest {
                when (it) {
                    is MainState.Provider -> appServiceState.providerState
                    is MainState.Receiver -> appServiceState.receiverState
                }
            }.collect { serviceState ->
                updateState {
                    when (this) {
                        is MainState.Provider -> copy(serviceState = serviceState)
                        is MainState.Receiver -> copy(serviceState = serviceState)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)
            if (preferences.isProvider.get()) {
                onProviderSelected()
            } else {
                onReceiverSelected()
            }
        }
    }

    fun onBluetoothPermissionsChanged(granted: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            permissionsEvents.emit(PermissionChangeEvent.BluetoothPermissionsChanged(granted))
        }
    }

    fun onNotificationsPermissionChanged(granted: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            permissionsEvents.emit(PermissionChangeEvent.NotificationsPermissionChanged(granted))
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            permissionsEvents.emit(PermissionChangeEvent.LocationPermissionChanged(granted))
        }
    }

    fun onBackgroundLocationPermissionChanged(granted: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            permissionsEvents.emit(PermissionChangeEvent.BackgroundLocationPermissionChanged(granted))
        }
    }

    fun refreshMockPermission() {
        viewModelScope.launch(Dispatchers.Default) {
            permissionsEvents.emit(PermissionChangeEvent.MockLocationPermissionChanged(PermissionUtils.hasMockLocationPermission()))
        }
    }

    fun onReceiverSelected() {
        viewModelScope.launch(Dispatchers.Default) {
            updateProviderState { MainState.Receiver() }
            preferences.isProvider.put(false)
        }
    }

    fun onProviderSelected() {
        if (state.value is MainState.Provider) {
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            updateState {
                MainState.Provider(selectedEndpointAddress = preferences.selectedReceiver.get())
            }
            preferences.isProvider.put(true)
            if (PermissionUtils.hasBluetoothPermissions()) {
                val availableEndpoints = bluetoothEndpoints.findAvailable()
                updateProviderState { copy(availableEndpoints = availableEndpoints) }
            } else {
                emitNews(MainNews.RequestBluetoothPermission)
                if (awaitPermission<PermissionChangeEvent.BluetoothPermissionsChanged>()) {
                    val availableEndpoints = bluetoothEndpoints.findAvailable()
                    updateProviderState { copy(availableEndpoints = availableEndpoints) }
                } else {
                    emitNews(MainNews.ShowSnackBar("Нет доступа к Bluetooth"))
                }
            }
        }
    }

    fun onEndpointSelected(endpoint: BluetoothEndpoint) {
        updateProviderState {
            copy(selectedEndpointAddress = endpoint.address)
        }
        preferences.selectedReceiver.put(endpoint.address)
    }

    fun hideMockLocationDialog() {
        updateReceiverState {
            copy(showMockPermissionDialog = false)
        }
    }

    fun start() {
        viewModelScope.launch(Dispatchers.Default) {
            startInternal()
        }
    }

    private suspend fun startInternal() {
        if (!PermissionUtils.hasNotificationsPermission()) {
            emitNews(MainNews.RequestNotificationsPermission)
            if (!awaitPermission<PermissionChangeEvent.NotificationsPermissionChanged>()) {
                emitNews(MainNews.ShowSnackBar("Нет доступа к уведомлениям"))
                return
            }
        }

        if (!PermissionUtils.hasBluetoothPermissions()) {
            emitNews(MainNews.RequestBluetoothPermission)
            if (!awaitPermission<PermissionChangeEvent.BluetoothPermissionsChanged>()) {
                emitNews(MainNews.ShowSnackBar("Нет доступа к Bluetooth"))
                return
            }
        }

        if (!PermissionUtils.hasLocationPermissions()) {
            emitNews(MainNews.RequestLocationPermission)
            if (!awaitPermission<PermissionChangeEvent.LocationPermissionChanged>()) {
                emitNews(MainNews.ShowSnackBar("Нет доступа к геолокации"))
                return
            }
        }

        when (state.value) {
            is MainState.Provider -> startProvider()
            is MainState.Receiver -> startReceiver()
        }
    }

    private suspend fun startProvider() {
        if (!PermissionUtils.hasBackgroundLocationPermission()) {
            emitNews(MainNews.RequestBackgroundLocationPermission)
            if (!awaitPermission<PermissionChangeEvent.BackgroundLocationPermissionChanged>()) {
                emitNews(MainNews.ShowSnackBar("Нет фонового доступа к геолокации"))
                return
            }
        }

        withContext(Dispatchers.Main) {
            ProviderLocationForegroundService.start(Di.context)
        }
    }

    private suspend fun startReceiver() {
        if (!PermissionUtils.hasMockLocationPermission()) {
            updateReceiverState {
                copy(showMockPermissionDialog = true)
            }
            if (!awaitPermission<PermissionChangeEvent.MockLocationPermissionChanged>()) {
                emitNews(MainNews.ShowSnackBar("Нет доступа к фиктивной геолокации"))
                return
            }
        }
        withContext(Dispatchers.Main) {
            ReceiverLocationForegroundService.start(Di.context)
        }
    }

    fun stop() {
        when (state.value) {
            is MainState.Provider -> ProviderLocationForegroundService.stop(Di.context)
            is MainState.Receiver -> ReceiverLocationForegroundService.stop(Di.context)
        }
    }

    private suspend inline fun <reified E : PermissionChangeEvent> awaitPermission(): Boolean {
        return permissionsEvents.filterIsInstance<E>().first().granted
    }

    private suspend fun emitNews(news: MainNews) {
        _news.emit(news)
    }

    private inline fun updateReceiverState(block: MainState.Receiver.() -> MainState): MainState {
        return updateState {
            if (this is MainState.Receiver) {
                block()
            } else {
                this
            }
        }
    }

    private inline fun updateProviderState(block: MainState.Provider.() -> MainState): MainState {
        return updateState {
            if (this is MainState.Provider) {
                block()
            } else {
                this
            }
        }
    }

    private inline fun updateState(block: MainState.() -> MainState): MainState {
        while (true) {
            val prevValue = _state.value
            val nextValue = block(prevValue)
            if (_state.compareAndSet(prevValue, nextValue)) {
                return nextValue
            }
        }
    }
}
