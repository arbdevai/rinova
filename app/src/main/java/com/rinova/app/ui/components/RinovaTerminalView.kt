package com.rinova.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.ui.theme.CyanPrimary
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning

@Composable
fun RinovaTerminalConsoleView(
    title: String,
    logs: List<String>,
    progress: Float = -1f,
    currentStepText: String = "",
    isRunning: Boolean = true,
    modifier: Modifier = Modifier,
    maxHeight: Int = 220
) {
    val listState = rememberLazyListState()

    // Otomatis scroll ke baris log terbaru setiap ada output baru
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0B0D11),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1F2430), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar Terminal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = alphaAnim))
                        )
                        Text(
                            text = if (progress >= 0f) "${(progress * 100).coerceIn(0f, 100f).toInt()}%" else "LIVE",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "SELESAI",
                            color = StatusSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Indikator Progress Garis
            if (isRunning) {
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Primary,
                        trackColor = Color(0xFF1E222B)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Primary,
                        trackColor = Color(0xFF1E222B)
                    )
                }
            }

            // Layar Konsol Terminal Monospace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = maxHeight.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF07090C))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = if (currentStepText.isNotEmpty()) currentStepText else "> Menyiapkan lingkungan terminal adb...",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { logLine ->
                            TerminalLogLine(logLine)
                        }
                    }
                }
            }

            // Footer Status Teks
            if (currentStepText.isNotEmpty()) {
                Text(
                    text = currentStepText,
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TerminalLogLine(line: String) {
    val trimmed = line.trim()
    val (color, prefix) = when {
        trimmed.startsWith(">") || trimmed.startsWith("$") -> Pair(Primary, "")
        trimmed.startsWith("[OK]") || trimmed.startsWith("[SUCCESS]") -> Pair(StatusSafe, "")
        trimmed.startsWith("[THREAT]") || trimmed.startsWith("[CRITICAL]") || trimmed.startsWith("[ERR]") -> Pair(StatusCritical, "")
        trimmed.startsWith("[WARN]") -> Pair(StatusWarning, "")
        trimmed.startsWith("[RUN]") || trimmed.startsWith("[EXEC]") -> Pair(CyanPrimary, "")
        trimmed.startsWith("[INFO]") || trimmed.startsWith("[AUDIT]") -> Pair(Color(0xFF93C5FD), "")
        else -> Pair(Color(0xFFD1D5DB), "")
    }

    Text(
        text = line,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 15.sp
    )
}
