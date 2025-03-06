package ru.debajo.locationprovider

import android.app.Activity
import java.lang.ref.WeakReference

class ActivityHolder {
    private var ref: WeakReference<Activity>? = null

    fun getOrNull(): Activity? = ref?.get()

    fun register(activity: Activity) {
        ref?.clear()
        ref = WeakReference(activity)
    }
}
