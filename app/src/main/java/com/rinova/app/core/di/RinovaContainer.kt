package com.rinova.app.core.di

import android.content.Context
import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.ai.GeminiApiClient
import com.rinova.app.core.update.UpdateManager
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.AdwareHunterRepository
import com.rinova.app.data.repository.AiDiagnosticRepository
import com.rinova.app.data.repository.DebloatRepository
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.data.repository.StorageRepository

class RinovaContainer(context: Context) {
    val adbManager = AdbConnectionManager(context.applicationContext)
    val geminiClient = GeminiApiClient()

    val settingsRepository = SettingsRepository(context.applicationContext)
    val adbRepository = AdbRepository(adbManager)
    val storageRepository = StorageRepository(adbManager)
    val adwareHunterRepository = AdwareHunterRepository(adbManager)
    val debloatRepository = DebloatRepository(adbManager)
    val aiDiagnosticRepository = AiDiagnosticRepository(
        adbManager = adbManager,
        storageRepo = storageRepository,
        adwareRepo = adwareHunterRepository,
        geminiClient = geminiClient
    )
    val updateManager = UpdateManager(context.applicationContext)
}
