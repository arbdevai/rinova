package com.rinova.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
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
import com.rinova.app.ui.theme.CyanOnPrimary
import com.rinova.app.ui.theme.CyanPrimary
import com.rinova.app.ui.theme.SlateBackground
import com.rinova.app.ui.theme.SlateSurface
import com.rinova.app.ui.theme.SlateSurfaceVariant
import com.rinova.app.ui.theme.TextPrimary
import com.rinova.app.ui.theme.TextSecondary

enum class RinovaTab(val label: String, val icon: ImageVector) {
    CONNECT("Sambungan", Icons.Default.Wifi),
    DASHBOARD("Status", Icons.Default.Dashboard),
    STORAGE("Storage", Icons.Default.CleaningServices),
    ADWARE("Adware", Icons.Default.Shield),
    DEBLOAT("Debloat", Icons.Default.Build),
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
                containerColor = SlateSurface,
                modifier = Modifier.fillMaxWidth()
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
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanOnPrimary,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        containerColor = SlateBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateBackground)
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
