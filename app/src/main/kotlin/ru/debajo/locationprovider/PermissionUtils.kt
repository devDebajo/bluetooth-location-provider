package ru.debajo.locationprovider

import android.Manifest
import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.debajo.locationprovider.utils.Di

object PermissionUtils {
    val BluetoothPermissionsList: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(Manifest.permission.BLUETOOTH)
    }

    val NotificationsPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        ""
    }

    val LocationPermissionsList: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val BackgroundLocationPermission: String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun hasBluetoothPermissions(): Boolean = hasPermissions(BluetoothPermissionsList)

    fun hasLocationPermissions(): Boolean = hasPermissions(LocationPermissionsList)

    fun hasBackgroundLocationPermission(): Boolean = hasPermission(BackgroundLocationPermission)

    fun hasNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(NotificationsPermission)
        } else {
            true
        }
    }

    fun hasPermissionsForProvider(): Boolean {
        return hasNotificationsPermission() &&
                hasBluetoothPermissions() &&
                hasLocationPermissions() &&
                hasBackgroundLocationPermission()
    }

    fun hasPermissionsForReceiver(): Boolean {
        return hasNotificationsPermission() &&
                hasBluetoothPermissions() &&
                hasLocationPermissions() &&
                hasMockLocationPermission()
    }

    fun hasPermissions(permissions: List<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(Di.context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMockLocationPermission(): Boolean {
        return try {
            Di.opsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                Di.context.applicationInfo.uid,
                Di.context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        val activity = Di.activityHolder.getOrNull() ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
