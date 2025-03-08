package ru.debajo.locationprovider.location

import android.location.Location
import android.os.Build
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

    @SerialName("ba")
    val bearingAccuracyDegrees: Float,

    @SerialName("va")
    val verticalAccuracyMeters: Float,

    @SerialName("mslaa")
    val mslAltitudeAccuracyMeters: Float?,

    @SerialName("mslam")
    val mslAltitudeMeters: Double?,

    @SerialName("sa")
    val speedAccuracyMetersPerSecond: Float,

    @SerialName("t")
    val time: Long,

    @SerialName("ern")
    val elapsedRealtimeNanos: Long,
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
        bearingAccuracyDegrees = receiver.bearingAccuracyDegrees
        verticalAccuracyMeters = receiver.verticalAccuracyMeters
        mslAltitudeAccuracyMetersCompat = receiver.mslAltitudeAccuracyMeters
        mslAltitudeMetersCompat = receiver.mslAltitudeMeters
        speedAccuracyMetersPerSecond = receiver.speedAccuracyMetersPerSecond
        time = receiver.time
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
        bearingAccuracyDegrees = receiver.bearingAccuracyDegrees,
        verticalAccuracyMeters = receiver.verticalAccuracyMeters,
        mslAltitudeAccuracyMeters = receiver.mslAltitudeAccuracyMetersCompat,
        mslAltitudeMeters = receiver.mslAltitudeMetersCompat,
        speedAccuracyMetersPerSecond = receiver.speedAccuracyMetersPerSecond,
        time = receiver.time,
        elapsedRealtimeNanos = receiver.elapsedRealtimeNanos,
    )
}

private var Location.mslAltitudeAccuracyMetersCompat: Float?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mslAltitudeAccuracyMeters
        } else {
            null
        }
    }
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && value != null) {
            mslAltitudeAccuracyMeters = value
        }
    }

private var Location.mslAltitudeMetersCompat: Double?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mslAltitudeMeters
        } else {
            null
        }
    }
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && value != null) {
            mslAltitudeMeters = value
        }
    }
