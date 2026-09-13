package com.example.aiaccounting.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.example.aiaccounting.data.local.prefs.UiScalePreferences

/**
 * CompositionLocal for UI scale preferences.
 * Use [LocalUiScale.current] to access the current scale values in composables.
 */
val LocalUiScale = compositionLocalOf { UiScalePreferences() }
