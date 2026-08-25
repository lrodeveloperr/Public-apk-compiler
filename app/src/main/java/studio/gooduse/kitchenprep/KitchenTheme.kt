package studio.gooduse.kitchenprep

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KitchenColors {
    val Canvas = Color(0xFFF5F0E8)
    val Canvas2 = Color(0xFFEDE6DA)
    val Surface = Color(0xFFFFFDF9)
    val SurfaceSoft = Color(0xFFF2ECE2)
    val Ink = Color(0xFF1E211B)
    val Ink2 = Color(0xFF464940)
    val Muted = Color(0xFF77796F)
    val Line = Color(0xFFDED7CB)
    val Olive = Color(0xFF4D5B27)
    val OliveDeep = Color(0xFF34401B)
    val OliveSoft = Color(0xFFDCE4C4)
    val Sage = Color(0xFF8EA376)
    val Terra = Color(0xFFB95635)
    val TerraDeep = Color(0xFF8E3B27)
    val TerraSoft = Color(0xFFF2D8CE)
    val Amber = Color(0xFFC58A29)
    val AmberSoft = Color(0xFFF4E4BE)
    val Teal = Color(0xFF3F726D)
    val TealSoft = Color(0xFFD5E6E2)
    val Success = Color(0xFF577A45)

    val DarkCanvas = Color(0xFF11140F)
    val DarkSurface = Color(0xFF1C2119)
    val DarkSurfaceSoft = Color(0xFF242A20)
    val DarkInk = Color(0xFFF4F0E7)
    val DarkInk2 = Color(0xFFD2D4CB)
    val DarkMuted = Color(0xFFA3A79B)
    val DarkLine = Color(0xFF363D32)
    val DarkOlive = Color(0xFF819451)
    val DarkOliveDeep = Color(0xFF607238)
    val DarkOliveSoft = Color(0xFF303A25)
    val DarkTerra = Color(0xFFD07350)
    val DarkTerraSoft = Color(0xFF41271E)
    val DarkAmber = Color(0xFFD8A34A)
    val DarkAmberSoft = Color(0xFF3C321F)
    val DarkTeal = Color(0xFF67A29B)
    val DarkTealSoft = Color(0xFF203B38)
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

private val DarkScheme = darkColorScheme(
    primary = KitchenColors.DarkOlive,
    onPrimary = Color(0xFF11140F),
    primaryContainer = KitchenColors.DarkOliveSoft,
    onPrimaryContainer = Color(0xFFDDE7C8),
    secondary = KitchenColors.DarkTeal,
    onSecondary = Color(0xFF11140F),
    secondaryContainer = KitchenColors.DarkTealSoft,
    tertiary = KitchenColors.DarkTerra,
    error = Color(0xFFE08C8C),
    background = KitchenColors.DarkCanvas,
    onBackground = KitchenColors.DarkInk,
    surface = KitchenColors.DarkSurface,
    onSurface = KitchenColors.DarkInk,
    surfaceVariant = KitchenColors.DarkSurfaceSoft,
    onSurfaceVariant = KitchenColors.DarkInk2,
    outline = KitchenColors.DarkLine,
)

private val KitchenTypography = Typography()

@Composable
fun KitchenTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
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
    val topBarHeight: Dp,
    val heroHeight: Dp,
    val heroTitleSize: TextUnit,
    val pageTitleSize: TextUnit,
    val touchTarget: Dp,
)

fun kitchenWindowProfile(width: Dp, height: Dp): KitchenWindowProfile {
    val compactHeight = height < 480.dp
    val useRail = width >= 840.dp && !compactHeight
    val gutter = when {
        width < 360.dp -> 12.dp
        width < 600.dp -> 16.dp
        width < 840.dp -> 24.dp
        width < 1200.dp -> 32.dp
        width < 1600.dp -> 40.dp
        else -> 48.dp
    }
    val maxWidth = when {
        width >= 1200.dp -> 1440.dp
        else -> 0.dp
    }
    val heroTitle = when {
        compactHeight && width < 840.dp -> 29.sp
        width >= 1200.dp && !compactHeight -> 52.sp
        width >= 840.dp && !compactHeight -> 38.sp
        width >= 600.dp -> 36.sp
        else -> 29.sp
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
        heroSideBySide = width >= 600.dp,
        wideLive = useRail,
        compactHeight = compactHeight,
        topBarHeight = if (compactHeight) 56.dp else 64.dp,
        heroHeight = heroHeight,
        heroTitleSize = heroTitle,
        pageTitleSize = if (width >= 840.dp && !compactHeight) 32.sp else 29.sp,
        touchTarget = 52.dp,
    )
}
