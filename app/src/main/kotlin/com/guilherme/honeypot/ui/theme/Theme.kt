package com.guilherme.honeypot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = InterFamily),
        displayMedium = displayMedium.copy(fontFamily = InterFamily),
        displaySmall = displaySmall.copy(fontFamily = InterFamily),
        headlineLarge = headlineLarge.copy(fontFamily = InterFamily),
        headlineMedium = headlineMedium.copy(fontFamily = InterFamily),
        headlineSmall = headlineSmall.copy(fontFamily = InterFamily),
        titleLarge = titleLarge.copy(fontFamily = InterFamily),
        titleMedium = titleMedium.copy(fontFamily = InterFamily),
        titleSmall = titleSmall.copy(fontFamily = InterFamily),
        bodyLarge = bodyLarge.copy(fontFamily = InterFamily),
        bodyMedium = bodyMedium.copy(fontFamily = InterFamily),
        bodySmall = bodySmall.copy(fontFamily = InterFamily),
        labelLarge = labelLarge.copy(fontFamily = InterFamily),
        labelMedium = labelMedium.copy(fontFamily = InterFamily),
        labelSmall = labelSmall.copy(fontFamily = InterFamily)
    )
}

@Composable
fun HoneypotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = AppTypography,
        content = content
    )
}
