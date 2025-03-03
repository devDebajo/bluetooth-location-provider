package ru.debajo.locationprovider.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import ru.debajo.locationprovider.R

private const val ChannelId: String = "location_service"

internal fun NotificationManager.addNotificationChannel() {
    val importance = NotificationManager.IMPORTANCE_HIGH
    val systemChannel = NotificationChannel(
        ChannelId,
        "location_service",
        importance
    ).apply {
        this.description = "location_service"
    }
    createNotificationChannel(systemChannel)
}

// TODO action stop
internal fun createServiceNotification(context: Context): Notification {
    return NotificationCompat.Builder(context, ChannelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Location")
        .build()
}
