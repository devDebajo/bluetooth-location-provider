package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.debajo.locationprovider.utils.runCatchingAsync

internal class BluetoothEndpoints(
    private val bluetoothManager: BluetoothManager,
) {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun findAvailable(): List<BluetoothEndpoint> {
        return withContext(Dispatchers.IO) {
            runCatchingAsync { findAvailableUnsafe() }.getOrElse { emptyList() }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun findAvailableUnsafe(): List<BluetoothEndpoint> {
        return bluetoothManager.adapter.bondedDevices.mapNotNull {
            val address = it.address
            if (address.isNullOrEmpty()) {
                null
            } else {
                BluetoothEndpoint(
                    address = address,
                    name = it.name ?: address,
                )
            }
        }
    }
}
