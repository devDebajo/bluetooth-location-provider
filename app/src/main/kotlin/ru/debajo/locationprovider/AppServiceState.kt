package ru.debajo.locationprovider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal class AppServiceState {
    private val _providerState: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState())
    private val _receiverState: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState())

    val providerState: StateFlow<ServiceState> = _providerState.asStateFlow()
    val receiverState: StateFlow<ServiceState> = _receiverState.asStateFlow()

    fun updateProviderState(block: ServiceState.() -> ServiceState) {
        _providerState.update(block)
    }

    fun updateReceiverState(block: ServiceState.() -> ServiceState) {
        _receiverState.update(block)
    }

    val isRunning: Boolean
        get() = providerState.value.isRunning || receiverState.value.isRunning

    fun observeServiceRunning(): Flow<Boolean> {
        return combine(
            providerState,
            receiverState,
        ) { a, b -> a.isRunning || b.isRunning }
    }
}
