package ru.debajo.locationprovider

import androidx.compose.runtime.Immutable

@Immutable
internal data class MainState(
    val isProvider: Boolean = false,
    val availableEndpoints: List<BluetoothEndpoint> = emptyList(),
    val selectedEndpoint: BluetoothEndpoint? = null,
    val isRunning: Boolean = false,
)
