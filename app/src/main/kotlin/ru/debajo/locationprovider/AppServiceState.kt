package ru.debajo.locationprovider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal class AppServiceState {
    val isProviderServiceRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isReceiverServiceRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isRunning: Boolean
        get() = isProviderServiceRunning.value || isReceiverServiceRunning.value

    fun observeServiceRunning(): Flow<Boolean> {
        return combine(
            isProviderServiceRunning,
            isReceiverServiceRunning,
        ) { a, b -> a || b }
    }
}
