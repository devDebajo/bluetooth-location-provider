package ru.debajo.locationprovider.location

import android.location.Location
import android.os.SystemClock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteLocation(
    @SerialName("la")
    val latitude: Double,

    @SerialName("lo")
    val longitude: Double,

    @SerialName("al")
    val altitude: Double,

    @SerialName("ac")
    val accuracy: Float,

    @SerialName("s")
    val speed: Float,

    @SerialName("b")
    val bearing: Float,
)

internal fun RemoteLocation.toLocation(provider: String): Location {
    val receiver = this
    return Location(provider).apply {
        latitude = receiver.latitude
        longitude = receiver.longitude
        altitude = receiver.altitude
        accuracy = receiver.accuracy
        speed = receiver.speed
        bearing = receiver.bearing
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }
}

internal fun Location.toRemote(): RemoteLocation {
    val receiver = this
    return RemoteLocation(
        latitude = receiver.latitude,
        longitude = receiver.longitude,
        altitude = receiver.altitude,
        accuracy = receiver.accuracy,
        speed = receiver.speed,
        bearing = receiver.bearing,
    )
}
