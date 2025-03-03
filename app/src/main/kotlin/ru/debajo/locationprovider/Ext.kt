package ru.debajo.locationprovider

import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

internal fun <T : Any> SharedPreferences.Editor.putSerializable(
    key: String,
    value: T,
    serializer: KSerializer<T>,
): SharedPreferences.Editor {
    return putString(key, Di.json.encodeToString(serializer, value))
}

internal fun <T : Any> SharedPreferences.getSerializableOrNull(key: String, serializer: KSerializer<T>): T? {
    return runCatchingAsync {
        val json = getString(key, null) ?: return null
        Di.json.decodeFromString(serializer, json)
    }.getOrNull()
}

internal inline fun <T> runCatchingAsync(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
