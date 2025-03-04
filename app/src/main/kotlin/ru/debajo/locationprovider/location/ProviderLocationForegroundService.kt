package ru.debajo.locationprovider.location

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
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
    private val fusedLocationClient: FusedLocationProviderClient by lazy { Di.fusedLocationClient }
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
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000
            )
                .setMinUpdateIntervalMillis(5000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { trySend(it) }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            awaitClose { fusedLocationClient.removeLocationUpdates(locationCallback) }
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
