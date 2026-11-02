package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.model.DebloatPackage
import com.rinova.app.core.util.VendorCategory
import com.rinova.app.data.debloat.SafeWhitelist
import com.rinova.app.data.debloat.UniversalVendorRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DebloatRepository(
    private val adbManager: AdbConnectionManager
) {
    private val _debloatList = MutableStateFlow<List<DebloatPackage>>(emptyList())
    val debloatList: StateFlow<List<DebloatPackage>> = _debloatList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadDebloatList(vendorCategory: VendorCategory): Result<List<DebloatPackage>> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val presets = UniversalVendorRules.getPresetRules(vendorCategory)

            // Ambil daftar paket terpasang & nonaktif
            val installedResp = adbManager.execute("pm list packages")
            val installedSet = installedResp.getOrNull()?.output?.lines()
                ?.map { it.removePrefix("package:").trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()

            val disabledResp = adbManager.execute("pm list packages -d")
            val disabledSet = disabledResp.getOrNull()?.output?.lines()
                ?.map { it.removePrefix("package:").trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()

            val updatedList = presets.map { item ->
                val isInstalled = installedSet.contains(item.packageName)
                val isEnabled = isInstalled && !disabledSet.contains(item.packageName)
                item.copy(isInstalled = isInstalled, isEnabled = isEnabled)
            }

            _debloatList.value = updatedList
            Result.success(updatedList)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun togglePackage(item: DebloatPackage): Result<Boolean> = withContext(Dispatchers.IO) {
        if (SafeWhitelist.isCritical(item.packageName)) {
            return@withContext Result.failure(IllegalStateException("Paket ini dilindungi oleh whitelist sistem"))
        }

        val result = if (item.isEnabled) {
            adbManager.execute("pm disable-user --user 0 ${item.packageName}")
        } else {
            adbManager.execute("pm enable ${item.packageName}")
        }

        result.fold(
            onSuccess = {
                val newEnabled = !item.isEnabled
                _debloatList.value = _debloatList.value.map {
                    if (it.packageName == item.packageName) it.copy(isEnabled = newEnabled) else it
                }
                Result.success(newEnabled)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun uninstallPackage(item: DebloatPackage): Result<String> = withContext(Dispatchers.IO) {
        if (SafeWhitelist.isCritical(item.packageName)) {
            return@withContext Result.failure(IllegalStateException("Paket ini dilindungi oleh whitelist sistem"))
        }
        val resp = adbManager.execute("pm uninstall -k --user 0 ${item.packageName}")
        resp.fold(
            onSuccess = { res ->
                _debloatList.value = _debloatList.value.map {
                    if (it.packageName == item.packageName) it.copy(isInstalled = false, isEnabled = false) else it
                }
                Result.success(res.allOutput)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun restorePackage(item: DebloatPackage): Result<String> = withContext(Dispatchers.IO) {
        val resp = adbManager.execute("cmd package install-existing ${item.packageName}")
        resp.fold(
            onSuccess = { res ->
                _debloatList.value = _debloatList.value.map {
                    if (it.packageName == item.packageName) it.copy(isInstalled = true, isEnabled = true) else it
                }
                Result.success(res.allOutput)
            },
            onFailure = { Result.failure(it) }
        )
    }
}
