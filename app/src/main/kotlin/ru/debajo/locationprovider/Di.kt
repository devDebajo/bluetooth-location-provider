package ru.debajo.locationprovider

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Context.LOCATION_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal object Di : CoroutineScope by CoroutineScope(Dispatchers.Main) {
    val context: Context
        get() = App.INSTANCE

    val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }

    val locationManager: LocationManager by lazy {
        context.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    val notificationManager: NotificationManager by lazy {
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    val coroutineScope: CoroutineScope = this

    val bluetoothClient: BluetoothClient
        get() = BluetoothClient(bluetoothManager)

    val bluetoothServer: BluetoothServer
        get() = BluetoothServer(bluetoothManager, coroutineScope)
}
