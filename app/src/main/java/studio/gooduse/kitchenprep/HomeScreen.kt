package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import studio.gooduse.kitchenprep.data.*
import java.util.Locale

@Composable
fun HomeScreen(
    boards: List<BoardEntity>,
    selectedBoard: BoardEntity?,
    tasks: List<TaskEntity>,
    profile: KitchenWindowProfile,
    tr: Translate,
    onContinue: () -> Unit,
    onLane: (LiveLane) -> Unit,
    onRepeat: () -> Unit,
    onNew: () -> Unit,
    onPaste: () -> Unit,
    onAll: () -> Unit,
    onOpenBoard: (String) -> Unit,
) {
    val contentModifier = centeredContentModifier(profile)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 12.dp else 20.dp)
        .padding(bottom = 24.dp)

    if (profile.homeTwoPane) {
        Row(
            modifier = contentModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1.15f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HomeActiveBlock(selectedBoard, tasks, profile, tr, onContinue, onLane)
                HeroBoard(selectedBoard, profile, tr)
            }
            Column(
                modifier = Modifier.weight(0.85f),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                StartBlock(tr, onRepeat, onNew, onPaste)
                RecentBlock(boards, tr, onAll, onOpenBoard)
            }
        }
    } else {
        Column(
            modifier = contentModifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeActiveBlock(selectedBoard, tasks, profile, tr, onContinue, onLane)
            HeroBoard(selectedBoard, profile, tr)
            StartBlock(tr, onRepeat, onNew, onPaste)
            RecentBlock(boards, tr, onAll, onOpenBoard)
        }
    }
}

