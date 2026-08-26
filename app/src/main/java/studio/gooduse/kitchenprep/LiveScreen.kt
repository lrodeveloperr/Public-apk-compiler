package studio.gooduse.kitchenprep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import studio.gooduse.kitchenprep.data.BoardEntity
import studio.gooduse.kitchenprep.data.SettingsState
import studio.gooduse.kitchenprep.data.TaskEntity
import kotlin.math.max

@Composable
fun LiveScreen(
    board: BoardEntity?,
    tasks: List<TaskEntity>,
    lane: LiveLane,
    profile: KitchenWindowProfile,
    settings: SettingsState,
    tr: Translate,
    undoAvailable: Boolean,
    onLane: (LiveLane) -> Unit,
    onPause: (Boolean) -> Unit,
    onToggleTimer: (TaskEntity) -> Unit,
    onDone: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
    onNow: (TaskEntity) -> Unit,
    onPriority: (TaskEntity) -> Unit,
    onUndo: () -> Unit,
    onClearUndo: () -> Unit,
    onRepeat: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val tactile: (() -> Unit) = {
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(board?.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    LaunchedEffect(undoAvailable) {
        if (undoAvailable) {
            delay(5_000L)
            onClearUndo()
        }
    }

    val page = centeredContentModifier(profile)
        .fillMaxSize()
        .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 10.dp else 18.dp)

    if (board == null) {
        Column(
            modifier = page.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PageTitle(tr("live", "Live"), profile)
            WorkbenchCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("—", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        tr("boards", "No active board"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    Box(modifier = page) {
        Column(Modifier.fillMaxSize()) {
            PageTitle(tr("live", "Live"), profile)
            Spacer(Modifier.height(14.dp))

            LiveBoardHeader(
                board = board,
                tr = tr,
                onPause = onPause,
            )
            Spacer(Modifier.height(12.dp))

            if (profile.wideLive) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LiveLane.entries.forEach { currentLane ->
                        LaneColumnFrozen(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            lane = currentLane,
                            tasks = tasks.filter { it.lane == currentLane.name },
                            now = now,
                            compact = true,
                            tr = tr,
                            tactile = tactile,
                            onToggleTimer = onToggleTimer,
                            onDone = onDone,
                            onCheck = onCheck,
                            onNow = onNow,
                        )
                    }
                }
            } else {
                LiveLaneTabsFrozen(tasks, lane, tr, onLane)
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = if (undoAvailable) 88.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LaneColumnFrozen(
                        lane = lane,
                        tasks = tasks.filter { it.lane == lane.name },
                        now = now,
                        compact = settings.compactLive || profile.compactHeight,
                        tr = tr,
                        tactile = tactile,
                        onToggleTimer = onToggleTimer,
                        onDone = onDone,
                        onCheck = onCheck,
                        onNow = onNow,
                    )
                }
            }
        }

        if (undoAvailable) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("done", "Done"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onUndo) {
                        Text(tr("undo", "Undo"), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBoardHeader(
    board: BoardEntity,
    tr: Translate,
    onPause: (Boolean) -> Unit,
) {
    WorkbenchCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tr("active", "Active").uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    board.name,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                )
                if (board.area.isNotBlank()) {
                    Text(
                        board.area,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(
                    text = if (board.status == "PAUSED") tr("pause", "Paused") else tr("active", "Running"),
                )
                TextButton(onClick = { onPause(board.status != "PAUSED") }) {
                    Text(
                        if (board.status == "PAUSED") tr("resume", "Resume") else tr("pause", "Pause"),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveLaneTabsFrozen(
    tasks: List<TaskEntity>,
    selected: LiveLane,
    tr: Translate,
    onLane: (LiveLane) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LiveLane.entries.forEach { current ->
                val count = tasks.count { it.lane == current.name }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clickable { onLane(current) },
                    shape = RoundedCornerShape(15.dp),
                    color = if (selected == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected == current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(laneLabel(current, tr), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Spacer(Modifier.width(4.dp))
                        Text(count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun LaneColumnFrozen(
    lane: LiveLane,
    tasks: List<TaskEntity>,
    now: Long,
    compact: Boolean,
    tr: Translate,
    tactile: () -> Unit,
    onToggleTimer: (TaskEntity) -> Unit,
    onDone: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
    onNow: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        if (tasks.isEmpty()) {
            WorkbenchCard(Modifier.fillMaxWidth()) {
                Text(
                    "—",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        tasks.forEach { task ->
            FrozenTaskCard(
                task = task,
                lane = lane,
                now = now,
                compact = compact,
                tr = tr,
                onToggleTimer = { tactile(); onToggleTimer(task) },
                onDone = { tactile(); onDone(task) },
                onCheck = { tactile(); onCheck(task) },
                onNow = { tactile(); onNow(task) },
            )
        }
    }
}

@Composable
private fun FrozenTaskCard(
    task: TaskEntity,
    lane: LiveLane,
    now: Long,
    compact: Boolean,
    tr: Translate,
    onToggleTimer: () -> Unit,
    onDone: () -> Unit,
    onCheck: () -> Unit,
    onNow: () -> Unit,
) {
    val remaining = when {
        task.timerRunning -> max(0L, ((task.timerDeadlineAt ?: now) - now + 999L) / 1000L)
        else -> task.remainingSeconds
    }

    WorkbenchCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        task.name,
                        fontSize = if (compact) 15.sp else 17.sp,
                        lineHeight = if (compact) 19.sp else 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                    )
                    val detail = listOfNotNull(
                        task.need.takeIf { it.isNotBlank() },
                        task.prep.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                when (lane) {
                    LiveLane.NOW, LiveLane.WAITING -> {
                        if (task.durationSeconds > 0L) {
                            Text(
                                formatDuration(remaining),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (lane == LiveLane.WAITING) KitchenColors.Terra else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    else -> Unit
                }
            }

            when (lane) {
                LiveLane.NOW -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onToggleTimer,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(if (task.timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (task.timerRunning) tr("pause", "Pause") else tr("start", "Start"), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDone,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(tr("done", "Done"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                LiveLane.WAITING -> {
                    OutlinedButton(
                        onClick = onCheck,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(tr("check", "Check"), fontWeight = FontWeight.Bold)
                    }
                }
                LiveLane.NEXT -> {
                    OutlinedButton(
                        onClick = onNow,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(tr("now", "Start"), fontWeight = FontWeight.Bold)
                    }
                }
                LiveLane.DONE -> {
                    StatusBadge(tr("done", "Done"))
                }
            }
        }
    }
}
