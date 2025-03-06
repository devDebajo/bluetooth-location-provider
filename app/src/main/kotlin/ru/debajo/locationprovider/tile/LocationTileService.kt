package ru.debajo.locationprovider.tile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.debajo.locationprovider.AppServiceState
import ru.debajo.locationprovider.MainActivity
import ru.debajo.locationprovider.PermissionUtils
import ru.debajo.locationprovider.bluetooth.BluetoothEndpoints
import ru.debajo.locationprovider.utils.AppLocalReceiver
import ru.debajo.locationprovider.utils.Di
import ru.debajo.locationprovider.utils.PendingIntentType
import ru.debajo.locationprovider.utils.Preferences
import ru.debajo.locationprovider.utils.toPending

internal class LocationTileService : TileService(), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val bluetoothEndpoints: BluetoothEndpoints by lazy { Di.bluetoothEndpoints }
    private val appServiceState: AppServiceState by lazy { Di.appServiceState }
    private val preferences: Preferences by lazy { Di.preferences }
    private var job: Job? = null

    override fun onStartListening() {
        job?.cancel()
        job = launch {
            combine(
                preferences.isProvider.state,
                appServiceState.observeServiceRunning(),
            ) { isProvider, isRunning -> isProvider to isRunning }.collect { (isProvider, isRunning) ->
                syncTileState(
                    isProvider = isProvider,
                    isRunning = isRunning
                )
            }

            launch {
                preferences.isProvider.state.collect { isProvider ->
                    qsTile.label = if (isProvider) "Передатчик" else "Приемник"
                    qsTile.updateTile()
                }
            }

            launch {
                appServiceState.observeServiceRunning().collect { isRunning ->
                    qsTile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    qsTile.subtitle = if (isRunning) "Вкл." else "Откл."
                    qsTile.updateTile()
                }
            }
        }
    }

    override fun onTileAdded() {
        syncTileState()
    }

    override fun onClick() {
        launch {
            when (qsTile.state) {
                Tile.STATE_ACTIVE -> {
                    stop()
                    syncTileState()
                }

                Tile.STATE_INACTIVE -> {
                    if (canStart()) {
                        start()
                        syncTileState()
                    } else {
                        openMainActivity()
                    }
                }
            }
        }
    }

    override fun onStopListening() {
        Log.d("yopta", "onStopListening")
        job?.cancel()
    }

    private suspend fun canStart(): Boolean {
        if (appServiceState.isRunning) {
            return false
        }

        val isProvider = preferences.isProvider.get()
        return if (isProvider) {
            if (!PermissionUtils.hasPermissionsForProvider()) {
                false
            } else {
                val selectedReceiver = preferences.selectedReceiver.get()
                if (selectedReceiver != null) {
                    bluetoothEndpoints.findAvailable().any { it.address == selectedReceiver }
                } else {
                    false
                }
            }
        } else {
            PermissionUtils.hasPermissionsForReceiver()
        }
    }

    private fun syncTileState(
        isProvider: Boolean = preferences.isProvider.get(),
        isRunning: Boolean = appServiceState.isRunning,
    ) {
        qsTile.label = if (isProvider) "Передатчик" else "Приемник"
        qsTile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.subtitle = if (isRunning) "Вкл." else "Откл."
        qsTile.updateTile()
    }

    private fun start() {
        sendBroadcast(AppLocalReceiver.startServicesIntent(this))
    }

    private fun stop() {
        sendBroadcast(AppLocalReceiver.stopServicesIntent(this))
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openMainActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                MainActivity.createIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .toPending(this, 0, PendingIntentType.ACTIVITY)
            )
        } else {
            startActivityAndCollapse(
                MainActivity.createIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
