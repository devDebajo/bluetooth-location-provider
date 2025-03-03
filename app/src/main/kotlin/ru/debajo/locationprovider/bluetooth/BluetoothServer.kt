package ru.debajo.locationprovider.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.debajo.locationprovider.utils.runCatchingAsync
import java.util.UUID

internal class BluetoothServer(
    private val bluetoothManager: BluetoothManager,
    private val coroutineScope: CoroutineScope,
) {
    private var job: Job? = null
    private var serverSocket: BluetoothServerSocket? = null

    private val _messages: MutableSharedFlow<String> = MutableSharedFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start(address: UUID = bluetoothServerUuid) {
        job?.cancel()
        job = coroutineScope.launch(Dispatchers.IO) {
            val serverSocket = runCatchingAsync {
                bluetoothManager.adapter.listenUsingRfcommWithServiceRecord(bluetoothServerName, address)
            }.getOrNull()
            this@BluetoothServer.serverSocket = serverSocket

            if (serverSocket == null) {
                _isRunning.value = false
            } else {
                _isRunning.value = true
                while (true) {
                    val socket = runCatchingAsync { serverSocket.accept() }.getOrNull()
                    if (socket == null) {
                        Log.e("BluetoothServer", "serverSocket.accept error")
                        delay(1000)
                        continue
                    }
                    runCatchingAsync {
                        socket.inputStream.bufferedReader().lineSequence().asFlow().collect { message ->
                            _messages.emit(message)
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        job?.cancel()
        job = null
        runCatchingAsync { serverSocket?.close() }.getOrNull()
        _isRunning.value = false
    }

    companion object {
        val bluetoothServerName: String = "BluetoothServer"
        val bluetoothServerUuid: UUID = UUID.fromString("cb9121b3-243c-46a9-b114-d1c72c51578a")
    }
}
