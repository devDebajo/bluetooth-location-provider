package ru.debajo.locationprovider

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Context.LOCATION_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.SharedPreferences
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

internal object Di : CoroutineScope by CoroutineScope(Dispatchers.Main) {
    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val context: Context
        get() = App.INSTANCE

    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("location_provider_prefs", Context.MODE_PRIVATE)
    }

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
        get() = BluetoothClient(bluetoothManager, json)

    val bluetoothServer: BluetoothServer
        get() = BluetoothServer(bluetoothManager, coroutineScope)

    val bluetoothEndpoints: BluetoothEndpoints
        get() = BluetoothEndpoints(bluetoothManager)
}
