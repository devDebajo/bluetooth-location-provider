package ru.debajo.locationprovider.location

import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class MockLocationManager(
    private val locationManager: LocationManager,
    private val coroutineScope: CoroutineScope,
) {
    private var job: Job? = null
    private val lastLocation: MutableStateFlow<Location?> = MutableStateFlow(null)

    fun mockLocation(location: RemoteLocation) {
        lastLocation.value = location.toLocation(LocationManager.GPS_PROVIDER)
    }

    fun start() {
        locationManager.mockGpsProvider()

        job?.cancel()
        job = coroutineScope.launch {
            while (true) {
                val mockLocation = lastLocation.filterNotNull().first()
                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                delay(300)
            }
        }
    }

    fun stop() {
        job?.cancel()
        locationManager.removeMockGpsProvider()
    }

    private fun LocationManager.mockGpsProvider() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
        } else {
            addTestProvider(
                LocationManager.GPS_PROVIDER,
                true,
                true,
                false,
                false,
                true,
                true,
                true,
                0,
                5,
            )
        }
        setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    private fun LocationManager.removeMockGpsProvider() {
        setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
        removeTestProvider(LocationManager.GPS_PROVIDER)
    }
}
