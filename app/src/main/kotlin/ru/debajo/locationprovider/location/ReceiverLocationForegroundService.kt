package ru.debajo.locationprovider.location

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import ru.debajo.locationprovider.AppServiceState
import ru.debajo.locationprovider.bluetooth.BluetoothServer
import ru.debajo.locationprovider.utils.Di
import ru.debajo.locationprovider.utils.addNotificationChannel
import ru.debajo.locationprovider.utils.createServiceNotification
import ru.debajo.locationprovider.utils.runCatchingAsync

internal class ReceiverLocationForegroundService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val appServiceState: AppServiceState by lazy { Di.appServiceState }
    private val notificationManager: NotificationManager by lazy { Di.notificationManager }
    private val bluetoothServer: BluetoothServer by lazy { Di.bluetoothServer }
    private val mockLocationManager: MockLocationManager by lazy { Di.mockLocationManager }
    private val json: Json by lazy { Di.json }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        notificationManager.addNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createServiceNotification(
                context = this,
                isProvider = false,
            )
        )

        appServiceState.isReceiverServiceRunning.value = true
        mockLocationManager.start()

        launch {
            var lastUpdate: Instant? = null
            bluetoothServer.observeMessages()
                .collect { message ->
                    when (message) {
                        is BluetoothServer.Message.Connected -> {
                            updateNotification(
                                isConnected = true,
                                lastUpdate = lastUpdate,
                            )
                        }

                        is BluetoothServer.Message.Disconnected -> {
                            updateNotification(
                                isConnected = false,
                                lastUpdate = lastUpdate,
                            )
                        }

                        is BluetoothServer.Message.TextMessage -> {
                            val location = runCatchingAsync { json.decodeFromString(RemoteLocation.serializer(), message.text) }.getOrNull()
                            if (location != null) {
                                lastUpdate = Clock.System.now()
                                mockLocationManager.mockLocation(location)
                            }

                            updateNotification(
                                isConnected = true,
                                lastUpdate = lastUpdate,
                            )
                        }
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        appServiceState.isReceiverServiceRunning.value = false
        cancel()
        mockLocationManager.stop()
    }

    private fun updateNotification(
        isConnected: Boolean = false,
        lastUpdate: Instant? = null,
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            createServiceNotification(
                context = this,
                isProvider = false,
                isConnected = isConnected,
                lastUpdate = lastUpdate,
            )
        )
    }

    companion object {
        private const val NOTIFICATION_ID: Int = 543675337

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ReceiverLocationForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ReceiverLocationForegroundService::class.java))
        }
    }
}
