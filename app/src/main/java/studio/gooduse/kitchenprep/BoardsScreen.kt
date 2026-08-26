package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import studio.gooduse.kitchenprep.data.*

@Composable
fun BoardsScreen(
    boards: List<BoardEntity>,
    profile: KitchenWindowProfile,
    tr: Translate,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var filter by remember { mutableStateOf("UPCOMING") }
    val visible = remember(boards, filter) {
        if (filter == "UPCOMING") boards.filter { it.status != "COMPLETED" } else boards
    }
    Column(
        modifier = centeredContentModifier(profile)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = profile.gutter, vertical = 18.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle(tr("boards", "Boards"), profile, Modifier.weight(1f))
            OutlinedButton(onClick = onNew, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(tr("new", "New"), maxLines = 2)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == "UPCOMING",
                onClick = { filter = "UPCOMING" },
                label = { Text(tr("upcoming", "Upcoming"), maxLines = 2) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = filter == "ALL",
                onClick = { filter = "ALL" },
                label = { Text(tr("all", "All"), maxLines = 2) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(15.dp))

        val featured = visible.firstOrNull()
        if (featured != null) {
            FeatureBoard(featured, tr) { onOpen(featured.id) }
            Spacer(Modifier.height(16.dp))
        }

        WorkbenchCard {
            if (visible.isEmpty()) {
                Text(
                    tr("boardsStay", "Boards stay on this device"),
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                visible.forEachIndexed { index, board ->
                    BoardListRow(board, tr) { onOpen(board.id) }
                    if (index != visible.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBoard(board: BoardEntity, tr: Translate, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = KitchenColors.OliveDeep,
        contentColor = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column {
            Column(Modifier.padding(16.dp)) {
                Text(board.name, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    board.area,
                    fontSize = 12.sp,
                    color = Color(0xFFCFD3C9),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                ) {
                    Text(tr("open", "Open"), fontWeight = FontWeight.Black, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun BoardListRow(board: BoardEntity, tr: Translate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            BoardArt(Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(board.name, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Text(
                board.area,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        StatusBadge(
            text = when (board.status) {
                "COMPLETED" -> tr("done", "Done")
                "PAUSED" -> tr("pause", "Pause")
                else -> tr("active", "Active")
            },
        )
    }
}
