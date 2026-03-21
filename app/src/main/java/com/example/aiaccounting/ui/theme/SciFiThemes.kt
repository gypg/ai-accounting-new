package com.example.aiaccounting.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Sci-Fi themed ColorSchemes derived from Doubao HTML prototype.
 */

// ==================== 浅色科幻清新 (Fresh Sci-Fi Light) ====================
val FreshSciColorScheme = lightColorScheme(
    primary = Color(0xFF4A89DC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9FF),
    onPrimaryContainer = Color(0xFF0E2A4D),

    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF3FF),
    onSecondaryContainer = Color(0xFF0B2A4D),

    tertiary = Color(0xFF00B3D6),
    onTertiary = Color(0xFF001F26),
    tertiaryContainer = Color(0xFFC9F3FF),
    onTertiaryContainer = Color(0xFF003643),

    background = Color.Transparent,
    onBackground = Color(0xFF0F172A),

    // 半透明表面：让全局渐变背景“透出来”（玻璃感）
    surface = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEAF2FF).copy(alpha = 0.72f),
    onSurfaceVariant = Color(0xFF334155),

    outline = Color(0xFFB7D3F2),
    outlineVariant = Color(0xFFD2E6FF),

    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

// ==================== 新马年科幻节日 (Horse Sci-Fi Festival, Light) ====================
val NewYearHorseSciColorScheme = lightColorScheme(
    primary = Color(0xFFD64040),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = Color(0xFF4B0F12),

    secondary = Color(0xFFE6C278), // gold accent
    onSecondary = Color(0xFF2A1E00),
    secondaryContainer = Color(0xFFFFEFD1),
    onSecondaryContainer = Color(0xFF3D2C00),

    tertiary = Color(0xFFB86BFF), // soft sci-fi purple
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0DDFF),
    onTertiaryContainer = Color(0xFF2F0053),

    background = Color.Transparent,
    onBackground = Color(0xFF1F1020),

    // 半透明表面：让全局渐变背景“透出来”（梦幻玻璃感）
    surface = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    onSurface = Color(0xFF1F1020),
    surfaceVariant = Color(0xFFFFF3F6).copy(alpha = 0.72f),
    onSurfaceVariant = Color(0xFF4A3A4C),

    outline = Color(0xFFE7C6D9),
    outlineVariant = Color(0xFFF3DCE8),

    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)
