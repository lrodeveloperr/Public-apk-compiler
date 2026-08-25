package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.util.Locale

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    customAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        when {
            customAction != null -> customAction()
            action != null && onAction != null -> {
                TextButton(onClick = onAction) {
                    Text(action, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
            trailing != null -> StatusBadge(trailing)
        }
    }
}

@Composable
fun StatusBadge(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
        )
    }
}

@Composable
fun PageTitle(
    title: String,
    profile: KitchenWindowProfile,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        modifier = modifier,
        fontSize = profile.pageTitleSize,
        lineHeight = profile.pageTitleSize * 1.1f,
        fontWeight = FontWeight.Black,
    )
}

@Composable
fun WorkbenchCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp,
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        shape = RoundedCornerShape(17.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
fun DividerInCard() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
}

@Composable
fun laneColors(lane: LiveLane): Pair<Color, Color> {
    val dark = MaterialTheme.colorScheme.background == KitchenColors.DarkCanvas
    return when (lane) {
        LiveLane.NOW -> if (dark) Color(0xFF303A25) to Color(0xFFD8E6B4)
        else KitchenColors.OliveSoft to KitchenColors.OliveDeep
        LiveLane.WAITING -> if (dark) Color(0xFF3C321F) to Color(0xFFF4D28F)
        else KitchenColors.AmberSoft to Color(0xFF80560E)
        LiveLane.NEXT -> if (dark) Color(0xFF203B38) to Color(0xFFB9E0DA)
        else KitchenColors.TealSoft to Color(0xFF2F5A56)
        LiveLane.DONE -> if (dark) Color(0xFF292E27) to Color(0xFFD5D7CF)
        else Color(0xFFECE9E2) to Color(0xFF595C54)
    }
}

fun laneLabel(lane: LiveLane, tr: Translate): String = when (lane) {
    LiveLane.NOW -> tr("now", "Now")
    LiveLane.WAITING -> tr("wait", "Waiting")
    LiveLane.NEXT -> tr("next", "Next")
    LiveLane.DONE -> tr("done", "Done")
}

fun centeredContentModifier(profile: KitchenWindowProfile): Modifier {
    val base = Modifier.fillMaxWidth()
    return if (profile.contentMaxWidth <= 0.dp) {
        base
    } else {
        base.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = profile.contentMaxWidth)
    }
}

fun formatMinutesOfDay(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(Locale.US, hour, minute)
}

fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60
    val secs = safe % 60
    return "%d:%02d".format(Locale.US, minutes, secs)
}
