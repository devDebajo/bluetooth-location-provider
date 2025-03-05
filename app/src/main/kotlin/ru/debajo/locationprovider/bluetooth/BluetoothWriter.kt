package ru.debajo.locationprovider.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedWriter

internal class BluetoothWriter(
    socket: BluetoothSocket,
    private val json: Json,
) {
    private val writer: BufferedWriter = socket.outputStream.bufferedWriter()

    suspend fun <T> write(obj: T, serializer: KSerializer<T>) {
        write(json.encodeToString(serializer, obj))
    }

    suspend fun write(message: String) {
        return withContext(Dispatchers.IO) {
            writer.write(message)
            writer.newLine()
            writer.flush()
        }
    }
}
