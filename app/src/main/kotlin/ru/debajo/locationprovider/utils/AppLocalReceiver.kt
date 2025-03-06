package ru.debajo.locationprovider.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import ru.debajo.locationprovider.AppServiceState
import ru.debajo.locationprovider.location.ProviderLocationForegroundService
import ru.debajo.locationprovider.location.ReceiverLocationForegroundService

internal class AppLocalReceiver : BroadcastReceiver() {

    private val appServiceState: AppServiceState by lazy { Di.appServiceState }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            STOP_SERVICES, NOTIFICATION_DELETED -> {
                if (appServiceState.isReceiverServiceRunning.value) {
                    ReceiverLocationForegroundService.stop(context)
                }

                if (appServiceState.isProviderServiceRunning.value) {
                    ProviderLocationForegroundService.stop(context)
                }
            }
        }
    }

    companion object {
        fun intentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(STOP_SERVICES)
                addAction(NOTIFICATION_DELETED)
            }
        }

        fun stopServicesPending(context: Context): PendingIntent {
            return Intent(STOP_SERVICES)
                .setPackage(context.packageName)
                .toPending(context, 0, PendingIntentType.BROADCAST)
        }

        fun notificationDeletedPending(context: Context): PendingIntent {
            return Intent(STOP_SERVICES)
                .setPackage(context.packageName)
                .toPending(context, 0, PendingIntentType.BROADCAST)
        }

        const val STOP_SERVICES: String = "ru.debajo.locationprovider.utils.STOP_SERVICES"
        const val NOTIFICATION_DELETED: String = "ru.debajo.locationprovider.utils.NOTIFICATION_DELETED"
    }
}