package com.rinova.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.log.LogEntry
import com.rinova.app.core.log.LogLevel
import com.rinova.app.core.log.RinovaLogManager
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusInfo
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning

/**
 * KONSOL LOG TELEMETRI REAL-TIME (Untuk Debugging & Analisis Lapangan Teknisi)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RinovaLogViewerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val logs by RinovaLogManager.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("SEMUA") } // SEMUA, ERROR, PAIR, SHELL, UPDATE

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { entry ->
            val matchFilter = when (selectedFilter) {
                "ERROR" -> entry.level == LogLevel.ERROR
                "WARN" -> entry.level == LogLevel.WARN
                "PAIR" -> entry.tag.contains("PAIR", ignoreCase = true) || entry.tag.contains("CONN", ignoreCase = true)
                "SHELL" -> entry.tag.contains("SHELL", ignoreCase = true)
                "UPDATE" -> entry.tag.contains("UPDATE", ignoreCase = true)
                else -> true
            }

            val matchQuery = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                entry.message.contains(searchQuery.trim(), ignoreCase = true) ||
                entry.tag.contains(searchQuery.trim(), ignoreCase = true) ||
                (entry.stackTrace?.contains(searchQuery.trim(), ignoreCase = true) == true)
            }

            matchFilter && matchQuery
        }
    }

    // Auto-scroll ke paling bawah saat ada log baru jika belum di-filter
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty() && searchQuery.isEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1116),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF384050))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title + Action Icons (Copy, Clear, Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Konsol Log Telemetri",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF3F4F6),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredLogs.size} dari ${logs.size} log tercatat",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tombol Salin Semua Log
                    IconButton(
                        onClick = {
                            val success = RinovaLogManager.copyToClipboard(context)
                            if (success) {
                                Toast.makeText(context, "Log berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Semua Log",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Tombol Bersihkan Log
                    IconButton(onClick = { RinovaLogManager.clear() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Bersihkan Log",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Tombol Tutup
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar
            RinovaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Cari kata kunci log (misal: pair, timeout, socket, pm)...",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf("SEMUA", "ERROR", "PAIR", "SHELL", "UPDATE")
                filters.forEach { tag ->
                    val isSel = selectedFilter == tag
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFilter = tag },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (tag == "ERROR") StatusCritical else Primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF161920),
                            labelColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }

            // Log Console Terminal Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF090A0E))
                    .border(0.8.dp, OutlineVariant, RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada log yang cocok dengan filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.matchParentSize()
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogItemRow(log)
                        }
                    }
                }
            }

            // Quick Copy Full Log Bar
            RinovaTactileButton(
                text = "Salin Seluruh Log Diagnostik",
                icon = Icons.Default.ContentCopy,
                onClick = {
                    val success = RinovaLogManager.copyToClipboard(context)
                    if (success) {
                        Toast.makeText(context, "Log berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }
                },
                isSecondary = true,
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp
            )
        }
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when (entry.level) {
        LogLevel.ERROR -> StatusCritical
        LogLevel.WARN -> StatusWarning
        LogLevel.INFO -> StatusSafe
        LogLevel.DEBUG -> StatusInfo
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF11141A))
            .border(0.5.dp, Color(0xFF1F2430), RoundedCornerShape(8.dp))
            .clickable {
                if (!entry.stackTrace.isNullOrBlank()) {
                    expanded = !expanded
                }
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Level Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(levelColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.level.name,
                        color = levelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Tag
                Text(
                    text = "[${entry.tag}]",
                    color = Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Timestamp
            Text(
                text = entry.formattedTime,
                color = Color(0xFF6B7280),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Message
        Text(
            text = entry.message,
            color = Color(0xFFE5E7EB),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontFamily = FontFamily.Monospace
        )

        // StackTrace if available
        if (!entry.stackTrace.isNullOrBlank()) {
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF07080B))
                        .padding(6.dp)
                ) {
                    Text(
                        text = entry.stackTrace,
                        color = StatusCritical.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "▶ Ketuk untuk melihat stack trace lengkap",
                    color = StatusWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
