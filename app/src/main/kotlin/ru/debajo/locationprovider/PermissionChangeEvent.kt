package ru.debajo.locationprovider

internal sealed interface PermissionChangeEvent {
    val granted: Boolean

    class BluetoothPermissionsChanged(override val granted: Boolean) : PermissionChangeEvent
    class NotificationsPermissionChanged(override val granted: Boolean) : PermissionChangeEvent
    class LocationPermissionChanged(override val granted: Boolean) : PermissionChangeEvent
    class BackgroundLocationPermissionChanged(override val granted: Boolean) : PermissionChangeEvent
    class MockLocationPermissionChanged(override val granted: Boolean) : PermissionChangeEvent
}
