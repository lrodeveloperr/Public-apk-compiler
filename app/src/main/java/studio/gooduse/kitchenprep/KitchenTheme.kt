package studio.gooduse.kitchenprep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KitchenColors {
    val Canvas = Color(0xFFF6F9F7)
    val Canvas2 = Color(0xFFE7ECE9)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF0F4F2)
    val Ink = Color(0xFF17201C)
    val Ink2 = Color(0xFF4B5551)
    val Muted = Color(0xFF68716D)
    val Line = Color(0xFFD9DFDC)
    val Olive = Color(0xFF176B4D)
    val OliveDeep = Color(0xFF123D30)
    val OliveSoft = Color(0xFFDDF1E7)
    val Sage = Color(0xFF78A890)
    val Terra = Color(0xFF963A3F)
    val TerraDeep = Color(0xFF74282D)
    val TerraSoft = Color(0xFFF7E3E4)
    val Amber = Color(0xFF805B00)
    val AmberSoft = Color(0xFFFFF0C4)
    val Teal = Color(0xFF315FAE)
    val TealSoft = Color(0xFFEDF3FF)
    val Success = Color(0xFF176B4D)

}

private val LightScheme = lightColorScheme(
    primary = KitchenColors.Olive,
    onPrimary = Color.White,
    primaryContainer = KitchenColors.OliveSoft,
    onPrimaryContainer = KitchenColors.OliveDeep,
    secondary = KitchenColors.Teal,
    onSecondary = Color.White,
    secondaryContainer = KitchenColors.TealSoft,
    tertiary = KitchenColors.Terra,
    error = KitchenColors.TerraDeep,
    background = KitchenColors.Canvas,
    onBackground = KitchenColors.Ink,
    surface = KitchenColors.Surface,
    onSurface = KitchenColors.Ink,
    surfaceVariant = KitchenColors.SurfaceSoft,
    onSurfaceVariant = KitchenColors.Ink2,
    outline = KitchenColors.Line,
)

private val KitchenTypography = Typography()

@Composable
fun KitchenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = KitchenTypography,
        content = content,
    )
}

data class KitchenWindowProfile(
    val width: Dp,
    val height: Dp,
    val gutter: Dp,
    val contentMaxWidth: Dp,
    val useRail: Boolean,
    val homeTwoPane: Boolean,
    val heroSideBySide: Boolean,
    val wideLive: Boolean,
    val compactHeight: Boolean,
    val largeText: Boolean,
    val topBarHeight: Dp,
    val heroHeight: Dp,
    val heroTitleSize: TextUnit,
    val pageTitleSize: TextUnit,
    val touchTarget: Dp,
)

fun kitchenWindowProfile(width: Dp, height: Dp, fontScale: Float = 1f): KitchenWindowProfile {
    val compactHeight = height < 480.dp
    val largeText = fontScale >= 1.30f
    // Accessibility reflow wins over width. A large-font tablet should not squeeze
    // four lanes or a navigation rail merely because it has expanded width.
    val useRail = width >= 840.dp && !compactHeight && !largeText
    val gutter = when {
        width < 360.dp -> 12.dp
        width < 600.dp -> 16.dp
        width < 840.dp -> 24.dp
        width < 1200.dp -> 32.dp
        width < 1600.dp -> 40.dp
        else -> 48.dp
    }
    val maxWidth = if (width >= 1200.dp) 1440.dp else 0.dp
    val heroTitle = when {
        compactHeight -> 29.sp
        width >= 1200.dp && !compactHeight -> 36.sp
        width >= 840.dp && !compactHeight -> 34.sp
        width >= 600.dp -> 32.sp
        else -> 30.sp
    }
    val heroHeight = when {
        compactHeight -> 230.dp
        width >= 840.dp -> 318.dp
        width >= 600.dp -> 330.dp
        else -> 300.dp
    }
    return KitchenWindowProfile(
        width = width,
        height = height,
        gutter = gutter,
        contentMaxWidth = maxWidth,
        useRail = useRail,
        homeTwoPane = useRail,
        heroSideBySide = width >= 600.dp && !largeText,
        wideLive = useRail,
        compactHeight = compactHeight,
        largeText = largeText,
        topBarHeight = if (compactHeight) 56.dp else 64.dp,
        heroHeight = heroHeight,
        heroTitleSize = heroTitle,
        pageTitleSize = if (width >= 840.dp && !compactHeight) 28.sp else 25.sp,
        touchTarget = 52.dp,
    )
}
