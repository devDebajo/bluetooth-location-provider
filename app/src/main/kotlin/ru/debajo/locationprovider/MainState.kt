package ru.debajo.locationprovider

import androidx.compose.runtime.Immutable
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoint

@Immutable
internal data class MainState(
    val isProvider: Boolean = false,
    val availableEndpoints: List<BluetoothEndpoint> = emptyList(),
    val selectedEndpointAddress: String? = null,
    val isRunning: Boolean = false,
) {
    val canStart: Boolean = !isRunning && availableEndpoints.any { it.address == selectedEndpointAddress }
    val canStop: Boolean = isRunning
}
