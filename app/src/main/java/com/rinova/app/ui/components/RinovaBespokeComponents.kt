package com.rinova.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.GeminiModelInfo
import com.rinova.app.ui.theme.Outline
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusSafe

/**
 * 1. TACTILE GRADIENT BUTTON (Anti-Default Compose Flat Box)
 * Menghasilkan tombol fisik dengan pendaran gradasi Citrus Orange dan animasi pegas lentur saat ditekan.
 */
@Composable
fun RinovaTactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    height: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ButtonPressScale"
    )

    val gradientBrush = if (isSecondary) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF282E3A), Color(0xFF1E222B))
        )
    } else {
        Brush.horizontalGradient(
            colors = if (enabled) listOf(Color(0xFFFF8A00), Color(0xFFFF5A00)) else listOf(Color(0xFF45220A), Color(0xFF331A08))
        )
    }

    val contentColor = if (isSecondary) Color(0xFFF3F4F6) else if (enabled) Color(0xFFFFFFFF) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(gradientBrush)
            .border(
                width = 0.8.dp,
                color = if (isSecondary) OutlineVariant else Color(0xFFFF9E3D).copy(alpha = if (enabled) 0.4f else 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * 2. BESPOKE SURFACE CARD (Tonal Depth & Rim Light)
 * Panel bertekstur arang bertingkat dengan aksen tepi atas berkilau halus.
 */
@Composable
fun RinovaSurfaceCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 18.dp,
    borderWidth: Dp = 0.8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B1E26),
            Color(0xFF14171E)
        )
    )

    Card(
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .background(cardBrush, RoundedCornerShape(shapeRadius))
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF384050),
                        Color(0xFF1F242E)
                    )
                ),
                shape = RoundedCornerShape(shapeRadius)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        content()
    }
}

/**
 * 3. RECESSED TEXT FIELD (Anti-Outlined Plain Box)
 * Kolom input dengan wadah ikon tersendiri dan garis pendar oranye saat fokus.
 */
@Composable
fun RinovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isFocused) Primary else Color(0xFF9CA3AF),
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F1116))
                .border(
                    width = if (isFocused) 1.2.dp else 0.8.dp,
                    color = if (isFocused) Primary else Color(0xFF282D38),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFocused) PrimaryContainer else Color(0xFF1B1E26)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (isFocused) Primary else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        textStyle = TextStyle(
                            color = Color(0xFFF3F4F6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }

                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * 4. BESPOKE BENTO STAT TILE
 */
@Composable
fun RinovaBentoStat(
    icon: ImageVector,
    label: String,
    primaryValue: String,
    subValue: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Primary
) {
    RinovaSurfaceCard(modifier = modifier, shapeRadius = 14.dp) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF3F4F6),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                maxLines = 1
            )
        }
    }
}

/**
 * 5. SEARCHABLE GEMINI MODEL PICKER BOTTOM SHEET (100% Dynamic, Zero Hardcode)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RinovaModelPickerSheet(
    sheetState: SheetState,
    models: List<GeminiModelInfo>,
    selectedModelId: String,
    onModelSelected: (GeminiModelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            models
        } else {
            models.filter {
                it.id.contains(searchQuery.trim(), ignoreCase = true) ||
                it.displayName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14171E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF384050))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pilih Model Gemini AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF3F4F6),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${models.size} model aktif terdeteksi dari kunci API Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }

            // Search Bar
            RinovaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Cari nama model (misal: 2.0-flash, pro, exp)...",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            // Models List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredModels, key = { it.id }) { model ->
                    val isSelected = model.id == selectedModelId
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp,
                        borderWidth = if (isSelected) 1.2.dp else 0.8.dp,
                        onClick = {
                            onModelSelected(model)
                            onDismiss()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) PrimaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = model.id,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) Primary else Color(0xFFF3F4F6),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (model.isRecommended) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "FLASH",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "In: ${model.inputTokenLimit / 1000}k tok",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9CA3AF),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "•",
                                        color = Color(0xFF4B5563)
                                    )
                                    Text(
                                        text = "Out: ${model.outputTokenLimit / 1000}k tok",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9CA3AF),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF1F0B00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
