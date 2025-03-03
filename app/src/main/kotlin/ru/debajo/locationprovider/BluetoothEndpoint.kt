package ru.debajo.locationprovider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class BluetoothEndpoint(
    @SerialName("address")
    val address: String,

    @SerialName("name")
    val name: String,
)
