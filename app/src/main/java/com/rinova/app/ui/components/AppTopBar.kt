package com.rinova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.ui.theme.CyanPrimary
import com.rinova.app.ui.theme.SlateBackground
import com.rinova.app.ui.theme.SlateSurfaceVariant
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import com.rinova.app.ui.theme.TextPrimary
import com.rinova.app.ui.theme.TextSecondary

@Composable
fun AppTopBar(
    connectionState: AdbConnectionState,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateBackground,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RINOVA",
                        color = CyanPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TOOLKIT",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "ADB Wireless Diagnostics & Hunter",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Connection Badge
            val (statusText, statusColor) = when (connectionState) {
                is AdbConnectionState.Connected -> Pair("${connectionState.device.brand} ${connectionState.device.model}", StatusSafe)
                is AdbConnectionState.Connecting, is AdbConnectionState.Authorizing -> Pair("Menghubungkan...", StatusWarning)
                is AdbConnectionState.Error -> Pair("Gagal Terhubung", StatusCritical)
                is AdbConnectionState.Disconnected -> Pair("Terputus", TextSecondary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SlateSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
