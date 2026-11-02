package com.rinova.app.core.model

sealed interface AdbConnectionState {
    data object Disconnected : AdbConnectionState
    data class Connecting(val host: String, val port: Int) : AdbConnectionState
    data class Authorizing(val host: String, val port: Int) : AdbConnectionState
    data class Connected(
        val host: String,
        val port: Int,
        val device: DeviceInfo
    ) : AdbConnectionState
    data class Error(val message: String) : AdbConnectionState
}
