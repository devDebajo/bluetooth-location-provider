package ru.debajo.locationprovider.utils

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class Preferences(
    sharedPreferences: SharedPreferences,
) {
    val isProvider: BooleanPreference = BooleanPreference(
        key = "is_provider",
        sharedPreferences = sharedPreferences,
    )

    val selectedReceiver: StringPreference = StringPreference(
        key = "selected_receiver",
        sharedPreferences = sharedPreferences,
    )
}

internal abstract class BasePreference<T>(
    private val key: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: T,
) {
    private val _state: MutableStateFlow<T> = MutableStateFlow(get())
    val state: StateFlow<T> = _state.asStateFlow()

    protected abstract fun put(editor: SharedPreferences.Editor, key: String, value: T)

    protected abstract fun get(preferences: SharedPreferences, key: String, defaultValue: T): T

    fun put(value: T) {
        sharedPreferences.edit()
            .also { put(it, key, value) }
            .apply()
        _state.value = value
    }

    fun get(): T = get(sharedPreferences, key, defaultValue)
}

internal class BooleanPreference(
    key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: Boolean = false,
) : BasePreference<Boolean>(
    key = key,
    sharedPreferences = sharedPreferences,
    defaultValue = defaultValue,
) {
    override fun put(editor: SharedPreferences.Editor, key: String, value: Boolean) {
        editor.putBoolean(key, value)
    }

    override fun get(preferences: SharedPreferences, key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }
}

internal class StringPreference(
    key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: String? = null,
) : BasePreference<String?>(
    key = key,
    sharedPreferences = sharedPreferences,
    defaultValue = defaultValue,
) {
    override fun put(editor: SharedPreferences.Editor, key: String, value: String?) {
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
    }

    override fun get(preferences: SharedPreferences, key: String, defaultValue: String?): String? {
        return preferences.getString(key, defaultValue)
    }
}
