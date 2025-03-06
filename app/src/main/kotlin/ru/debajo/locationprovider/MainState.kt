package ru.debajo.locationprovider

import androidx.compose.runtime.Immutable
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoint

@Immutable
internal sealed interface MainState {
    val isRunning: Boolean

    val canStop: Boolean
        get() = isRunning

    val canStart: Boolean

    data class Receiver(
        override val isRunning: Boolean = false,
        val showMockPermissionDialog: Boolean = false,
    ) : MainState {
        override val canStart: Boolean = !isRunning
    }

    data class Provider(
        override val isRunning: Boolean = false,
        val availableEndpoints: List<BluetoothEndpoint> = emptyList(),
        val selectedEndpointAddress: String? = null,
    ) : MainState {
        override val canStart: Boolean = !isRunning && availableEndpoints.any { it.address == selectedEndpointAddress }
    }
}
