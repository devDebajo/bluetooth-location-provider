package ru.debajo.locationprovider

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.debajo.locationprovider.ui.theme.LocationProviderTheme

class MainActivity : ComponentActivity() {
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
                        ), { permissions ->
                            if (permissions.all { it.value }) {
                                MockLocationForegroundService.start(this)
                            }
                        }
                    )
                    LaunchedEffect(state) {
                        if (state.permissions.any { !it.status.isGranted }) {
                            state.launchMultiplePermissionRequest()
                        } else {
                            MockLocationForegroundService.start(this@MainActivity)
                        }
                    }
                }
            }
        }
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
