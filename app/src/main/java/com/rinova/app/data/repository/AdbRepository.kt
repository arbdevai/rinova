package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DeviceInfo
import dadb.AdbShellResponse
import kotlinx.coroutines.flow.StateFlow

class AdbRepository(
    private val adbManager: AdbConnectionManager
) {
    val connectionState: StateFlow<AdbConnectionState> = adbManager.connectionState

    suspend fun connect(host: String, port: Int): Result<DeviceInfo> {
        return adbManager.connect(host, port)
    }

    suspend fun disconnect() {
        adbManager.disconnect()
    }

    suspend fun executeCommand(command: String, timeoutMs: Long = 20000): Result<AdbShellResponse> {
        return adbManager.execute(command, timeoutMs)
    }

    suspend fun rebootTarget(mode: String = ""): Result<String> {
        val cmd = if (mode.isEmpty()) "reboot" else "reboot $mode"
        return executeCommand(cmd).map { it.allOutput }
    }
}
