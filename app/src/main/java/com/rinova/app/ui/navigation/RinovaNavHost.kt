package com.rinova.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.di.RinovaContainer
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.ui.components.AppTopBar
import com.rinova.app.ui.screens.adware.AdwareHunterScreen
import com.rinova.app.ui.screens.ai.AiDiagnosticScreen
import com.rinova.app.ui.screens.connect.ConnectScreen
import com.rinova.app.ui.screens.dashboard.DashboardScreen
import com.rinova.app.ui.screens.debloat.DebloatScreen
import com.rinova.app.ui.screens.storage.StorageScreen
import com.rinova.app.ui.theme.OutlineVariant

enum class RinovaTab(val label: String, val icon: ImageVector) {
    CONNECT("Sambungan", Icons.Default.Devices),
    DASHBOARD("Status", Icons.Default.Speed),
    STORAGE("Storage", Icons.Default.FolderDelete),
    ADWARE("Adware", Icons.Default.Security),
    DEBLOAT("Debloat", Icons.Default.DeleteSweep),
    AI("AI Audit", Icons.Default.AutoAwesome)
}

@Composable
fun RinovaMainScreen(
    container: RinovaContainer,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(RinovaTab.CONNECT) }
    val connectionState by container.adbRepository.connectionState.collectAsStateWithLifecycle()

    BackHandler(enabled = currentTab != RinovaTab.DASHBOARD && currentTab != RinovaTab.CONNECT) {
        currentTab = if (connectionState is AdbConnectionState.Connected) RinovaTab.DASHBOARD else RinovaTab.CONNECT
    }

    Scaffold(
        topBar = {
            AppTopBar(
                connectionState = connectionState,
                modifier = Modifier
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.8.dp, color = OutlineVariant)
            ) {
                RinovaTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "TabContentAnimation"
            ) { tab ->
                when (tab) {
                    RinovaTab.CONNECT -> ConnectScreen(
                        adbRepository = container.adbRepository,
                        settingsRepository = container.settingsRepository,
                        connectionState = connectionState,
                        onConnectSuccess = { currentTab = RinovaTab.DASHBOARD }
                    )
                    RinovaTab.DASHBOARD -> DashboardScreen(
                        connectionState = connectionState,
                        adbRepository = container.adbRepository,
                        storageRepository = container.storageRepository,
                        updateManager = container.updateManager,
                        onNavigateToStorage = { currentTab = RinovaTab.STORAGE },
                        onNavigateToAdware = { currentTab = RinovaTab.ADWARE },
                        onNavigateToDebloat = { currentTab = RinovaTab.DEBLOAT },
                        onNavigateToAi = { currentTab = RinovaTab.AI }
                    )
                    RinovaTab.STORAGE -> StorageScreen(
                        storageRepository = container.storageRepository
                    )
                    RinovaTab.ADWARE -> AdwareHunterScreen(
                        adwareHunterRepository = container.adwareHunterRepository
                    )
                    RinovaTab.DEBLOAT -> DebloatScreen(
                        debloatRepository = container.debloatRepository,
                        connectionState = connectionState
                    )
                    RinovaTab.AI -> AiDiagnosticScreen(
                        aiRepository = container.aiDiagnosticRepository,
                        settingsRepository = container.settingsRepository
                    )
                }
            }
        }
    }
}