@Composable
fun HomeActiveBlock(
    board: BoardEntity?,
    tasks: List<TaskEntity>,
    profile: KitchenWindowProfile,
    tr: Translate,
    onContinue: () -> Unit,
    onLane: (LiveLane) -> Unit,
) {
    SectionHeader(
        title = tr("active", "Active"),
        trailing = if (board != null) tr("onTrack", "On track") else null,
    )
    WorkbenchCard {
        if (board == null) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    tr("new", "New"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    tr("start", "Start"),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    tr("boardsStay", "Boards stay on this device"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@WorkbenchCard
        }

        val done = tasks.count { it.lane == LiveLane.DONE.name }
        val progress = if (tasks.isEmpty()) 0 else (done * 100 / tasks.size)
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        board.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        board.area,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                    )
                    val time = board.targetMinutesOfDay?.let(::formatMinutesOfDay).orEmpty()
                    val meta = listOfNotNull(
                        time.takeIf { it.isNotBlank() },
                        "${tasks.size} ${tr("tasks", "Tasks")}",
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ProgressRing(progress)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LiveLane.entries.forEach { lane ->
                    val count = tasks.count { it.lane == lane.name }
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        lane = lane,
                        count = count,
                        label = laneLabel(lane, tr),
                        onClick = { onLane(lane) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = tr("continue", "Continue"),
                icon = Icons.Default.PlayArrow,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                minHeight = if (profile.compactHeight) 52.dp else 58.dp,
            )
        }
    }
}

@Composable
fun HeroBoard(
    board: BoardEntity?,
    profile: KitchenWindowProfile,
    tr: Translate,
) {
    WorkbenchCard(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (profile.heroSideBySide) {
            Row(Modifier.height(profile.heroHeight)) {
                HeroCopy(
                    board = board,
                    profile = profile,
                    tr = tr,
                    modifier = Modifier.weight(1.02f).fillMaxHeight(),
                )
                BoardArt(Modifier.weight(0.98f).fillMaxHeight())
            }
        } else {
            Column {
                BoardArt(Modifier.fillMaxWidth().height(170.dp))
                HeroCopy(
                    board = board,
                    profile = profile,
                    tr = tr,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun HeroCopy(
    board: BoardEntity?,
    profile: KitchenWindowProfile,
    tr: Translate,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(if (profile.width >= 600.dp) 28.dp else 18.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            tr("today", "Today").uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            board?.name ?: tr("new", "New"),
            fontSize = profile.heroTitleSize,
            lineHeight = profile.heroTitleSize,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Serif,
            maxLines = 2,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (board != null) {
                listOfNotNull(
                    board.area.takeIf { it.isNotBlank() },
                    board.targetMinutesOfDay?.let(::formatMinutesOfDay),
                ).joinToString(" · ")
            } else {
                tr("boardsStay", "Boards stay on this device")
            },
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BoardArt(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == KitchenColors.DarkCanvas
    Canvas(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = if (dark) {
                    listOf(Color(0xFF353A2D), Color(0xFF273425))
                } else {
                    listOf(Color(0xFFDAD0BD), Color(0xFFB8B594))
                }
            )
        )
    ) {
        val w = size.width
        val h = size.height
        drawOval(
            color = Color(0xFF5D6941).copy(alpha = 0.48f),
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.35f),
        )
        drawOval(
            color = Color(0xFFC7653A).copy(alpha = 0.86f),
            topLeft = Offset(w * 0.48f, h * 0.22f),
            size = Size(w * 0.22f, h * 0.25f),
        )
        drawOval(
            color = Color(0xFFD1A044).copy(alpha = 0.9f),
            topLeft = Offset(w * 0.66f, h * 0.48f),
            size = Size(w * 0.15f, h * 0.18f),
        )
        drawOval(
            color = Color(0xFF7F9655),
            topLeft = Offset(w * 0.28f, h * 0.50f),
            size = Size(w * 0.30f, h * 0.25f),
        )
        drawRoundRect(
            color = Color(0xFFB98E60).copy(alpha = 0.72f),
            topLeft = Offset(w * 0.09f, h * 0.72f),
            size = Size(w * 0.42f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
        )
    }
}

@Composable
fun ProgressRing(progress: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(66.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = MaterialTheme.colorScheme.surfaceVariant,
                style = Stroke(width = 7.dp.toPx()),
            )
            drawArc(
                color = MaterialTheme.colorScheme.primary,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0, 100) * 3.6f,
                useCenter = false,
                style = Stroke(width = 7.dp.toPx()),
            )
        }
        Text("$progress%", fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StatusTile(
    modifier: Modifier,
    lane: LiveLane,
    count: Int,
    label: String,
    onClick: () -> Unit,
) {
    val (background, foreground) = laneColors(lane)
    Surface(
        modifier = modifier.heightIn(min = 58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = background,
        contentColor = foreground,
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

@Composable
fun StartBlock(
    tr: Translate,
    onRepeat: () -> Unit,
    onNew: () -> Unit,
    onPaste: () -> Unit,
) {
    SectionHeader(tr("start", "Start"))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StartCard(
            modifier = Modifier.weight(1f),
            label = tr("repeat", "Repeat"),
            icon = Icons.Default.Refresh,
            emphasized = true,
            onClick = onRepeat,
        )
        StartCard(
            modifier = Modifier.weight(1f),
            label = tr("new", "New"),
            icon = Icons.Default.Add,
            onClick = onNew,
        )
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onPaste,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(17.dp),
    ) {
        Icon(Icons.Default.ContentPaste, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(tr("paste", "Paste"), fontWeight = FontWeight.Black)
    }
}

@Composable
fun StartCard(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = 86.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        ),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun RecentBlock(
    boards: List<BoardEntity>,
    tr: Translate,
    onAll: () -> Unit,
    onOpen: (String) -> Unit,
) {
    SectionHeader(
        title = tr("recent", "Recent"),
        action = tr("all", "All"),
        onAction = onAll,
    )
    WorkbenchCard {
        if (boards.isEmpty()) {
            Text(
                tr("boardsStay", "Boards stay on this device"),
                modifier = Modifier.padding(18.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            boards.take(3).forEachIndexed { index, board ->
                BoardListRow(board = board, tr = tr, onClick = { onOpen(board.id) })
                if (index < minOf(boards.size, 3) - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                }
            }
        }
    }
}
