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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

internal class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
                    val state = rememberMultiplePermissionsState(PermissionsList) { map ->
                        if (map.all { it.value }) {
                            viewModel.onPermissionsGranted()
                        }
                    }
                    LaunchedEffect(state) {
                        if (state.permissions.any { !it.status.isGranted }) {
                            state.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onPermissionsGranted()
                        }
                    }

                    val hasAllPermissions = state.permissions.all { it.status.isGranted }
                    if (hasAllPermissions) {
                        PermittedContent(innerPadding)
                    }
                }
            }
        }
    }

    @Composable
    private fun PermittedContent(paddings: PaddingValues) {
        val state by viewModel.state.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(Modifier.size(10.dp))
                RadioButtons(state)
                if (state.isProvider) {
                    Spacer(Modifier.size(20.dp))
                    AvailableEndpoints(state)
                }
            }
        }
    }

    @Composable
    private fun RadioButtons(
        state: MainState,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Режим",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(8.dp))
            TextRadioButton(
                text = "Приемник геолокации",
                selected = !state.isProvider,
                onClick = { viewModel.onReceiverSelected() },
                modifier = Modifier.fillMaxWidth(),
            )
            TextRadioButton(
                text = "Передатчик геолокации",
                selected = state.isProvider,
                onClick = { viewModel.onProviderSelected() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun AvailableEndpoints(
        state: MainState,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Выберите устройство-приемник",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(count = state.availableEndpoints.size) { index ->
                    val endpoint = state.availableEndpoints[index]
                    TextRadioButton(
                        text = endpoint.name,
                        selected = endpoint.address == state.selectedEndpoint?.address,
                        onClick = { viewModel.onEndpointSelected(endpoint) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    @Composable
    private fun TextRadioButton(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
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
        val PermissionsList: List<String> = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
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
