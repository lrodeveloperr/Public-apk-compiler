package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import studio.gooduse.kitchenprep.data.*
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
    if (board == null) {
        Box(
            modifier = centeredContentModifier(profile)
                .fillMaxSize()
                .padding(profile.gutter),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("live", "Live"), fontSize = profile.pageTitleSize, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("boardsStay", "Boards stay on this device"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val haptic = LocalHapticFeedback.current
    val tactile: (() -> Unit) = {
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var doneExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(board.id) {
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

    val waiting = tasks.filter { it.lane == LiveLane.WAITING.name }
    val allDone = tasks.isNotEmpty() && tasks.all { it.lane == LiveLane.DONE.name }

    Box(modifier = centeredContentModifier(profile).fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LiveHeader(
                board = board,
                tr = tr,
                onPause = onPause,
            )
            if (waiting.isNotEmpty()) {
                WaitingAlert(
                    count = waiting.size,
                    tr = tr,
                    onClick = { onLane(LiveLane.WAITING) },
                )
            }

            if (!profile.wideLive) {
                LiveLaneTabs(tasks, lane, tr, onLane)
                val laneTasks = tasks.filter { it.lane == lane.name }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (profile.compactHeight) 10.dp else 12.dp,
                            vertical = 10.dp,
                        )
                        .padding(bottom = if (undoAvailable) 92.dp else 20.dp),
                ) {
                    LaneColumn(
                        lane = lane,
                        tasks = laneTasks,
                        now = now,
                        compact = settings.compactLive || profile.compactHeight,
                        tr = tr,
                        doneExpanded = doneExpanded,
                        allDone = allDone,
                        onDoneExpanded = { doneExpanded = !doneExpanded },
                        onToggleTimer = { tactile(); onToggleTimer(it) },
                        onDone = { tactile(); onDone(it) },
                        onCheck = { tactile(); onCheck(it) },
                        onNow = { tactile(); onNow(it) },
                        onPriority = { tactile(); onPriority(it) },
                        onRepeat = onRepeat,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LiveLane.entries.forEach { currentLane ->
                        LaneColumn(
                            lane = currentLane,
                            tasks = tasks.filter { it.lane == currentLane.name },
                            now = now,
                            compact = true,
                            tr = tr,
                            doneExpanded = doneExpanded,
                            allDone = allDone,
                            onDoneExpanded = { doneExpanded = !doneExpanded },
                            onToggleTimer = { tactile(); onToggleTimer(it) },
                            onDone = { tactile(); onDone(it) },
                            onCheck = { tactile(); onCheck(it) },
                            onNow = { tactile(); onNow(it) },
                            onPriority = { tactile(); onPriority(it) },
                            onRepeat = onRepeat,
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = if (undoAvailable) 92.dp else 0.dp),
                        )
                    }
                }
            }
        }

        if (undoAvailable) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFF22271F),
                contentColor = Color.White,
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("done", "Done"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(18.dp))
                    TextButton(onClick = onUndo) {
                        Text(
                            tr("undo", "Undo"),
                            color = Color(0xFFDCE4C4),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveHeader(
    board: BoardEntity,
    tr: Translate,
    onPause: (Boolean) -> Unit,
) {
    Surface(
        color = Color(0xFF222720),
        contentColor = Color.White,
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(board.name, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text(board.area, fontSize = 11.sp, color = Color(0xFFBFC2B9), maxLines = 2)
            }
            OutlinedButton(
                onClick = { onPause(board.status != "PAUSED") },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(
                    if (board.status == "PAUSED") Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (board.status == "PAUSED") tr("resume", "Resume") else tr("pause", "Pause"),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun WaitingAlert(count: Int, tr: Translate, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(19.dp),
        color = KitchenColors.Terra,
        contentColor = Color.White,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$count · ${tr("wait", "Waiting")}",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text(tr("review", "Review"), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LiveLaneTabs(
    tasks: List<TaskEntity>,
    selected: LiveLane,
    tr: Translate,
    onLane: (LiveLane) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 3.dp,
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LiveLane.entries.forEach { lane ->
                val count = tasks.count { it.lane == lane.name }
                val (bg, fg) = laneColors(lane)
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable { onLane(lane) },
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected == lane) bg else Color.Transparent,
                    contentColor = if (selected == lane) fg else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            laneLabel(lane, tr),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$count", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun LaneColumn(
    lane: LiveLane,
    tasks: List<TaskEntity>,
    now: Long,
    compact: Boolean,
    tr: Translate,
    doneExpanded: Boolean,
    allDone: Boolean,
    onDoneExpanded: () -> Unit,
    onToggleTimer: (TaskEntity) -> Unit,
    onDone: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
    onNow: (TaskEntity) -> Unit,
    onPriority: (TaskEntity) -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${laneLabel(lane, tr)} · ${tasks.size}",
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = laneColors(lane).second,
            )
            if (lane == LiveLane.DONE) {
                TextButton(onClick = onDoneExpanded) {
                    Text(if (doneExpanded) tr("hide", "Hide") else tr("show", "Show"), fontSize = 10.sp)
                }
            }
        }

        if (lane == LiveLane.DONE && allDone) {
            PrimaryButton(
                text = tr("repeat", "Repeat"),
                icon = Icons.Default.Refresh,
                onClick = onRepeat,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 48.dp,
            )
        }

        val visibleTasks = if (lane == LiveLane.DONE && !doneExpanded) emptyList() else tasks
        if (visibleTasks.isEmpty() && lane != LiveLane.DONE) {
            Text(
                "—",
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        visibleTasks.forEach { task ->
            LiveTaskCard(
                task = task,
                now = now,
                compact = compact,
                tr = tr,
                onToggleTimer = { onToggleTimer(task) },
                onDone = { onDone(task) },
                onCheck = { onCheck(task) },
                onNow = { onNow(task) },
                onPriority = { onPriority(task) },
            )
        }
    }
}

@Composable
fun LiveTaskCard(
    task: TaskEntity,
    now: Long,
    compact: Boolean,
    tr: Translate,
    onToggleTimer: () -> Unit,
    onDone: () -> Unit,
    onCheck: () -> Unit,
    onNow: () -> Unit,
    onPriority: () -> Unit,
) {
    val expired = task.timerRunning && (task.timerDeadlineAt ?: Long.MAX_VALUE) <= now
    val remaining = when {
        task.timerRunning -> max(0L, ((task.timerDeadlineAt ?: now) - now + 999L) / 1000L)
        else -> task.remainingSeconds
    }
    WorkbenchCard(
        shape = RoundedCornerShape(if (compact) 14.dp else 17.dp),
        borderColor = if (expired) KitchenColors.Terra else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    ) {
        Column(Modifier.padding(if (compact) 8.dp else 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        task.name,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    val quantities = listOfNotNull(
                        task.have.takeIf { it.isNotBlank() }?.let { "${tr("have", "Have")} $it" },
                        task.need.takeIf { it.isNotBlank() }?.let { "${tr("need", "Need")} $it" },
                        task.prep.takeIf { it.isNotBlank() }?.let { "${tr("prep", "Prep")} $it" },
                    )
                    if (quantities.isNotEmpty()) {
                        Text(
                            quantities.joinToString(" · "),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp,
                        )
                    }
                }

                when (task.lane) {
                    LiveLane.NOW.name, LiveLane.WAITING.name -> {
                        if (task.durationSeconds > 0L) {
                            TimerChip(
                                label = if (expired) {
                                    tr("attentionRequired", "Attention required")
                                } else {
                                    formatDuration(remaining)
                                },
                                waiting = task.lane == LiveLane.WAITING.name,
                                expired = expired,
                                onClick = onToggleTimer,
                            )
                        }
                    }
                    LiveLane.NEXT.name -> {
                        if (task.durationSeconds > 0) {
                            StatusBadge(text = "${task.durationSeconds / 60} min")
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (task.lane) {
                    LiveLane.NOW.name -> {
                        SmallActionButton(tr("done", "Done"), onDone, Modifier.weight(1f))
                    }
                    LiveLane.WAITING.name -> {
                        SmallActionButton(tr("check", "Check"), onCheck, Modifier.weight(1f), warn = true)
                    }
                    LiveLane.NEXT.name -> {
                        SmallActionButton(tr("now", "Now"), onNow, Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onPriority,
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = tr("priority", "Priority"),
                                tint = if (task.priority) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    LiveLane.DONE.name -> {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun TimerChip(
    label: String,
    waiting: Boolean,
    expired: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        expired -> KitchenColors.TerraSoft
        waiting -> KitchenColors.AmberSoft
        else -> KitchenColors.OliveSoft
    }
    val foreground = when {
        expired -> KitchenColors.TerraDeep
        waiting -> Color(0xFF84580D)
        else -> KitchenColors.OliveDeep
    }
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(max = 152.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background,
        contentColor = foreground,
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontSize = if (expired) 10.sp else 11.sp, fontWeight = FontWeight.Black, maxLines = 2)
        }
    }
}

@Composable
fun SmallActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    warn: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = if (warn) {
            ButtonDefaults.buttonColors(
                containerColor = KitchenColors.AmberSoft,
                contentColor = Color(0xFF855A10),
            )
        } else ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 10.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 2)
    }
}
