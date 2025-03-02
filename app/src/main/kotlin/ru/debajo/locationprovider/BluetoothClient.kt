package ru.debajo.locationprovider

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.UUID

internal class BluetoothClient(
    private val bluetoothManager: BluetoothManager,
) {
    private val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }

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
        val device = bluetoothAdapter.bondedDevices.firstOrNull { it.address == endpoint.bluetoothDevice.address } ?: return null
        yield()
        val socket = runCatching {
            val socket = device.createRfcommSocketToServiceRecord(serverAddress)
            yield()
            socket.connect()
            socket
        }.getOrNull() ?: return null
        return BluetoothConnection(endpoint, socket)
    }
}
