package com.example.aiaccounting.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Draws dreamy gradients / glow spots behind the entire app.
 * Only enabled for Fresh Sci-Fi and New Year Horse themes.
 *
 * Gradient offset values are carefully tuned for visual balance:
 * - Primary glow (center): Offset(0.20f, 0.18f) positions the main highlight
 *   in the upper-left to create depth and draw the eye naturally.
 * - Secondary glow (center): Offset(0.85f, 0.42f) adds a complementary
 *   highlight in the lower-right for balanced composition.
 * - Radii (0.95f, 0.85f, 0.9f) control glow spread for soft, diffuse edges.
 */
@Composable
fun AppThemeBackground(
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	val themeId = LocalAppThemeId.current

	val backgroundBrush = when (themeId) {
		AppThemeIds.FRESH_SCI -> Brush.verticalGradient(
			colors = listOf(
				Color(0xFFEAF6FF),
				Color(0xFFF5FBFF),
				Color(0xFFFFFFFF)
			)
		)

		else -> null
	}

	val glowBrush = when (themeId) {
		AppThemeIds.FRESH_SCI -> Brush.radialGradient(
			colors = listOf(
				Color(0xFFBFE7FF).copy(alpha = 0.45f),
				Color(0xFFBFE7FF).copy(alpha = 0.0f)
			),
			// Primary highlight: upper-left for natural focal point
			center = Offset(0.20f, 0.18f),
			radius = 0.95f
		)

		else -> null
	}

	val glowBrush2 = when (themeId) {
		AppThemeIds.FRESH_SCI -> Brush.radialGradient(
			colors = listOf(
				Color(0xFF86FFF2).copy(alpha = 0.22f),
				Color(0xFF86FFF2).copy(alpha = 0.0f)
			),
			// Secondary glow: lower-right for visual balance
			center = Offset(0.85f, 0.42f),
			radius = 0.85f
		)

		else -> null
	}

	Box(modifier = modifier.fillMaxSize()) {
		if (backgroundBrush != null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(backgroundBrush)
			)
			if (glowBrush != null) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(glowBrush)
				)
			}
			if (glowBrush2 != null) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(glowBrush2)
				)
			}
		}

		content()
	}
}
