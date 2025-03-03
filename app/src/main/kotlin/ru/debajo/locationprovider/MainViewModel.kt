package ru.debajo.locationprovider

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MainViewModel : ViewModel() {

    private val bluetoothEndpoints: BluetoothEndpoints by lazy { Di.bluetoothEndpoints }
    private val preferences: Preferences by lazy { Di.preferences }

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isProvider.state.collect { isProvider ->
                updateState {
                    copy(isProvider = isProvider)
                }
            }
        }

        viewModelScope.launch {
            preferences.selectedReceiver.state.collect { selectedReceiver ->
                updateState {
                    copy(selectedEndpointAddress = selectedReceiver)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onPermissionsGranted() {
        viewModelScope.launch {
            val availableEndpoints = bluetoothEndpoints.findAvailable()
            updateState {
                copy(availableEndpoints = availableEndpoints)
            }
        }
    }

    fun onProviderSelected() {
        viewModelScope.launch(Dispatchers.Default) {
            preferences.isProvider.put(true)
        }
    }

    fun onReceiverSelected() {
        viewModelScope.launch(Dispatchers.Default) {
            preferences.isProvider.put(false)
        }
    }

    fun onEndpointSelected(endpoint: BluetoothEndpoint) {
        viewModelScope.launch(Dispatchers.Default) {
            preferences.selectedReceiver.put(endpoint.address)
        }
    }

    fun start() {

    }

    fun stop() {

    }

    private inline fun updateState(block: MainState.() -> MainState) {
        _state.update(block)
    }
}
