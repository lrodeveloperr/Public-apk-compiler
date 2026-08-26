@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import studio.gooduse.kitchenprep.data.*

@Composable
fun CreateScreen(
    draft: CreateDraft,
    step: Int,
    profile: KitchenWindowProfile,
    tr: Translate,
    onName: (String) -> Unit,
    onArea: (String) -> Unit,
    onNotes: (String) -> Unit,
    onTime: (Int) -> Unit,
    onTiming: (String) -> Unit,
    onAddTask: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onUpdateTask: (String, (TaskInput) -> TaskInput) -> Unit,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = centeredContentModifier(profile)
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = profile.gutter),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(if (profile.compactHeight) 10.dp else 18.dp))
            PageTitle(tr("new", "New"), profile)
            CreateSteps(step, tr)
            Spacer(Modifier.height(14.dp))
            when (step) {
                1 -> DetailsStep(draft, profile, tr, onName, onArea, onNotes, onTime)
                2 -> TasksStep(
                    draft = draft,
                    profile = profile,
                    tr = tr,
                    onAddTask = onAddTask,
                    onDeleteTask = onDeleteTask,
                    onUpdateTask = onUpdateTask,
                )
                else -> TimingStep(draft, tr, onTiming)
            }
            Spacer(Modifier.height(18.dp))
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(0.7f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(tr("back", "Back"), fontWeight = FontWeight.Black, maxLines = 2)
                }
                Button(
                    onClick = onNext,
                    enabled = canAdvance,
                    modifier = Modifier.weight(1.3f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        if (step == 3) tr("start", "Start") else tr("next", "Next"),
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun CreateSteps(step: Int, tr: Translate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            1 to tr("details", "Details"),
            2 to tr("tasks", "Tasks"),
            3 to tr("timing", "Timing"),
        ).forEachIndexed { index, pair ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = if (step == pair.first) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (step == pair.first) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${pair.first}", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    pair.second,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (step == pair.first) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (index < 2) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
fun DetailsStep(
    draft: CreateDraft,
    profile: KitchenWindowProfile,
    tr: Translate,
    onName: (String) -> Unit,
    onArea: (String) -> Unit,
    onNotes: (String) -> Unit,
    onTime: (Int) -> Unit,
) {
    WorkbenchCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = onName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("name", "Name")) },
                singleLine = true,
            )
            NativeTimeField(
                minutes = draft.targetMinutesOfDay ?: 18 * 60 + 30,
                compactHeight = profile.compactHeight,
                label = tr("time", "Time"),
                tr = tr,
                onTime = onTime,
            )
            AreaField(
                current = draft.area,
                tr = tr,
                onArea = onArea,
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = onNotes,
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                label = { Text(tr("notes", "Notes")) },
                minLines = 2,
            )
        }
    }
}

@Composable
fun NativeTimeField(
    minutes: Int,
    compactHeight: Boolean,
    label: String,
    tr: Translate,
    onTime: (Int) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { show = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Icon(Icons.Default.AccessTime, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            "$label · ${formatMinutesOfDay(minutes)}",
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
    }
    if (show) {
        val state = rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { show = false },
            text = {
                if (compactHeight) TimeInput(state = state) else TimePicker(state = state)
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text(tr("cancel", "Cancel")) }
            },
            confirmButton = {
                Button(onClick = {
                    onTime(state.hour * 60 + state.minute)
                    show = false
                }) { Text(tr("save", "Save")) }
            },
        )
    }
}

