package com.example.aiaccounting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aiaccounting.ui.theme.Elevation
import com.example.aiaccounting.ui.theme.GlassTokens
import com.example.aiaccounting.ui.theme.Shapes

/**
 * Glass-effect card for dreamy themes (Fresh Sci-Fi / New Year Horse).
 * Uses semi-transparent backgrounds with subtle borders.
 * Elevation defaults to Elevation.none for flat glass appearance.
 */
@Composable
fun GlassCard(
	modifier: Modifier = Modifier,
	shapeDp: Dp = 0.dp,
	contentPadding: PaddingValues = PaddingValues(16.dp),
	elevation: Dp = Elevation.none,
	content: @Composable ColumnScope.() -> Unit
) {
	val shape = if (shapeDp.value > 0f) {
		androidx.compose.foundation.shape.RoundedCornerShape(shapeDp)
	} else {
		Shapes.card
	}

	val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GlassTokens.containerAlpha)
	val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = GlassTokens.borderAlpha)

	Card(
		modifier = modifier
			.clip(shape)
			.border(BorderStroke(GlassTokens.borderWidth, borderColor), shape),
		shape = shape,
		colors = CardDefaults.cardColors(containerColor = containerColor),
		elevation = CardDefaults.cardElevation(defaultElevation = elevation)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(contentPadding),
			content = content
		)
	}
}
