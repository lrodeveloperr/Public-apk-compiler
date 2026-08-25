package studio.gooduse.kitchenprep

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import studio.gooduse.kitchenprep.data.BoardEntity
import studio.gooduse.kitchenprep.data.BoardInput
import studio.gooduse.kitchenprep.data.KitchenRepository
import studio.gooduse.kitchenprep.data.SettingsState
import studio.gooduse.kitchenprep.data.TaskEntity
import studio.gooduse.kitchenprep.data.TaskInput
import studio.gooduse.kitchenprep.timers.TimerScheduler
import java.util.UUID
import kotlin.math.max

enum class AppScreen { HOME, CREATE, PASTE, BOARDS, LIVE, SETTINGS }
enum class LiveLane { NOW, WAITING, NEXT, DONE }

data class CreateDraft(
    val name: String = "",
    val targetMinutesOfDay: Int? = 18 * 60 + 30,
    val area: String = "Hot station",
    val notes: String = "",
    val timingMode: String = "TARGET",
    val tasks: List<TaskInput> = emptyList(),
    val sourceType: String = "MANUAL",
    val originalText: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class KitchenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KitchenRepository(application)
    private val timerScheduler = TimerScheduler(application)

    val boards: StateFlow<List<BoardEntity>> = repository.boards.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val settings: StateFlow<SettingsState> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsState(),
    )

    private val _screen = MutableStateFlow(AppScreen.HOME)
    val screen: StateFlow<AppScreen> = _screen

    private val _selectedBoardId = MutableStateFlow<String?>(null)
    val selectedBoardId: StateFlow<String?> = _selectedBoardId

    val selectedBoard: StateFlow<BoardEntity?> = combine(boards, _selectedBoardId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull { it.status == "ACTIVE" || it.status == "PAUSED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedTasks: StateFlow<List<TaskEntity>> = _selectedBoardId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeTasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _liveLane = MutableStateFlow(LiveLane.NOW)
    val liveLane: StateFlow<LiveLane> = _liveLane

    private val _createDraft = MutableStateFlow(CreateDraft())
    val createDraft: StateFlow<CreateDraft> = _createDraft

    private val _createStep = MutableStateFlow(1)
    val createStep: StateFlow<Int> = _createStep

    private val _pasteText = MutableStateFlow("")
    val pasteText: StateFlow<String> = _pasteText

    private val _safetyConfirmation = MutableStateFlow(false)
    val safetyConfirmation: StateFlow<Boolean> = _safetyConfirmation

    private val _lastUndoTaskId = MutableStateFlow<String?>(null)
    val lastUndoTaskId: StateFlow<String?> = _lastUndoTaskId

    init {
        viewModelScope.launch {
            boards.collect { list ->
                val current = _selectedBoardId.value
                if (current == null || list.none { it.id == current }) {
                    _selectedBoardId.value =
                        list.firstOrNull { it.status == "ACTIVE" || it.status == "PAUSED" }?.id
                            ?: list.firstOrNull()?.id
                }
            }
        }
        viewModelScope.launch {
            settings
                .distinctUntilChanged { old, new -> old.languageTag == new.languageTag }
                .collect { state ->
                    val requested = state.languageTag.ifBlank { "en" }
                    val requestedLanguage = requested.substringBefore('-').substringBefore('_')
                    val supported = KitchenStrings.supportedLocales.firstOrNull { choice ->
                        choice.tag.equals(requested, ignoreCase = true) ||
                            choice.tag.substringBefore('-').equals(requestedLanguage, ignoreCase = true)
                    }?.tag ?: "en"
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(supported))
                }
        }
        timerScheduler.restore()
    }

    fun navigate(screen: AppScreen) {
        _screen.value = screen
    }

    fun backToHome(): Boolean {
        if (_screen.value == AppScreen.HOME) return false
        _screen.value = AppScreen.HOME
        return true
    }

    fun openLive(boardId: String? = null, lane: LiveLane = LiveLane.NOW) {
        val resolved = boardId
            ?: boards.value.firstOrNull { it.status == "ACTIVE" || it.status == "PAUSED" }?.id
            ?: boards.value.firstOrNull()?.id
        _selectedBoardId.value = resolved
        _liveLane.value = lane
        _screen.value = AppScreen.LIVE
    }

    fun selectLane(lane: LiveLane) {
        _liveLane.value = lane
    }

    fun startNewBoard() {
        _createDraft.value = CreateDraft()
        _createStep.value = 1
        _screen.value = AppScreen.CREATE
    }

    fun openPaste() {
        _pasteText.value = ""
        _screen.value = AppScreen.PASTE
    }

    fun setPasteText(text: String) {
        _pasteText.value = text
    }

    fun importPaste() {
        val text = _pasteText.value
        if (text.isBlank()) return
        loadTextIntoDraft(text, "PASTE_TEXT")
    }

    fun handleSharedText(text: String) {
        if (text.isBlank()) return
        _pasteText.value = text
        loadTextIntoDraft(text, "ANDROID_SHARE_TEXT")
    }

    private fun loadTextIntoDraft(text: String, sourceType: String) {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(100)
            .toList()
        if (lines.isEmpty()) return
        _createDraft.value = CreateDraft(
            name = "",
            tasks = lines.map {
                TaskInput(
                    id = UUID.randomUUID().toString(),
                    name = it,
                    lane = "NEXT",
                    durationMinutes = 10,
                )
            },
            sourceType = sourceType,
            originalText = text,
        )
        _createStep.value = 2
        _screen.value = AppScreen.CREATE
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }
    fun updateArea(value: String) = updateDraft { copy(area = value) }
    fun updateNotes(value: String) = updateDraft { copy(notes = value) }
    fun updateTargetMinutes(value: Int) = updateDraft { copy(targetMinutesOfDay = value.coerceIn(0, 1439)) }
    fun updateTimingMode(value: String) = updateDraft { copy(timingMode = value) }

    private inline fun updateDraft(block: CreateDraft.() -> CreateDraft) {
        _createDraft.value = _createDraft.value.block()
    }

    fun addTask() {
        updateDraft {
            copy(tasks = tasks + TaskInput(id = UUID.randomUUID().toString()))
        }
    }

    fun deleteTask(id: String) {
        updateDraft { copy(tasks = tasks.filterNot { it.id == id }) }
    }

    fun updateTask(id: String, transform: (TaskInput) -> TaskInput) {
        updateDraft {
            copy(tasks = tasks.map { if (it.id == id) transform(it) else it })
        }
    }

    fun previousCreateStep() {
        if (_createStep.value > 1) _createStep.value = _createStep.value - 1 else _screen.value = AppScreen.HOME
    }

    fun nextCreateStep() {
        when (_createStep.value) {
            1 -> if (_createDraft.value.name.isNotBlank()) _createStep.value = 2
            2 -> if (_createDraft.value.tasks.any { it.name.isNotBlank() }) _createStep.value = 3
            else -> requestCreateBoard()
        }
    }

    fun canAdvanceCreate(): Boolean = when (_createStep.value) {
        1 -> _createDraft.value.name.isNotBlank()
        2 -> _createDraft.value.tasks.any { it.name.isNotBlank() }
        else -> _createDraft.value.name.isNotBlank() && _createDraft.value.tasks.any { it.name.isNotBlank() }
    }

    fun requestCreateBoard() {
        if (!canAdvanceCreate()) return
        if (!settings.value.safetyAcknowledged) {
            _safetyConfirmation.value = true
        } else {
            createBoardNow()
        }
    }

    fun dismissSafetyConfirmation() {
        _safetyConfirmation.value = false
    }

    fun confirmSafetyAndCreate() {
        viewModelScope.launch {
            repository.acknowledgeSafety()
            _safetyConfirmation.value = false
            createBoardNow()
        }
    }

    private fun createBoardNow() {
        val draft = _createDraft.value
        viewModelScope.launch {
            val boardId = repository.createBoard(
                BoardInput(
                    name = draft.name,
                    area = draft.area,
                    targetMinutesOfDay = draft.targetMinutesOfDay,
                    notes = draft.notes,
                    timingMode = draft.timingMode,
                    tasks = draft.tasks,
                    sourceType = draft.sourceType,
                    originalText = draft.originalText,
                )
            )
            _selectedBoardId.value = boardId
            _liveLane.value = LiveLane.NOW
            _screen.value = AppScreen.LIVE
            _createDraft.value = CreateDraft()
            _createStep.value = 1
        }
    }

    fun repeatMostRecent(boardId: String? = null) {
        val source = boardId ?: boards.value.firstOrNull()?.id
        if (source == null) {
            startNewBoard()
            return
        }
        viewModelScope.launch {
            repository.duplicateBoard(source)?.let { newId ->
                _selectedBoardId.value = newId
                _liveLane.value = LiveLane.NOW
                _screen.value = AppScreen.LIVE
            }
        }
    }

    fun setBoardPaused(paused: Boolean) {
        val boardId = selectedBoard.value?.id ?: return
        viewModelScope.launch { repository.setBoardPaused(boardId, paused) }
    }

    fun moveTask(task: TaskEntity, lane: LiveLane) {
        viewModelScope.launch {
            if (lane == LiveLane.DONE) timerScheduler.cancel(task.id)
            repository.moveTask(task.id, lane.name)
            if (lane == LiveLane.DONE) _lastUndoTaskId.value = task.id
        }
    }

    fun checkWaiting(task: TaskEntity) {
        viewModelScope.launch {
            timerScheduler.cancel(task.id)
            if (task.timerRunning || task.timerDeadlineAt != null) {
                repository.resetExpiredTimer(task.id)
            }
            repository.moveTask(task.id, LiveLane.NOW.name)
            _liveLane.value = LiveLane.NOW
        }
    }

    fun togglePriority(task: TaskEntity) {
        viewModelScope.launch { repository.togglePriority(task.id) }
    }

    fun toggleTimer(task: TaskEntity) {
        if (task.durationSeconds <= 0L) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (task.timerRunning) {
                val remaining = task.timerDeadlineAt
                    ?.let { max(0L, (it - now + 999L) / 1000L) }
                    ?: task.remainingSeconds
                timerScheduler.cancel(task.id)
                repository.pauseTimer(task.id, remaining)
            } else {
                val remaining = task.remainingSeconds
                    .takeIf { it > 0L }
                    ?: task.durationSeconds
                val deadline = now + remaining * 1000L
                repository.startTimer(task.id, deadline, remaining)
                if (settings.value.alerts) timerScheduler.schedule(task.id, deadline)
            }
        }
    }

    fun undoLastTask() {
        val taskId = _lastUndoTaskId.value ?: return
        viewModelScope.launch {
            timerScheduler.cancel(taskId)
            repository.undoTask(taskId)
            _lastUndoTaskId.value = null
        }
    }

    fun clearUndo() {
        _lastUndoTaskId.value = null
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch { repository.setLanguage(tag) }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch { repository.setTheme(mode) }
    }

    fun setAlerts(value: Boolean) {
        viewModelScope.launch { repository.setAlerts(value) }
    }

    fun setAwake(value: Boolean) {
        viewModelScope.launch { repository.setAwake(value) }
    }

    fun setCompact(value: Boolean) {
        viewModelScope.launch { repository.setCompact(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { repository.setHaptics(value) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            timerScheduler.clearAll()
            repository.clearAllData()
            _selectedBoardId.value = null
            _screen.value = AppScreen.HOME
        }
    }

    fun reconcileTimers() {
        timerScheduler.restore()
    }
}
