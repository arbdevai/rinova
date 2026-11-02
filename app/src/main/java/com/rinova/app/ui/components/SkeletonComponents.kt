package com.rinova.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rinova.app.ui.theme.SlateSurface
import com.rinova.app.ui.theme.SlateSurfaceHighlight
import com.rinova.app.ui.theme.SlateSurfaceVariant

fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1100
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat().coerceAtLeast(400f),
        targetValue = 2 * size.width.toFloat().coerceAtLeast(400f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                SlateSurfaceVariant,
                SlateSurfaceHighlight,
                SlateSurfaceVariant
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat().coerceAtLeast(400f), size.height.toFloat().coerceAtLeast(200f))
        ),
        shape = shape
    ).clip(shape).onGloballyPositioned { size = it.size }
}

@Composable
fun SkeletonBox(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier = modifier.shimmerEffect(shape))
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    heightDp: Int = 96
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(18.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
            )
        }
    }
}

@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                )
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SkeletonBox(
                modifier = Modifier
                    .size(width = 64.dp, height = 28.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }
    }
}

@Composable
fun SkeletonDashboard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonCard(heightDp = 130)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
        repeat(3) {
            SkeletonListItem()
        }
    }
}
