
package studio.gooduse.kitchenprep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import studio.gooduse.kitchenprep.data.BoardEntity
import studio.gooduse.kitchenprep.data.TaskEntity
import java.util.Calendar
import kotlin.math.max

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
    onPauseBoard: (Boolean) -> Unit,
    onToggleTimer: (TaskEntity) -> Unit,
    onDone: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
    onNow: (TaskEntity) -> Unit,
) {
    val pageModifier = centeredContentModifier(profile)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 12.dp else 18.dp)
        .padding(bottom = 24.dp)

    if (selectedBoard == null) {
        EmptyStation(boards, profile, tr, onNew, onPaste, onAll, onOpenBoard, pageModifier)
        return
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(selectedBoard.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val activeTask = tasks.firstOrNull { it.lane == LiveLane.NOW.name }
    val nextTasks = tasks.filter { it.lane == LiveLane.NEXT.name }
        .sortedWith(compareByDescending<TaskEntity> { it.priority }.thenBy { it.sortOrder })
    val waitingTasks = tasks.filter { it.lane == LiveLane.WAITING.name }
    val prepGaps = tasks.filter {
        it.lane != LiveLane.DONE.name && (it.prep.isNotBlank() || it.need.isNotBlank())
    }

    Column(modifier = pageModifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StationTargetHeader(selectedBoard, tasks, tr)

        if (profile.homeTwoPane) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(7f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DoNowCard(
                        activeTask, selectedBoard.status == "PAUSED", nowMillis, tr,
                        onPauseBoard, onToggleTimer, onDone, onContinue,
                    )
                    SectionHeader(
                        title = tr("next", "Cooking timeline"),
                        action = tr("live", "Open live"),
                        onAction = onContinue,
                    )
                    TimelinePanel(nextTasks + waitingTasks, tr, onNow, onCheck)
                }
                Column(
                    modifier = Modifier.weight(3f).widthIn(min = 292.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RunningTimersPanel(tasks, nowMillis, tr, onToggleTimer)
                    PrepGapsPanel(prepGaps, tr, onContinue)
                    WaitingPanel(waitingTasks, tr, onCheck) { onLane(LiveLane.WAITING) }
                }
            }
        } else {
            DoNowCard(
                activeTask, selectedBoard.status == "PAUSED", nowMillis, tr,
                onPauseBoard, onToggleTimer, onDone, onContinue,
            )
            prepGaps.firstOrNull()?.let { PrepGapAlert(it, tr, onContinue) }
            SectionHeader(
                title = tr("next", "Next up"),
                action = tr("live", "Open live"),
                onAction = onContinue,
            )
            TimelinePanel(nextTasks.take(5) + waitingTasks.take(1), tr, onNow, onCheck)
            if (waitingTasks.isNotEmpty()) {
                SectionHeader(title = tr("wait", "Waiting"), trailing = waitingTasks.size.toString())
                WaitingPanel(waitingTasks, tr, onCheck) { onLane(LiveLane.WAITING) }
            }
        }

        HomeUtilities(tr, onRepeat, onNew, onPaste)
        RecentBoards(boards, selectedBoard.id, tr, onAll, onOpenBoard)
    }
}

@Composable
private fun StationTargetHeader(board: BoardEntity, tasks: List<TaskEntity>, tr: Translate) {
    val done = tasks.count { it.lane == LiveLane.DONE.name }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                timingLabel(board.timingMode, tr).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                board.targetMinutesOfDay?.let(::formatMinutesOfDay) ?: tr("now", "Now"),
                fontSize = 31.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                targetSummary(board, done, tasks.size, tr),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        StatusBadge(
            if (board.status == "PAUSED") tr("pause", "Paused") else tr("onTrack", "On track")
        )
    }
}

private fun timingLabel(mode: String, tr: Translate): String = when (mode.uppercase()) {
    "READY_BY" -> tr("readyBy", "Ready by")
    "COOK_NOW" -> tr("now", "Cook now")
    else -> tr("serveAt", "Serve at")
}

private fun targetSummary(board: BoardEntity, done: Int, total: Int, tr: Translate): String {
    val target = board.targetMinutesOfDay
        ?: return "$done / $total ${tr("done", "done")}"
    val clock = Calendar.getInstance()
    val current = clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE)
    val delta = target - current
    val timing = if (delta >= 0) {
        "$delta min ${tr("remaining", "remaining")}"
    } else {
        "${-delta} min ${tr("late", "past target")}"
    }
    return "$timing · $done / $total ${tr("done", "done")}"
}

