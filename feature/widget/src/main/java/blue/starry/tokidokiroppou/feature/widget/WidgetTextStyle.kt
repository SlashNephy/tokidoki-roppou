package blue.starry.tokidokiroppou.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.TextDefaults

object WidgetTextStyle {
  val Overline
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(
      color = GlanceTheme.colors.onSurfaceVariant,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
    )

  val Headline
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(
      color = GlanceTheme.colors.onSurface,
      fontSize = 14.sp,
    )

  val Supporting
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(
      color = GlanceTheme.colors.onSurfaceVariant,
      fontSize = 12.sp,
    )
}
