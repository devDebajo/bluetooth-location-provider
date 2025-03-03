package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        endpoint: BluetoothEndpoint,
        serverAddress: UUID = BluetoothServer.bluetoothServerUuid,
    ): BluetoothConnection? {
        return runCatchingAsync {
            withContext(Dispatchers.IO) {
                connectUnsafe(serverAddress, endpoint)
            }
        }.getOrNull()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun connectUnsafe(serverAddress: UUID, endpoint: BluetoothEndpoint): BluetoothConnection? {
        val device = bluetoothManager.adapter.bondedDevices.firstOrNull { it.address == endpoint.address } ?: return null
        yield()
        val socket = runCatchingAsync {
            val socket = device.createRfcommSocketToServiceRecord(serverAddress)
            yield()
            socket.connect()
            socket
        }.getOrNull() ?: return null
        return BluetoothConnection(endpoint, socket, json)
    }
}
