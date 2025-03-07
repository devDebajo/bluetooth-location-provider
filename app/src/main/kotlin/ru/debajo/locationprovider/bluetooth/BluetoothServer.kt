package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ru.debajo.locationprovider.utils.runCatchingAsync
import java.util.UUID

internal class BluetoothServer(
    private val bluetoothManager: BluetoothManager,
) {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun observeMessages(
        connectTimeoutMs: Long,
        address: UUID = bluetoothServerUuid,
    ): Flow<Message> {
        return callbackFlow {
            val serverSocket = runCatchingAsync {
                bluetoothManager.adapter.listenUsingRfcommWithServiceRecord(bluetoothServerName, address)
            }.getOrNull()

            val job = launch {
                serverSocket?.listen(connectTimeoutMs)?.collect { trySend(it) }
            }

            awaitClose {
                job.cancel()
                runCatchingAsync { serverSocket?.close() }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun BluetoothServerSocket.listen(connectTimeoutMs: Long): Flow<Message> {
        return callbackFlow {
            val job = launch {
                while (currentCoroutineContext().isActive) {
                    val socket = withTimeoutOrNull(connectTimeoutMs) {
                        awaitConnection()
                    }

                    if (socket == null) {
                        close()
                        break
                    }

                    trySend(Message.Connected)
                    socket.listen().collect { trySend(Message.TextMessage(it)) }
                    trySend(Message.Disconnected)
                }
            }

            awaitClose { job.cancel() }
        }
    }

    private fun BluetoothSocket.listen(): Flow<String> {
        val socket = this
        return callbackFlow {
            val job = launch {
                runCatchingAsync {
                    socket.use {
                        for (message in it.inputStream.bufferedReader().lineSequence()) {
                            trySend(message)
                        }
                    }
                }
                close()
            }

            awaitClose {
                job.cancel()
                runCatchingAsync { socket.close() }
            }
        }
    }

    private suspend fun BluetoothServerSocket.awaitConnection(): BluetoothSocket {
        while (true) {
            val socket = runCatchingAsync { awaitSocket() }.getOrNull()
            if (socket != null) {
                return socket
            }
            delay(3000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun BluetoothServerSocket.awaitSocket(): BluetoothSocket {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { close() }
            val socket = accept(7_000)
            continuation.resume(socket) { close() }
        }
    }

    sealed interface Message {
        data object Connected : Message
        class TextMessage(val text: String) : Message
        data object Disconnected : Message
    }

    companion object {
        val bluetoothServerName: String = "BluetoothServer"
        val bluetoothServerUuid: UUID = UUID.fromString("cb9121b3-243c-46a9-b114-d1c72c51578a")
    }
}