@Composable
private fun DoNowCard(
    task: TaskEntity?,
    boardPaused: Boolean,
    nowMillis: Long,
    tr: Translate,
    onPauseBoard: (Boolean) -> Unit,
    onToggleTimer: (TaskEntity) -> Unit,
    onDone: (TaskEntity) -> Unit,
    onContinue: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = KitchenColors.OliveDeep,
        contentColor = Color.White,
    ) {
        if (task == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("now", "DO NOW"), color = KitchenColors.Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(tr("next", "Choose the next task"), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onContinue,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(tr("live", "Open live"), fontWeight = FontWeight.Bold) }
            }
            return@Surface
        }

        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(tr("now", "DO NOW"), color = KitchenColors.Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(task.name, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    taskDetail(task).takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFFD8E8E1), fontSize = 11.sp, maxLines = 2)
                    }
                }
                if (task.durationSeconds > 0L) {
                    Text(
                        formatDuration(taskRemaining(task, nowMillis)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (task.timerRunning) onToggleTimer(task) else onPauseBoard(!boardPaused)
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(if (boardPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (boardPaused) tr("resume", "Resume") else tr("pause", "Pause"), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onDone(task) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = KitchenColors.OliveDeep,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(tr("done", "Done"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun taskRemaining(task: TaskEntity, nowMillis: Long): Long = when {
    task.timerRunning -> max(0L, ((task.timerDeadlineAt ?: nowMillis) - nowMillis + 999L) / 1000L)
    else -> task.remainingSeconds
}

private fun taskDetail(task: TaskEntity): String = listOfNotNull(
    task.need.takeIf { it.isNotBlank() },
    task.prep.takeIf { it.isNotBlank() },
    task.durationSeconds.takeIf { it > 0 }?.let { "${it / 60} min" },
).joinToString(" · ")

@Composable
private fun PrepGapAlert(task: TaskEntity, tr: Translate, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = KitchenColors.AmberSoft,
        contentColor = KitchenColors.Amber,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${tr("prep", "Prep gap")}: ${task.prep.ifBlank { task.need }}",
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            TextButton(onClick = onClick) {
                Text(tr("review", "Resolve"), color = KitchenColors.Amber, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimelinePanel(
    tasks: List<TaskEntity>,
    tr: Translate,
    onNow: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
) {
    WorkbenchCard(Modifier.fillMaxWidth()) {
        if (tasks.isEmpty()) {
            Text(
                tr("done", "Nothing else is queued"),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@WorkbenchCard
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
            tasks.forEachIndexed { index, task ->
                TimelineRow(task, tr, onNow, onCheck)
                if (index != tasks.lastIndex) HorizontalDivider(color = KitchenColors.Line)
            }
        }
    }
}

@Composable
private fun TimelineRow(
    task: TaskEntity,
    tr: Translate,
    onNow: (TaskEntity) -> Unit,
    onCheck: (TaskEntity) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(12.dp).background(
                if (task.lane == LiveLane.WAITING.name) KitchenColors.Amber else KitchenColors.Olive,
                CircleShape,
            )
        )
        Column(Modifier.weight(1f)) {
            Text(
                task.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            taskDetail(task).takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
        OutlinedButton(
            onClick = { if (task.lane == LiveLane.WAITING.name) onCheck(task) else onNow(task) },
            modifier = Modifier.heightIn(min = 40.dp),
            shape = RoundedCornerShape(7.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                if (task.lane == LiveLane.WAITING.name) tr("check", "Check") else tr("now", "Start"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RunningTimersPanel(
    tasks: List<TaskEntity>,
    nowMillis: Long,
    tr: Translate,
    onToggleTimer: (TaskEntity) -> Unit,
) {
    val timed = tasks.filter {
        it.timerRunning || (it.lane == LiveLane.NOW.name && it.durationSeconds > 0)
    }.take(3)
    SupportPanel(
        title = tr("timers", "Running timers"),
        badge = "${timed.count { it.timerRunning }} ${tr("active", "active")}",
    ) {
        if (timed.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        timed.forEachIndexed { index, task ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleTimer(task) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(task.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(
                        if (task.timerRunning) tr("pause", "Tap to pause") else tr("start", "Tap to start"),
                        fontSize = 10.sp,
                        color = KitchenColors.Muted,
                    )
                }
                Text(formatDuration(taskRemaining(task, nowMillis)), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            if (index != timed.lastIndex) HorizontalDivider(color = KitchenColors.Line)
        }
    }
}

@Composable
private fun PrepGapsPanel(tasks: List<TaskEntity>, tr: Translate, onContinue: () -> Unit) {
    SupportPanel(
        title = tr("prep", "Prep gaps"),
        badge = "${tasks.size} ${tr("remaining", "left")}",
    ) {
        if (tasks.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        tasks.take(4).forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onContinue).padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, KitchenColors.Muted),
                    color = Color.Transparent,
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(task.prep.ifBlank { task.need }, fontSize = 11.sp, maxLines = 3)
            }
        }
    }
}

@Composable
private fun WaitingPanel(
    tasks: List<TaskEntity>,
    tr: Translate,
    onCheck: (TaskEntity) -> Unit,
    onOpen: () -> Unit,
) {
    SupportPanel(title = tr("wait", "Waiting check"), badge = tasks.size.toString()) {
        val task = tasks.firstOrNull()
        if (task == null) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KitchenColors.AmberSoft,
                contentColor = KitchenColors.Amber,
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(task.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    if (task.durationSeconds > 0L) {
                        Text(formatDuration(task.remainingSeconds), fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = { onCheck(task) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(tr("check", "Check now"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(tr("review", "Review"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SupportPanel(
    title: String,
    badge: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    WorkbenchCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                StatusBadge(badge)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun HomeUtilities(
    tr: Translate,
    onRepeat: () -> Unit,
    onNew: () -> Unit,
    onPaste: () -> Unit,
) {
    SectionHeader(tr("start", "Start another board"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UtilityButton(tr("repeat", "Repeat"), Icons.Default.Refresh, onRepeat, Modifier.weight(1f))
        UtilityButton(tr("new", "New"), Icons.Default.Add, onNew, Modifier.weight(1f))
        UtilityButton(tr("paste", "Paste"), Icons.Default.ContentPaste, onPaste, Modifier.weight(1f))
    }
}

@Composable
private fun UtilityButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

@Composable
private fun RecentBoards(
    boards: List<BoardEntity>,
    selectedId: String,
    tr: Translate,
    onAll: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val recent = boards.filter { it.id != selectedId }.take(3)
    if (recent.isEmpty()) return
    SectionHeader(tr("recent", "Recent"), action = tr("all", "All"), onAction = onAll)
    WorkbenchCard {
        Column {
            recent.forEachIndexed { index, board ->
                BoardListRow(board, tr) { onOpen(board.id) }
                if (index != recent.lastIndex) HorizontalDivider(color = KitchenColors.Line)
            }
        }
    }
}

@Composable
private fun EmptyStation(
    boards: List<BoardEntity>,
    profile: KitchenWindowProfile,
    tr: Translate,
    onNew: () -> Unit,
    onPaste: () -> Unit,
    onAll: () -> Unit,
    onOpenBoard: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("home", "Station"), fontSize = profile.pageTitleSize, fontWeight = FontWeight.Bold)
        WorkbenchCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = KitchenColors.OliveSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Kitchen, contentDescription = null, tint = KitchenColors.Olive)
                    }
                }
                Text(tr("new", "Start your first board"), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    tr("boardsStay", "Boards stay on this device"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                Button(
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr("new", "New board"), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPaste,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr("paste", "Paste tasks"), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (boards.isNotEmpty()) {
            SectionHeader(tr("recent", "Recent"), action = tr("all", "All"), onAction = onAll)
            WorkbenchCard {
                Column {
                    boards.take(4).forEachIndexed { index, board ->
                        BoardListRow(board, tr) { onOpenBoard(board.id) }
                        if (index != minOf(boards.lastIndex, 3)) HorizontalDivider(color = KitchenColors.Line)
                    }
                }
            }
        }
    }
}

@Composable
fun BoardArt(modifier: Modifier = Modifier) {
    Box(modifier.background(KitchenColors.OliveSoft), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.Kitchen,
            contentDescription = null,
            tint = KitchenColors.Olive,
            modifier = Modifier.size(32.dp),
        )
    }
}
