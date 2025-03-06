package ru.debajo.locationprovider

internal sealed interface MainNews {
    class ShowSnackBar(val text: String) : MainNews
    data object RequestNotificationsPermission : MainNews
    data object RequestBluetoothPermission : MainNews
    data object RequestLocationPermission : MainNews
    data object RequestBackgroundLocationPermission : MainNews
}
