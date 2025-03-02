package ru.debajo.locationprovider

import android.app.Application

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: App
    }
}
