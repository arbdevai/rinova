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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer

// 4 TAB KERJA UTAMA TEKNISI (Ergonomis, Bebas Sempit, Standar M3)
enum class RinovaWorkspaceTab(val label: String, val icon: ImageVector) {
    IKHTISAR("Ikhtisar", Icons.Default.Speed),
    STORAGE("Storage", Icons.Default.FolderDelete),
    KEAMANAN("Keamanan", Icons.Default.Security),
    OPTIMASI("Optimasi", Icons.Default.AutoAwesome)
}

@Composable
fun RinovaMainScreen(
    container: RinovaContainer,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(RinovaWorkspaceTab.IKHTISAR) }
    var optimasiSubTab by remember { mutableIntStateOf(0) } // 0 = Debloat, 1 = Gemini AI
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    val connectionState by container.adbRepository.connectionState.collectAsStateWithLifecycle()
    val isConnected = connectionState is AdbConnectionState.Connected

    BackHandler(enabled = isConnected && currentTab != RinovaWorkspaceTab.IKHTISAR) {
        currentTab = RinovaWorkspaceTab.IKHTISAR
    }

    Scaffold(
        topBar = {
            AppTopBar(
                connectionState = connectionState,
                onStatusClick = {
                    if (isConnected) {
                        showDisconnectConfirm = true
                    }
                }
            )
        },
        bottomBar = {
            // Hanya tampilkan floating navbar saat teknisi sudah terkoneksi
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RinovaFloatingDockBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        },
        containerColor = Color(0xFF101216),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF101216))
        ) {
            if (!isConnected) {
                // GERBANG SAMBUNGAN AWAL (Full Screen Focused Gateway)
                ConnectScreen(
                    adbRepository = container.adbRepository,
                    settingsRepository = container.settingsRepository,
                    connectionState = connectionState,
                    onConnectSuccess = { currentTab = RinovaWorkspaceTab.IKHTISAR }
                )
            } else {
                // WORKSPACE TEKNISI (4 Core Tabs)
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
                        RinovaWorkspaceTab.IKHTISAR -> DashboardScreen(
                            connectionState = connectionState,
                            adbRepository = container.adbRepository,
                            storageRepository = container.storageRepository,
                            updateManager = container.updateManager,
                            onNavigateToStorage = { currentTab = RinovaWorkspaceTab.STORAGE },
                            onNavigateToAdware = { currentTab = RinovaWorkspaceTab.KEAMANAN },
                            onNavigateToDebloat = {
                                optimasiSubTab = 0
                                currentTab = RinovaWorkspaceTab.OPTIMASI
                            },
                            onNavigateToAi = {
                                optimasiSubTab = 1
                                currentTab = RinovaWorkspaceTab.OPTIMASI
                            }
                        )

                        RinovaWorkspaceTab.STORAGE -> StorageScreen(
                            storageRepository = container.storageRepository
                        )

                        RinovaWorkspaceTab.KEAMANAN -> AdwareHunterScreen(
                            adwareHunterRepository = container.adwareHunterRepository
                        )

                        RinovaWorkspaceTab.OPTIMASI -> {
                            // Modul Optimalisasi Terpadu: Segmented Header [ Debloat Vendor | Gemini AI ]
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF101216))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF181C24))
                                            .border(0.8.dp, OutlineVariant, RoundedCornerShape(12.dp))
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            Pair("Debloat Vendor", Icons.Default.Build),
                                            Pair("Gemini AI Audit", Icons.Default.AutoAwesome)
                                        ).forEachIndexed { idx, (title, icon) ->
                                            val isSel = optimasiSubTab == idx
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (isSel) PrimaryContainer else Color.Transparent)
                                                    .clickable { optimasiSubTab = idx }
                                                    .padding(vertical = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSel) Primary else Color(0xFF9CA3AF),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSel) Primary else Color(0xFF9CA3AF),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (optimasiSubTab == 0) {
                                    DebloatScreen(
                                        debloatRepository = container.debloatRepository,
                                        connectionState = connectionState
                                    )
                                } else {
                                    AiDiagnosticScreen(
                                        aiRepository = container.aiDiagnosticRepository,
                                        settingsRepository = container.settingsRepository
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * BESPOKE FLOATING DOCK NAVBAR (Anti-Kuno, Anti-Sempit, 4 Tab Luwes)
 */
@Composable
private fun RinovaFloatingDockBar(
    currentTab: RinovaWorkspaceTab,
    onTabSelected: (RinovaWorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF161920))
            .border(width = 0.8.dp, color = OutlineVariant, shape = RoundedCornerShape(22.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RinovaWorkspaceTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Primary else Color(0xFF9CA3AF),
                            modifier = Modifier
                                .size(20.dp)
                                .scale(if (isSelected) 1.08f else 1.0f)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Primary else Color(0xFF9CA3AF),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
