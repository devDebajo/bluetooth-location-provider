package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
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
        block: suspend BluetoothWriter.() -> Unit
    ) {
        runCatchingAsync {
            connectAndRunUnsafe(
                serverAddress = serverAddress,
                endpointAddress = endpointAddress,
                block = { socket -> BluetoothWriter(socket, json).block() },
            )
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun connectAndRunUnsafe(
        serverAddress: UUID,
        endpointAddress: String,
        block: suspend (BluetoothSocket) -> Unit,
    ) {
        val device = bluetoothManager.adapter.bondedDevices.firstOrNull { it.address == endpointAddress } ?: return
        yield()

        val socket = device.createRfcommSocketToServiceRecord(serverAddress)
        socket.connect()
        socket.use { block(it) }
    }
}
