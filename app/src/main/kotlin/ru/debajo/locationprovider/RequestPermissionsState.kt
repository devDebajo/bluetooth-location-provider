package ru.debajo.locationprovider

internal enum class RequestPermissionsState {
    None,
    RequestedNotificationsPermission,
    RequestedBluetoothPermission,
    RequestedBluetoothPermissionForShowList,
    RequestedLocationPermission,
}
