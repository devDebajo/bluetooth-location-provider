package ru.debajo.locationprovider

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MainViewModel : ViewModel() {

    private val bluetoothEndpoints: BluetoothEndpoints by lazy { Di.bluetoothEndpoints }

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

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
        updateState {
            copy(isProvider = true)
        }
    }

    fun onReceiverSelected() {
        updateState {
            copy(isProvider = false)
        }
    }

    fun onEndpointSelected(endpoint: BluetoothEndpoint) {
        updateState {
            copy(selectedEndpoint = endpoint)
        }
    }

    private inline fun updateState(block: MainState.() -> MainState) {
        _state.update(block)
    }
}
