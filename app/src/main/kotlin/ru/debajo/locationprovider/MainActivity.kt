package ru.debajo.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.debajo.locationprovider.ui.theme.LocationProviderTheme
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

internal inline fun <T> runCatchingAsync(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

class MainActivity : ComponentActivity() {

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LocationProviderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state = rememberMultiplePermissionsState(
                        listOf(
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_CONNECT,
                        )
                    )
                    LaunchedEffect(state) {
                        if (state.permissions.any { !it.status.isGranted }) {
                            state.launchMultiplePermissionRequest()
                        }
                    }

                    val hasAllPermissions = state.permissions.all { it.status.isGranted }
                    if (hasAllPermissions) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            var serverStarted by remember { mutableStateOf(false) }
                            var clientStarted by remember { mutableStateOf(false) }
                            Column {
                                Button(
                                    enabled = !serverStarted && !clientStarted,
                                    onClick = {
                                        startServer()
                                        serverStarted = true
                                    }) {
                                    Text("Server ${if (serverStarted) "on" else "off"}")
                                }
                                Button(
                                    enabled = !serverStarted && !clientStarted,
                                    onClick = {
                                        startClient()
                                        clientStarted = true
                                    }) {
                                    Text("Client ${if (clientStarted) "on" else "off"}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", BT_UUID)
            while (true) {
                val socket = serverSocket.accept()
                socket.inputStream.bufferedReader().lineSequence().forEach { line ->
                    Log.d("yopta", "received $line")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startClient() {
        lifecycleScope.launch(Dispatchers.IO) {
            val device = bluetoothAdapter.bondedDevices.firstOrNull { it.name.contains("Pixel Tablet") }
            if (device != null) {
                while (true) {
                    val socket = runCatching {
                        val socket = device.createRfcommSocketToServiceRecord(BT_UUID)
                        socket.connect()
                        socket
                    }.getOrNull()
                    if (socket == null) {
                        Log.d("yopta", "connect failure")
                        delay(1000)
                    } else {
                        Log.d("yopta", "connected")
                        val writer = socket.outputStream.bufferedWriter()
                        while (true) {
                            writer.write("puk\n")
                            writer.flush()
                            delay(1000)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val BT_UUID: UUID = UUID.fromString("cb9121b3-243c-46a9-b114-d1c72c51578a")
    }
}

private fun LocationManager.mockGpsProvider() {
    addTestProvider(
        LocationManager.GPS_PROVIDER,
        true,
        true,
        false,
        false,
        true,
        true,
        true,
        ProviderProperties.POWER_USAGE_HIGH,
        ProviderProperties.ACCURACY_FINE,
    )
    setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
}

private fun LocationManager.removeMockGpsProvider() {
    removeTestProvider(LocationManager.GPS_PROVIDER)
    setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
}

private fun LocationManager.mockNetworkProvider() {
    addTestProvider(
        LocationManager.NETWORK_PROVIDER,
        false,
        true,
        false,
        false,
        true,
        false,
        false,
        ProviderProperties.POWER_USAGE_LOW,
        ProviderProperties.ACCURACY_COARSE,
    )
    setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
}

internal class MockLocationForegroundService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private val locationManager: LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun addChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val systemChannel = NotificationChannel(
            "12321321",
            "31321321",
            importance
        ).apply {
            this.description = "fsdafdsafasd"
        }
        notificationManager.createNotificationChannel(systemChannel)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
        locationManager.mockGpsProvider()

        job?.cancel()
        job = launch {
            while (true) {
                val mockLocation = Location(LocationManager.GPS_PROVIDER).apply {
                    this.latitude = 56.833332
                    this.longitude = 60.583332
                    this.altitude = 10.0
                    this.accuracy = 2f
                    this.time = System.currentTimeMillis()
                    this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }

                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                delay(300)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        locationManager.removeMockGpsProvider()
    }

    private fun startForeground() {
        addChannel()
        val notification = NotificationCompat.Builder(this, "12321321")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location")
            .build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, MockLocationForegroundService::class.java))
        }
    }
}
