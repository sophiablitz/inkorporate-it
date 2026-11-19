package com.example.inkorporate_it.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.inkorporate_it.R

// Sniglet Regular (400), bundled in res/font so it works offline and on phones
// without Google Play Services (issue #15). License: assets/licenses/Sniglet-OFL.txt.
// Only Regular is in this family, so bold text is drawn as "fake bold" from Regular.
val bodyFontFamily = FontFamily(
    Font(R.font.sniglet_regular)
)

val displayFontFamily = bodyFontFamily

// Sniglet ExtraBold (800), for the ".it" in the Home title.
// Its own family on purpose: if it were in the family above, every fake-bold
// (SemiBold) text would switch to this much heavier font.
val extraBoldFontFamily = FontFamily(
    Font(R.font.sniglet_extrabold, FontWeight.ExtraBold)
)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)
