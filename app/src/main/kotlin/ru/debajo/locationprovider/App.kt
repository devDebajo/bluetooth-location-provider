package ru.debajo.locationprovider

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import ru.debajo.locationprovider.utils.AppLocalReceiver

internal class App : Application() {
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(AppLocalReceiver(), AppLocalReceiver.intentFilter(), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(AppLocalReceiver(), AppLocalReceiver.intentFilter())
        }
    }

    companion object {
        lateinit var INSTANCE: App
    }
}
