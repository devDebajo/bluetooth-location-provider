package ru.debajo.locationprovider.utils

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Context.LOCATION_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.SharedPreferences
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import ru.debajo.locationprovider.App
import ru.debajo.locationprovider.AppServiceState
import ru.debajo.locationprovider.bluetooth.BluetoothClient
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoints
import ru.debajo.locationprovider.bluetooth.BluetoothServer
import ru.debajo.locationprovider.location.MockLocationManager

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

    val locationManager: LocationManager by lazy {
        context.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val mockLocationManager: MockLocationManager
        get() = MockLocationManager(locationManager, coroutineScope)

    val notificationManager: NotificationManager by lazy {
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val coroutineScope: CoroutineScope = this

    val bluetoothClient: BluetoothClient
        get() = BluetoothClient(bluetoothManager, json)

    val bluetoothServer: BluetoothServer
        get() = BluetoothServer(bluetoothManager)

    val bluetoothEndpoints: BluetoothEndpoints
        get() = BluetoothEndpoints(bluetoothManager)

    val appServiceState: AppServiceState by lazy { AppServiceState() }
}
