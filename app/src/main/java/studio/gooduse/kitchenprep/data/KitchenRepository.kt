package studio.gooduse.kitchenprep.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import java.util.UUID

private val Context.kitchenPreferences by preferencesDataStore(name = "kitchen_prep_settings")

data class SettingsState(
    val languageTag: String = Locale.getDefault().toLanguageTag(),
    val themeMode: String = "system",
    val alerts: Boolean = true,
    val keepAwake: Boolean = true,
    val compactLive: Boolean = false,
    val haptics: Boolean = true,
    val safetyAcknowledged: Boolean = false,
)

data class TaskInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val lane: String = "NEXT",
    val have: String = "",
    val need: String = "",
    val prep: String = "",
    val durationMinutes: Int = 10,
)

data class BoardInput(
    val name: String,
    val area: String,
    val targetMinutesOfDay: Int?,
    val notes: String,
    val timingMode: String,
    val tasks: List<TaskInput>,
    val sourceType: String = "MANUAL",
    val originalText: String? = null,
)

class KitchenRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = KitchenDatabase.get(appContext)
    private val dao = db.kitchenDao()

    val boards: Flow<List<BoardEntity>> = dao.observeBoards()

    val settings: Flow<SettingsState> = appContext.kitchenPreferences.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            SettingsState(
                languageTag = prefs[KEY_LANGUAGE] ?: Locale.getDefault().toLanguageTag(),
                themeMode = prefs[KEY_THEME] ?: "system",
                alerts = prefs[KEY_ALERTS] ?: true,
                keepAwake = prefs[KEY_AWAKE] ?: true,
                compactLive = prefs[KEY_COMPACT] ?: false,
                haptics = prefs[KEY_HAPTICS] ?: true,
                safetyAcknowledged = prefs[KEY_SAFETY] ?: false,
            )
        }

    fun observeTasks(boardId: String): Flow<List<TaskEntity>> = dao.observeTasks(boardId)

    suspend fun createBoard(input: BoardInput): String {
        val cleanTasks = input.tasks
            .filter { it.name.isNotBlank() }
            .mapIndexed { index, task ->
                val cleanLane = task.lane.uppercase().let {
                    if (it in VALID_LANES) it else "NEXT"
                }
                task.copy(lane = cleanLane, durationMinutes = task.durationMinutes.coerceAtLeast(0))
                    .let { index to it }
            }

        require(input.name.isNotBlank()) { "Board name is required" }
        require(cleanTasks.isNotEmpty()) { "At least one task is required" }

        val boardId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val hasNow = cleanTasks.any { (_, task) -> task.lane == "NOW" }

        db.withTransaction {
            dao.upsertBoard(
                BoardEntity(
                    id = boardId,
                    name = input.name.trim(),
                    area = input.area.trim().ifBlank { "Hot station" },
                    targetMinutesOfDay = input.targetMinutesOfDay,
                    notes = input.notes.trim(),
                    timingMode = input.timingMode,
                    status = "ACTIVE",
                    sourceType = input.sourceType,
                    originalText = input.originalText,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            dao.upsertTasks(
                cleanTasks.map { (index, task) ->
                    val lane = if (!hasNow && index == 0 && task.lane == "NEXT") "NOW" else task.lane
                    val seconds = task.durationMinutes.toLong() * 60L
                    TaskEntity(
                        id = task.id,
                        boardId = boardId,
                        name = task.name.trim(),
                        lane = lane,
                        previousLane = null,
                        have = task.have.trim(),
                        need = task.need.trim(),
                        prep = task.prep.trim(),
                        durationSeconds = seconds,
                        remainingSeconds = seconds,
                        timerDeadlineAt = null,
                        timerRunning = false,
                        priority = false,
                        sortOrder = index,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            )
        }
        return boardId
    }

    suspend fun duplicateBoard(sourceBoardId: String): String? {
        val source = dao.getBoard(sourceBoardId) ?: return null
        val tasks = dao.getTasks(sourceBoardId)
        if (tasks.isEmpty()) return null

        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.upsertBoard(
                source.copy(
                    id = newId,
                    status = "ACTIVE",
                    sourceType = "DUPLICATE_BOARD",
                    originalText = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            dao.upsertTasks(
                tasks.mapIndexed { index, task ->
                    task.copy(
                        id = UUID.randomUUID().toString(),
                        boardId = newId,
                        lane = if (index == 0) "NOW" else "NEXT",
                        previousLane = null,
                        remainingSeconds = task.durationSeconds,
                        timerDeadlineAt = null,
                        timerRunning = false,
                        priority = false,
                        sortOrder = index,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            )
        }
        return newId
    }

    suspend fun setBoardPaused(boardId: String, paused: Boolean) {
        val board = dao.getBoard(boardId) ?: return
        dao.upsertBoard(
            board.copy(
                status = if (paused) "PAUSED" else "ACTIVE",
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun moveTask(taskId: String, newLane: String) {
        val task = dao.getTask(taskId) ?: return
        val lane = newLane.uppercase()
        if (lane !in VALID_LANES || lane == task.lane) return
        dao.upsertTask(
            task.copy(
                previousLane = task.lane,
                lane = lane,
                timerRunning = if (lane == "DONE") false else task.timerRunning,
                timerDeadlineAt = if (lane == "DONE") null else task.timerDeadlineAt,
                updatedAt = System.currentTimeMillis(),
            )
        )
        touchBoard(task.boardId)
        refreshBoardCompletion(task.boardId)
    }

    suspend fun undoTask(taskId: String) {
        val task = dao.getTask(taskId) ?: return
        val previous = task.previousLane ?: if (task.lane == "DONE") "NOW" else return
        dao.upsertTask(
            task.copy(
                lane = previous,
                previousLane = null,
                timerRunning = false,
                timerDeadlineAt = null,
                remainingSeconds = task.durationSeconds,
                updatedAt = System.currentTimeMillis(),
            )
        )
        val board = dao.getBoard(task.boardId)
        if (board != null && board.status == "COMPLETED") {
            dao.upsertBoard(board.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis()))
        } else {
            touchBoard(task.boardId)
        }
    }

    suspend fun togglePriority(taskId: String) {
        val task = dao.getTask(taskId) ?: return
        dao.upsertTask(task.copy(priority = !task.priority, updatedAt = System.currentTimeMillis()))
        touchBoard(task.boardId)
    }

    suspend fun startTimer(taskId: String, deadlineAt: Long, remainingSeconds: Long) {
        val task = dao.getTask(taskId) ?: return
        dao.upsertTask(
            task.copy(
                timerDeadlineAt = deadlineAt,
                remainingSeconds = remainingSeconds.coerceAtLeast(1L),
                timerRunning = true,
                updatedAt = System.currentTimeMillis(),
            )
        )
        touchBoard(task.boardId)
    }

    suspend fun pauseTimer(taskId: String, remainingSeconds: Long) {
        val task = dao.getTask(taskId) ?: return
        dao.upsertTask(
            task.copy(
                timerDeadlineAt = null,
                remainingSeconds = remainingSeconds.coerceAtLeast(0L),
                timerRunning = false,
                updatedAt = System.currentTimeMillis(),
            )
        )
        touchBoard(task.boardId)
    }

    suspend fun resetExpiredTimer(taskId: String) {
        val task = dao.getTask(taskId) ?: return
        dao.upsertTask(
            task.copy(
                timerDeadlineAt = null,
                timerRunning = false,
                remainingSeconds = task.durationSeconds,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun setLanguage(tag: String) = editSetting(KEY_LANGUAGE, tag)
    suspend fun setTheme(mode: String) = editSetting(KEY_THEME, mode)
    suspend fun setAlerts(value: Boolean) = editSetting(KEY_ALERTS, value)
    suspend fun setAwake(value: Boolean) = editSetting(KEY_AWAKE, value)
    suspend fun setCompact(value: Boolean) = editSetting(KEY_COMPACT, value)
    suspend fun setHaptics(value: Boolean) = editSetting(KEY_HAPTICS, value)
    suspend fun acknowledgeSafety() = editSetting(KEY_SAFETY, true)

    suspend fun clearAllData() {
        db.withTransaction {
            dao.deleteAllTasks()
            dao.deleteAllBoards()
        }
        appContext.kitchenPreferences.edit { it.clear() }
    }

    private suspend fun touchBoard(boardId: String) {
        val board = dao.getBoard(boardId) ?: return
        dao.upsertBoard(board.copy(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun refreshBoardCompletion(boardId: String) {
        val tasks = dao.getTasks(boardId)
        if (tasks.isEmpty()) return
        val board = dao.getBoard(boardId) ?: return
        val allDone = tasks.all { it.lane == "DONE" }
        val nextStatus = when {
            allDone -> "COMPLETED"
            board.status == "COMPLETED" -> "ACTIVE"
            else -> board.status
        }
        if (nextStatus != board.status) {
            dao.upsertBoard(board.copy(status = nextStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun <T> editSetting(
        key: androidx.datastore.preferences.core.Preferences.Key<T>,
        value: T,
    ) {
        appContext.kitchenPreferences.edit { preferences -> preferences[key] = value }
    }

    companion object {
        private val VALID_LANES = setOf("NOW", "WAITING", "NEXT", "DONE")
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_ALERTS = booleanPreferencesKey("alerts")
        private val KEY_AWAKE = booleanPreferencesKey("keep_awake")
        private val KEY_COMPACT = booleanPreferencesKey("compact_live")
        private val KEY_HAPTICS = booleanPreferencesKey("haptics")
        private val KEY_SAFETY = booleanPreferencesKey("safety_acknowledged")
    }
}
