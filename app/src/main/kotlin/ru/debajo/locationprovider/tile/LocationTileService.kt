package ru.debajo.locationprovider.tile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
            appServiceState.observeServiceRunning().collect { isRunning ->
                qsTile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                qsTile.updateTile()
            }
        }
    }

    override fun onTileAdded() {
        qsTile.state = if (appServiceState.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    override fun onClick() {
        launch {
            when (qsTile.state) {
                Tile.STATE_ACTIVE -> {
                    stop()
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }

                Tile.STATE_INACTIVE -> {
                    if (canStart()) {
                        start()
                        qsTile.state = Tile.STATE_ACTIVE
                        qsTile.updateTile()
                    } else {
                        openMainActivity()
                    }
                }
            }
        }
    }

    override fun onStopListening() {
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
