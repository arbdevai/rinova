package com.rinova.app.ui.screens.debloat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DebloatPackage
import com.rinova.app.core.model.DebloatRisk
import com.rinova.app.core.util.VendorCategory
import com.rinova.app.core.util.VendorProfileDetector
import com.rinova.app.data.repository.DebloatRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.components.SkeletonListItem
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@Composable
fun DebloatScreen(
    debloatRepository: DebloatRepository,
    connectionState: AdbConnectionState,
    modifier: Modifier = Modifier
) {
    val debloatList by debloatRepository.debloatList.collectAsStateWithLifecycle()
    val isLoading by debloatRepository.isLoading.collectAsStateWithLifecycle()

    var selectedVendor by remember { mutableStateOf(VendorCategory.UNIVERSAL) }
    var riskFilter by remember { mutableStateOf<DebloatRisk?>(null) } // null = All
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var pendingPackageAction by remember { mutableStateOf<Pair<DebloatPackage, String>?>(null) } // Pair(pkg, actionType)
    val coroutineScope = rememberCoroutineScope()

    // Auto-detect vendor on connect
    LaunchedEffect(connectionState) {
        if (connectionState is AdbConnectionState.Connected) {
            val detected = VendorProfileDetector.detect(
                connectionState.device.manufacturer,
                connectionState.device.brand,
                connectionState.device.customRomBrand
            )
            selectedVendor = detected
            debloatRepository.loadDebloatList(detected)
        } else {
            debloatRepository.loadDebloatList(VendorCategory.UNIVERSAL)
        }
    }

    if (pendingPackageAction != null) {
        val (item, actionType) = pendingPackageAction!!
        val isUninstall = actionType == "uninstall"
        CustomActionDialog(
            title = if (isUninstall) "Copot Paket Vendor" else "Ubah Status Paket",
            message = if (isUninstall) {
                "Copot '${item.appLabel}' (${item.packageName}) dari profil pengguna target? Paket sistem tetap utuh dan dapat direstorasi sewaktu-waktu."
            } else {
                "Nonaktifkan '${item.appLabel}' (${item.packageName})?"
            },
            commandPreview = if (isUninstall) "pm uninstall -k --user 0 ${item.packageName}" else "pm disable-user --user 0 ${item.packageName}",
            alertType = if (item.risk == DebloatRisk.SAFE) AlertType.WARNING else AlertType.CRITICAL,
            confirmButtonText = if (isUninstall) "Copot Paket" else "Konfirmasi",
            onConfirm = {
                pendingPackageAction = null
                coroutineScope.launch {
                    val res = if (isUninstall) {
                        debloatRepository.uninstallPackage(item)
                    } else {
                        debloatRepository.togglePackage(item).map { "Status diperbarui" }
                    }
                    res.fold(
                        onSuccess = { actionNotification = "Tindakan berhasil dieksekusi: $it" },
                        onFailure = { actionNotification = "Gagal memproses: ${it.localizedMessage}" }
                    )
                }
            },
            onDismiss = { pendingPackageAction = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vendor Selection Horizontal Carousel
        Text(
            text = "Kategori Vendor Perangkat",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VendorCategory.values().forEach { cat ->
                val isSelected = selectedVendor == cat
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedVendor = cat
                        coroutineScope.launch {
                            debloatRepository.loadDebloatList(cat)
                        }
                    },
                    label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF16191F),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Risk Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = riskFilter == null,
                onClick = { riskFilter = null },
                label = { Text("Semua", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF16191F),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            FilterChip(
                selected = riskFilter == DebloatRisk.SAFE,
                onClick = { riskFilter = DebloatRisk.SAFE },
                label = { Text("Aman Dihapus", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusSafe,
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF16191F),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            FilterChip(
                selected = riskFilter == DebloatRisk.ADVANCED,
                onClick = { riskFilter = DebloatRisk.ADVANCED },
                label = { Text("Lanjutan", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusWarning,
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF16191F),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        if (actionNotification != null) {
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 14.dp
            ) {
                Text(
                    text = actionNotification ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Package List
        val filteredList = debloatList.filter { item ->
            riskFilter == null || item.risk == riskFilter
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Daftar Paket Vendor (${filteredList.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            RinovaTactileButton(
                text = "Refresh",
                onClick = {
                    coroutineScope.launch {
                        debloatRepository.loadDebloatList(selectedVendor)
                    }
                },
                icon = Icons.Default.Refresh,
                isSecondary = true,
                height = 36.dp
            )
        }

        if (isLoading) {
            repeat(3) { SkeletonListItem() }
        } else if (filteredList.isEmpty()) {
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 16.dp
            ) {
                Text(
                    text = "Tidak ada paket yang sesuai dengan filter saat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            filteredList.forEach { pkg ->
                DebloatPackageCard(
                    pkg = pkg,
                    onToggle = {
                        if (pkg.isEnabled) {
                            pendingPackageAction = Pair(pkg, "disable")
                        } else {
                            coroutineScope.launch {
                                debloatRepository.togglePackage(pkg)
                            }
                        }
                    },
                    onUninstall = {
                        pendingPackageAction = Pair(pkg, "uninstall")
                    },
                    onRestore = {
                        coroutineScope.launch {
                            val res = debloatRepository.restorePackage(pkg)
                            res.fold(
                                onSuccess = { actionNotification = "Paket ${pkg.appLabel} berhasil dipulihkan." },
                                onFailure = { actionNotification = "Gagal memulihkan: ${it.localizedMessage}" }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DebloatPackageCard(
    pkg: DebloatPackage,
    onToggle: () -> Unit,
    onUninstall: () -> Unit,
    onRestore: () -> Unit
) {
    val riskColor = when (pkg.risk) {
        DebloatRisk.SAFE -> StatusSafe
        DebloatRisk.ADVANCED -> StatusWarning
        DebloatRisk.CRITICAL -> StatusCritical
    }

    RinovaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shapeRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pkg.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (pkg.risk == DebloatRisk.SAFE) "Aman Dihapus" else "Lanjutan",
                        color = riskColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = pkg.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusLabel = if (!pkg.isInstalled) "Dicopot" else if (pkg.isEnabled) "Aktif" else "Dinonaktifkan"
                    val statusColor = if (!pkg.isInstalled) MaterialTheme.colorScheme.onSurfaceVariant else if (pkg.isEnabled) StatusSafe else StatusWarning
                    Text(
                        text = "Status: $statusLabel",
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pkg.isInstalled) {
                        Switch(
                            checked = pkg.isEnabled,
                            onCheckedChange = { onToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Primary,
                                checkedTrackColor = Primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = Color(0xFF282D38)
                            )
                        )
                        RinovaTactileButton(
                            text = "Copot",
                            onClick = onUninstall,
                            isSecondary = true,
                            height = 36.dp
                        )
                    } else {
                        RinovaTactileButton(
                            text = "Pulihkan",
                            onClick = onRestore,
                            icon = Icons.Default.Restore,
                            height = 36.dp
                        )
                    }
                }
            }
        }
    }
}
