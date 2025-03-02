package ru.debajo.locationprovider

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter

internal class BluetoothConnection(
    private val endpoint: BluetoothEndpoint,
    private val socket: BluetoothSocket,
) {
    private val writer: BufferedWriter = socket.outputStream.bufferedWriter()
    private val _isConnected: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    suspend fun write(message: String): Boolean {
        if (!_isConnected.value) {
            return false
        }

        return runCatchingAsync {
            withContext(Dispatchers.IO) {
                writer.write(message)
                writer.newLine()
                writer.flush()
            }
        }
            .map { true }
            .onFailure { close() }
            .getOrElse { false }
    }

    suspend fun close() {
        withContext(Dispatchers.IO) {
            runCatchingAsync { writer.close() }
            runCatchingAsync { socket.close() }
            _isConnected.value = false
        }
    }
}
