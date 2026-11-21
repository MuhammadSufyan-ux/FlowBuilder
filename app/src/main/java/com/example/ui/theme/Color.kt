package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Brand & Studio Surfaces
val SlateInk950 = Color(0xFF0B111B)
val SlateInk900 = Color(0xFF0F172A)
val SlateInk800 = Color(0xFF1E293B)
val SlateInk700 = Color(0xFF334155)
val SlateMuted500 = Color(0xFF64748B)
val SlateBorderLight = Color(0xFFE2E8F0)
val SlateBorderDark = Color(0xFF283548)

// Light Canvas & Surfaces
val StudioCanvasLight = Color(0xFFFAFBFD)
val StudioSurfaceLight = Color(0xFFFFFFFF)
val StudioSurfaceVariantLight = Color(0xFFF1F5F9)
val StudioGridLight = Color(0xFFD5DFEA)

// Dark Canvas & Surfaces
val StudioCanvasDark = Color(0xFF0C121C)
val StudioSurfaceDark = Color(0xFF151E2C)
val StudioSurfaceVariantDark = Color(0xFF1E2A3B)
val StudioGridDark = Color(0xFF243247)

// Vibrant Signature Accents (Architectural Teal + Mint + Coral)
val FlowTeal600 = Color(0xFF0D9488)
val FlowTeal500 = Color(0xFF14B8A6)
val FlowTeal400 = Color(0xFF2DD4BF)
val FlowTeal100 = Color(0xFFCCFBF1)
val FlowTeal900 = Color(0xFF134E4A)

val FlowSky500 = Color(0xFF0EA5E9)
val FlowSky100 = Color(0xFFE0F2FE)
val FlowSky900 = Color(0xFF0C4A6E)

val FlowCoral500 = Color(0xFFF43F5E)
val FlowCoral100 = Color(0xFFFFE4E6)
val FlowAmber500 = Color(0xFFF59E0B)
val FlowAmber100 = Color(0xFFFEF3C7)
val FlowEmerald500 = Color(0xFF10B981)

// Signature Node Palette (inspired by crisp pastel + vivid studio swatches)
object NodeSwatches {
    val PastelColors = listOf(
        "#BEF6AC", // Mint Start
        "#A3E9CC", // Seafoam Decision
        "#F1F0D4", // Warm Cream Process
        "#D4EFF0", // Ice Cyan IO
        "#FA93B2", // Coral End
        "#E5D9FF", // Soft Lavender
        "#FFE4BA", // Soft Peach Amber
        "#FFFFFF", // Clean White
    )

    val VividColors = listOf(
        "#22C55E", // Vivid Green
        "#0EA5E9", // Vivid Sky
        "#F59E0B", // Vivid Amber
        "#F43F5E", // Vivid Rose
        "#8B5CF6", // Vivid Violet
        "#EC4899", // Vivid Pink
        "#14B8A6", // Vivid Teal
        "#1E293B", // Deep Slate
    )

    val BorderColors = listOf(
        "#161D29", // Ink Slate
        "#0D9488", // Teal
        "#0284C7", // Sky
        "#D97706", // Amber
        "#E11D48", // Rose
        "#7C3AED", // Violet
        "#64748B", // Muted Gray
        "#FFFFFF", // White (great in dark mode)
    )

    val TextColors = listOf(
        "#0F172A", // Deep Ink
        "#FFFFFF", // Crisp White
        "#0F766E", // Dark Teal
        "#1E40AF", // Royal Blue
        "#9F1239", // Crimson
        "#475569", // Slate
    )
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFFFFFFFF)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val colorLong = when (cleaned.length) {
            6 -> ("FF$cleaned").toLong(16)
            8 -> cleaned.toLong(16)
            else -> return fallback
        }
        Color(colorLong)
    } catch (_: Exception) {
        fallback
    }
}
