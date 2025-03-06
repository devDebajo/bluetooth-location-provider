package ru.debajo.locationprovider

import android.app.Application
import ru.debajo.locationprovider.utils.AppLocalReceiver

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        registerReceiver(AppLocalReceiver(), AppLocalReceiver.intentFilter(), RECEIVER_NOT_EXPORTED)
    }

    companion object {
        lateinit var INSTANCE: App
    }
}
