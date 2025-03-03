package ru.debajo.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow

internal class AppServiceState {
    val isProviderServiceRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isReceiverServiceRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