@Composable
fun AreaField(
    current: String,
    tr: Translate,
    onArea: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "Hot station" to tr("stationHot", "Hot station"),
        "Cold prep" to tr("stationCold", "Cold prep"),
        "Pastry" to tr("stationPastry", "Pastry"),
        "Sandwich" to tr("stationSandwich", "Sandwich"),
        "Banquet" to tr("stationBanquet", "Banquet"),
    )
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                "${tr("area", "Area")} · ${options.firstOrNull { it.first == current }?.second ?: current}",
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onArea(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun TasksStep(
    draft: CreateDraft,
    profile: KitchenWindowProfile,
    tr: Translate,
    onAddTask: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onUpdateTask: (String, (TaskInput) -> TaskInput) -> Unit,
) {
    SectionHeader(
        title = tr("tasks", "Tasks"),
        customAction = {
            OutlinedButton(
                onClick = onAddTask,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(tr("newTask", "New task"))
            }
        },
    )
    if (draft.tasks.isEmpty()) {
        WorkbenchCard {
            Text(
                tr("newTask", "New task"),
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    draft.tasks.forEach { task ->
        Spacer(Modifier.height(9.dp))
        TaskEditorCard(
            task = task,
            profile = profile,
            tr = tr,
            onDelete = { onDeleteTask(task.id) },
            onChange = { transform ->
                onUpdateTask(task.id) { current -> current.transform() }
            },
        )
    }
}

@Composable
fun TaskEditorCard(
    task: TaskInput,
    profile: KitchenWindowProfile,
    tr: Translate,
    onDelete: () -> Unit,
    onChange: (TaskInput.() -> TaskInput) -> Unit,
) {
    WorkbenchCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = task.name,
                    onValueChange = { value -> onChange { copy(name = value) } },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("name", "Name")) },
                    singleLine = true,
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = tr("delete", "Delete"))
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                listOf(LiveLane.NOW, LiveLane.WAITING, LiveLane.NEXT).forEach { lane ->
                    FilterChip(
                        selected = task.lane == lane.name,
                        onClick = { onChange { copy(lane = lane.name) } },
                        label = { Text(laneLabel(lane, tr), maxLines = 2) },
                    )
                }
            }

            if (profile.width >= 600.dp && !profile.largeText) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuantityField(
                        value = task.have,
                        label = tr("have", "Have"),
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onChange { copy(have = value) } },
                    )
                    QuantityField(
                        value = task.need,
                        label = tr("need", "Need"),
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onChange { copy(need = value) } },
                    )
                    QuantityField(
                        value = task.prep,
                        label = tr("prep", "Prep"),
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onChange { copy(prep = value) } },
                    )
                }
            } else {
                QuantityField(task.have, tr("have", "Have"), Modifier.fillMaxWidth()) { value ->
                    onChange { copy(have = value) }
                }
                QuantityField(task.need, tr("need", "Need"), Modifier.fillMaxWidth()) { value ->
                    onChange { copy(need = value) }
                }
                QuantityField(task.prep, tr("prep", "Prep"), Modifier.fillMaxWidth()) { value ->
                    onChange { copy(prep = value) }
                }
            }

            OutlinedTextField(
                value = task.durationMinutes.toString(),
                onValueChange = { text ->
                    text.filter(Char::isDigit).toIntOrNull()?.let { value ->
                        onChange { copy(durationMinutes = value.coerceIn(0, 24 * 60)) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("timing", "Timing")) },
                suffix = { Text("min") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
fun QuantityField(
    value: String,
    label: String,
    modifier: Modifier,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
fun TimingStep(
    draft: CreateDraft,
    tr: Translate,
    onTiming: (String) -> Unit,
) {
    SectionHeader(tr("timing", "Timing"))
    WorkbenchCard {
        FlowRow(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "MANUAL" to tr("manual", "Manual"),
                "TARGET" to tr("target", "Target"),
                "CUSTOM" to tr("custom", "Custom"),
            ).forEach { (value, label) ->
                FilterChip(
                    selected = draft.timingMode == value,
                    onClick = { onTiming(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
fun PasteScreen(
    text: String,
    profile: KitchenWindowProfile,
    tr: Translate,
    onText: (String) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = centeredContentModifier(profile)
            .fillMaxSize()
            .padding(horizontal = profile.gutter, vertical = 18.dp),
    ) {
        PageTitle(tr("paste", "Paste"), profile)
        Spacer(Modifier.height(12.dp))
        WorkbenchCard(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = text,
                onValueChange = onText,
                modifier = Modifier.fillMaxSize().padding(14.dp),
                label = { Text(tr("tasks", "Tasks")) },
                minLines = 8,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(0.8f).heightIn(min = 52.dp),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(tr("back", "Back"), maxLines = 2)
            }
            Button(
                onClick = onImport,
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1.2f).heightIn(min = 52.dp),
            ) {
                Text(tr("continue", "Continue"), fontWeight = FontWeight.Black, maxLines = 2)
            }
        }
    }
}
