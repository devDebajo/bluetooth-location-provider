package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import ru.debajo.locationprovider.utils.runCatchingAsync
import java.util.UUID

internal class BluetoothClient(
    private val bluetoothManager: BluetoothManager,
    private val json: Json,
) {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun connect(
        endpointAddress: String,
        serverAddress: UUID = BluetoothServer.bluetoothServerUuid,
        connectTimeoutMs: Long,
        block: suspend BluetoothWriter.() -> Unit
    ) {
        runCatchingAsync {
            connectAndRunUnsafe(
                serverAddress = serverAddress,
                endpointAddress = endpointAddress,
                connectTimeoutMs = connectTimeoutMs,
                block = { socket -> BluetoothWriter(socket, json).block() },
            )
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun connectAndRunUnsafe(
        serverAddress: UUID,
        endpointAddress: String,
        connectTimeoutMs: Long,
        block: suspend (BluetoothSocket) -> Unit,
    ) {
        val device = bluetoothManager.adapter.bondedDevices.firstOrNull { it.address == endpointAddress } ?: return
        yield()

        val socket = withTimeoutOrNull(connectTimeoutMs) {
            device.awaitConnection(serverAddress)
        }

        socket?.use { block(it) }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun BluetoothDevice.awaitConnection(serverAddress: UUID): BluetoothSocket {
        while (true) {
            val socket = runCatchingAsync {
                createRfcommSocketToServiceRecord(serverAddress).also { it.connect() }
            }.getOrNull()
            if (socket != null) {
                return socket
            }
            delay(3000)
        }
    }
}
