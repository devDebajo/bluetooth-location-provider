package ru.debajo.locationprovider.utils

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
import ru.debajo.locationprovider.App
import ru.debajo.locationprovider.bluetooth.BluetoothClient
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoints
import ru.debajo.locationprovider.bluetooth.BluetoothServer

internal object Di : CoroutineScope by CoroutineScope(Dispatchers.Main) {
    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val context: Context
        get() = App.INSTANCE

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("location_provider_prefs", Context.MODE_PRIVATE)
    }

    val preferences: Preferences by lazy { Preferences(sharedPreferences) }

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val locationManager: LocationManager by lazy {
        context.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val coroutineScope: CoroutineScope = this

    val bluetoothClient: BluetoothClient
        get() = BluetoothClient(bluetoothManager, json)

    val bluetoothServer: BluetoothServer
        get() = BluetoothServer(bluetoothManager, coroutineScope)

    val bluetoothEndpoints: BluetoothEndpoints
        get() = BluetoothEndpoints(bluetoothManager)
}
