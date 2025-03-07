package ru.debajo.locationprovider

import kotlinx.datetime.Instant

internal data class ServiceState(
    val isRunning: Boolean = false,
    val isConnected: Boolean = false,
    val lastUpdate: Instant? = null,
)
