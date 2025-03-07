package ru.debajo.locationprovider.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import ru.debajo.locationprovider.MainActivity
import ru.debajo.locationprovider.R
import java.time.format.DateTimeFormatter

private const val ChannelId: String = "location_service"

internal fun NotificationManager.addNotificationChannel() {
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val systemChannel = NotificationChannel(
        ChannelId,
        "location_service",
        importance
    ).apply {
        this.description = "location_service"
    }
    createNotificationChannel(systemChannel)
}

internal fun createServiceNotification(
    context: Context,
    isProvider: Boolean,
    isConnected: Boolean = false,
    lastUpdate: Instant? = null,
): Notification {
    val text = buildString {
        if (isConnected) {
            append("Подключено")
        } else {
            append("Ожидание подключения")
        }
        if (lastUpdate != null) {
            append("\n")
            append("Последнее обновление: ${lastUpdate.format()}")
        }
    }
    return NotificationCompat.Builder(context, ChannelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(if (isProvider) "Передатчик геолокации" else "Приемник геолокации")
        .setContentText(text)
        .setContentIntent(
            MainActivity.createIntent(context)
                .toPending(context, 0, PendingIntentType.ACTIVITY)
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Стоп",
            AppLocalReceiver.stopServicesPending(context)
        )
        .setDeleteIntent(AppLocalReceiver.notificationDeletedPending(context))
        .build()
}

internal fun Instant.format(): String {
    val localDateTime = toLocalDateTime(TimeZone.currentSystemDefault())
    val formatter = if (Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date == localDateTime.date) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
    return localDateTime.toJavaLocalDateTime().format(formatter)
}

internal enum class PendingIntentType { BROADCAST, ACTIVITY }

internal fun Intent.toPending(context: Context, requestCode: Int, type: PendingIntentType): PendingIntent {
    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    return when (type) {
        PendingIntentType.BROADCAST -> PendingIntent.getBroadcast(context, requestCode, this, flags)
        PendingIntentType.ACTIVITY -> PendingIntent.getActivity(context, requestCode, this, flags)
    }
}
