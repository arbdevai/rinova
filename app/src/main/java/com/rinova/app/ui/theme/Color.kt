package com.rinova.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// RINOVA INDUSTRIAL THEME: JERUK ORANGE X DARK CHARCOAL (AGAK GELAP)
// Authentic Technician & Precision Engineering Aesthetic
// =========================================================================

// Primary Brand: Vibrant Citrus Orange (Jeruk Orange)
val Primary = Color(0xFFFF7A00)               // Electric Citrus Orange (#FF7A00)
val OnPrimary = Color(0xFF1F0B00)             // Deep dark contrast text on Orange
val PrimaryContainer = Color(0xFF3D1B00)      // Deep charcoal-orange container
val OnPrimaryContainer = Color(0xFFFFD8B3)    // Soft cream citrus text on container

// Secondary: Radiant Tangerine
val Secondary = Color(0xFFFB923C)             // Tangerine Accent (#FB923C)
val OnSecondary = Color(0xFF2E1000)
val SecondaryContainer = Color(0xFF451E06)
val OnSecondaryContainer = Color(0xFFFED7AA)

// Tertiary: Precision Warm Amber
val Tertiary = Color(0xFFFBBF24)              // Amber Gold (#FBBF24)
val OnTertiary = Color(0xFF331D00)
val TertiaryContainer = Color(0xFF422800)
val OnTertiaryContainer = Color(0xFFFEF3C7)

// Dark Charcoal Surface Hierarchy (Agak Gelap Dikit - Deep Matte Industrial)
val Background = Color(0xFF101216)            // Deep velvety dark charcoal (#101216)
val OnBackground = Color(0xFFF3F4F6)          // Crisp white typography (High readability)

val Surface = Color(0xFF16191F)               // Base dark charcoal surface
val OnSurface = Color(0xFFF3F4F6)             // Primary text
val OnSurfaceVariant = Color(0xFF9CA3AF)      // Secondary subtitle text

val SurfaceContainerLowest = Color(0xFF0C0E11)// Darkest recessed layer
val SurfaceContainerLow = Color(0xFF1A1D24)   // Elevated cards base
val SurfaceContainer = Color(0xFF20242D)      // Standard card layer
val SurfaceContainerHigh = Color(0xFF272C37)  // Interactive chips / search boxes
val SurfaceContainerHighest = Color(0xFF303643)// Hover / Active pill

val Outline = Color(0xFF3D4453)               // Subtle borders
val OutlineVariant = Color(0xFF282D37)        // Low-contrast separators

// Semantic Diagnostics (Harmonized with Charcoal & Citrus)
val StatusSafe = Color(0xFF34D399)            // Mint Jade
val StatusSafeContainer = Color(0xFF063720)
val OnStatusSafe = Color(0xFF022C17)

val StatusWarning = Color(0xFFF59E0B)         // Amber Warning
val StatusWarningContainer = Color(0xFF3D2605)
val OnStatusWarning = Color(0xFF451A03)

val StatusCritical = Color(0xFFF87171)        // Coral Crimson
val StatusCriticalContainer = Color(0xFF3B1212)
val OnStatusCritical = Color(0xFF450A0A)

val StatusInfo = Color(0xFF38BDF8)            // Sky Azure
val StatusInfoContainer = Color(0xFF0C3147)

// Backward Compatibility Aliases so all screens compile and render seamlessly
val CyanPrimary = Primary
val CyanOnPrimary = OnPrimary
val CyanPrimaryContainer = PrimaryContainer
val CyanOnPrimaryContainer = OnPrimaryContainer

val TealSecondary = Secondary
val TealOnSecondary = OnSecondary
val TealSecondaryContainer = SecondaryContainer
val TealOnSecondaryContainer = OnSecondaryContainer

val SlateBackground = Background
val SlateSurface = SurfaceContainerLow
val SlateSurfaceVariant = SurfaceContainer
val SlateSurfaceHighlight = OutlineVariant

val TextPrimary = OnSurface
val TextSecondary = OnSurfaceVariant
val TextTertiary = Outline
