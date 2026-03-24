package blue.starry.tokidokiroppou

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C5E4),
    secondary = Color(0xFFBCC7DC),
    tertiary = Color(0xFFD5BDE2),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF43474E),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B5998),
    secondary = Color(0xFF565E71),
    tertiary = Color(0xFF715574),
    surface = Color(0xFFF9F9FF),
    surfaceVariant = Color(0xFFE0E2EC),
)

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.increaseFontSize(),
        displayMedium = displayMedium.increaseFontSize(),
        displaySmall = displaySmall.increaseFontSize(),
        headlineLarge = headlineLarge.increaseFontSize(),
        headlineMedium = headlineMedium.increaseFontSize(),
        headlineSmall = headlineSmall.increaseFontSize(),
        titleLarge = titleLarge.increaseFontSize(),
        titleMedium = titleMedium.increaseFontSize(),
        titleSmall = titleSmall.increaseFontSize(),
        bodyLarge = bodyLarge.increaseFontSize(),
        bodyMedium = bodyMedium.increaseFontSize(),
        bodySmall = bodySmall.increaseFontSize(),
        labelLarge = labelLarge.increaseFontSize(),
        labelMedium = labelMedium.increaseFontSize(),
        labelSmall = labelSmall.increaseFontSize(),
    )
}

private const val FONT_SIZE_ADJUST = 2

private fun TextStyle.increaseFontSize(): TextStyle {
    return copy(
        fontSize = fontSize.add(FONT_SIZE_ADJUST)
    )
}

private fun TextUnit.add(other: Int): TextUnit {
    return (value+other).let {
        when (type) {
            TextUnitType.Sp -> it.sp
            TextUnitType.Em -> it.em
            else -> TextUnit(it, TextUnitType.Unspecified)
        }
    }
}

@Composable
fun TokidokiRoppouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
