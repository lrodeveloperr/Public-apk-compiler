package studio.gooduse.kitchenprep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.gooduse.kitchenprep.data.BoardEntity
import studio.gooduse.kitchenprep.data.TaskEntity

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
    val page = centeredContentModifier(profile)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 12.dp else 18.dp)
        .padding(bottom = 24.dp)

    Column(
        modifier = page,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageTitle(tr("home", "Home"), profile)

        if (selectedBoard == null) {
            EmptyHomeCard(tr = tr, onNew = onNew)
        } else if (profile.homeTwoPane) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1.08f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ActiveBoardCard(selectedBoard, tasks, tr, onContinue)
                    HomePrimaryActions(
                        profile = profile,
                        tr = tr,
                        onContinue = onContinue,
                        onNew = onNew,
                    )
                }
                Column(
                    modifier = Modifier.weight(0.92f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RecentBoardsCard(
                        boards = boards.filterNot { it.id == selectedBoard.id },
                        tr = tr,
                        onAll = onAll,
                        onOpenBoard = onOpenBoard,
                    )
                }
            }
        } else {
            ActiveBoardCard(selectedBoard, tasks, tr, onContinue)
            HomePrimaryActions(
                profile = profile,
                tr = tr,
                onContinue = onContinue,
                onNew = onNew,
            )
            RecentBoardsCard(
                boards = boards.filterNot { it.id == selectedBoard.id },
                tr = tr,
                onAll = onAll,
                onOpenBoard = onOpenBoard,
            )
        }
    }
}

@Composable
private fun EmptyHomeCard(tr: Translate, onNew: () -> Unit) {
    WorkbenchCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                tr("boards", "No boards yet"),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
            Button(
                onClick = onNew,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(tr("new", "New"), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ActiveBoardCard(
    board: BoardEntity,
    tasks: List<TaskEntity>,
    tr: Translate,
    onContinue: () -> Unit,
) {
    val counts = LiveLane.entries.associateWith { lane -> tasks.count { it.lane == lane.name } }

    WorkbenchCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(
                tr("active", "Active").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                board.name,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
            )
            if (board.area.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    board.area,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LiveLane.entries.forEach { lane ->
                    HomeCountTile(
                        modifier = Modifier.weight(1f),
                        value = counts[lane] ?: 0,
                        label = laneLabel(lane, tr),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(
                    text = if (board.status == "PAUSED") tr("pause", "Paused") else tr("active", "Active"),
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onContinue) {
                    Text(tr("open", "Open"), fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun HomeCountTile(
    modifier: Modifier,
    value: Int,
    label: String,
) {
    Surface(
        modifier = modifier.heightIn(min = 78.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                value.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun HomePrimaryActions(
    profile: KitchenWindowProfile,
    tr: Translate,
    onContinue: () -> Unit,
    onNew: () -> Unit,
) {
    val stack = profile.width < 380.dp || profile.largeText
    if (stack) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StartLiveButton(tr, onContinue, Modifier.fillMaxWidth())
            NewBoardButton(tr, onNew, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StartLiveButton(tr, onContinue, Modifier.weight(1f))
            NewBoardButton(tr, onNew, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StartLiveButton(tr: Translate, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(tr("live", "Live"), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun NewBoardButton(tr: Translate, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(tr("new", "New"), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RecentBoardsCard(
    boards: List<BoardEntity>,
    tr: Translate,
    onAll: () -> Unit,
    onOpenBoard: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${tr("recent", "Recent")} ${tr("boards", "Boards")}",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
        TextButton(onClick = onAll) {
            Text(tr("all", "All"), fontWeight = FontWeight.Bold)
        }
    }

    WorkbenchCard(Modifier.fillMaxWidth()) {
        if (boards.isEmpty()) {
            Text(
                "—",
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                boards.take(3).forEachIndexed { index, board ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBoard(board.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                board.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                            )
                            if (board.area.isNotBlank()) {
                                Text(
                                    board.area,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                    if (index != minOf(boards.size, 3) - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f))
                    }
                }
            }
        }
    }
}
