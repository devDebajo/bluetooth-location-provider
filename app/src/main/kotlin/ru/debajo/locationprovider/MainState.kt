package ru.debajo.locationprovider

import androidx.compose.runtime.Immutable
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoint

@Immutable
internal sealed interface MainState {
    val serviceState: ServiceState

    val isRunning: Boolean
        get() = serviceState.isRunning

    val canStop: Boolean
        get() = isRunning

    val canStart: Boolean

    data class Receiver(
        override val serviceState: ServiceState = ServiceState(),
        val showMockPermissionDialog: Boolean = false,
    ) : MainState {
        override val canStart: Boolean = !isRunning
    }

    data class Provider(
        override val serviceState: ServiceState = ServiceState(),
        val availableEndpoints: List<BluetoothEndpoint> = emptyList(),
        val selectedEndpointAddress: String? = null,
    ) : MainState {
        override val canStart: Boolean = !isRunning && availableEndpoints.any { it.address == selectedEndpointAddress }
    }
}
