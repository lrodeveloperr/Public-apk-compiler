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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.gooduse.kitchenprep.data.BoardEntity

@Composable
fun BoardsScreen(
    boards: List<BoardEntity>,
    profile: KitchenWindowProfile,
    tr: Translate,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var filter by remember { mutableStateOf("ALL") }
    val visible = remember(boards, filter) {
        when (filter) {
            "ACTIVE" -> boards.filter { it.status == "ACTIVE" }
            "PAUSED" -> boards.filter { it.status == "PAUSED" }
            "DONE" -> boards.filter { it.status == "COMPLETED" }
            else -> boards
        }
    }

    Column(
        modifier = centeredContentModifier(profile)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 12.dp else 18.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (profile.width < 420.dp || profile.largeText) {
            PageTitle(tr("boards", "Boards"), profile)
            NewBoardTopButton(tr, onNew, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PageTitle(tr("boards", "Boards"), profile, Modifier.weight(1f))
                NewBoardTopButton(tr, onNew, Modifier.widthIn(min = 180.dp))
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoardFilterChip("ALL", tr("all", "All"), filter) { filter = "ALL" }
            BoardFilterChip("ACTIVE", tr("active", "Active"), filter) { filter = "ACTIVE" }
            BoardFilterChip("PAUSED", tr("pause", "Paused"), filter) { filter = "PAUSED" }
            BoardFilterChip("DONE", tr("done", "Done"), filter) { filter = "DONE" }
        }

        if (visible.isEmpty()) {
            WorkbenchCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("—", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    OutlinedButton(
                        onClick = onNew,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(tr("new", "New"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (profile.width >= 840.dp && !profile.largeText) {
            visible.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    pair.forEach { board ->
                        FrozenBoardCard(
                            board = board,
                            tr = tr,
                            onClick = { onOpen(board.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            visible.forEach { board ->
                FrozenBoardCard(
                    board = board,
                    tr = tr,
                    onClick = { onOpen(board.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NewBoardTopButton(tr: Translate, onNew: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onNew,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(tr("new", "New"), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BoardFilterChip(
    key: String,
    label: String,
    selected: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected == key,
        onClick = onClick,
        label = { Text(label, maxLines = 2) },
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun FrozenBoardCard(
    board: BoardEntity,
    tr: Translate,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    WorkbenchCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    board.name,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                )
                if (board.area.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        board.area,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            StatusBadge(
                text = when (board.status) {
                    "COMPLETED" -> tr("done", "Done")
                    "PAUSED" -> tr("pause", "Paused")
                    else -> tr("active", "Active")
                },
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
