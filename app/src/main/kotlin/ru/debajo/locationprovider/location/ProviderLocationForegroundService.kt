package ru.debajo.locationprovider.location

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import ru.debajo.locationprovider.AppServiceState
import ru.debajo.locationprovider.bluetooth.BluetoothClient
import ru.debajo.locationprovider.bluetooth.BluetoothConnection
import ru.debajo.locationprovider.utils.Di
import ru.debajo.locationprovider.utils.Preferences
import ru.debajo.locationprovider.utils.addNotificationChannel
import ru.debajo.locationprovider.utils.createServiceNotification

internal class ProviderLocationForegroundService : Service(), CoroutineScope by CoroutineScope(Main) {

    private val appServiceState: AppServiceState by lazy { Di.appServiceState }
    private val notificationManager: NotificationManager by lazy { Di.notificationManager }
    private val locationManager: LocationManager by lazy { Di.locationManager }
    private val bluetoothClient: BluetoothClient by lazy { Di.bluetoothClient }
    private val preferences: Preferences by lazy { Di.preferences }
    private var job: Job? = null
    private var lastConnection: BluetoothConnection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        notificationManager.addNotificationChannel()
        startForeground(NOTIFICATION_ID, createServiceNotification(this))

        job?.cancel()
        job = launch {
            runListening()
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runListening() {
        val receiverAddress = preferences.selectedReceiver.state.value ?: return
        lastConnection?.close()
        val connection = bluetoothClient.connect(receiverAddress) ?: return
        lastConnection = connection

        appServiceState.isProviderServiceRunning.value = true
        listenLocation()
            .map { connection.write(it.toRemote(), RemoteLocation.serializer()) }
            .takeWhile { it }
            .collect()
    }

    @SuppressLint("MissingPermission")
    private fun listenLocation(): Flow<Location> {
        return callbackFlow {
            val locationListener = LocationListener {
                trySend(it)
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
            awaitClose { locationManager.removeUpdates(locationListener) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appServiceState.isProviderServiceRunning.value = false
        launch {
            lastConnection?.close()
        }.invokeOnCompletion {
            cancel()
        }
    }

    companion object {
        private const val NOTIFICATION_ID: Int = 543675336

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ProviderLocationForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProviderLocationForegroundService::class.java))
        }
    }
}
