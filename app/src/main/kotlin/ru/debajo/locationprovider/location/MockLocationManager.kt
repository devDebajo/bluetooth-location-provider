package ru.debajo.locationprovider.location

import android.location.LocationManager
import android.location.provider.ProviderProperties
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
    private val lastLocation: MutableStateFlow<RemoteLocation?> = MutableStateFlow(null)

    fun mockLocation(location: RemoteLocation) {
        lastLocation.value = location
    }

    fun start() {
        locationManager.mockGpsProvider()

        job?.cancel()
        job = coroutineScope.launch {
            while (true) {
                val mockLocation = lastLocation.filterNotNull().first().toLocation(LocationManager.GPS_PROVIDER)
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
        setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
        removeTestProvider(LocationManager.GPS_PROVIDER)
    }
}
