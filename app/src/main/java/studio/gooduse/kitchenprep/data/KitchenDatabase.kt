package studio.gooduse.kitchenprep.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val area: String,
    val targetMinutesOfDay: Int?,
    val notes: String,
    val timingMode: String,
    val status: String,
    val sourceType: String,
    val originalText: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("boardId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val name: String,
    val lane: String,
    val previousLane: String?,
    val have: String,
    val need: String,
    val prep: String,
    val durationSeconds: Long,
    val remainingSeconds: Long,
    val timerDeadlineAt: Long?,
    val timerRunning: Boolean,
    val priority: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface KitchenDao {
    @Query("SELECT * FROM boards ORDER BY updatedAt DESC")
    fun observeBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM tasks WHERE boardId = :boardId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeTasks(boardId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM boards WHERE id = :boardId LIMIT 1")
    suspend fun getBoard(boardId: String): BoardEntity?

    @Query("SELECT * FROM tasks WHERE boardId = :boardId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getTasks(boardId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBoard(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBoards(boards: List<BoardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE boardId = :boardId")
    suspend fun deleteTasksForBoard(boardId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM boards WHERE id = :boardId")
    suspend fun deleteBoard(boardId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM boards")
    suspend fun deleteAllBoards()
}

@Database(
    entities = [BoardEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun kitchenDao(): KitchenDao

    companion object {
        @Volatile private var instance: KitchenDatabase? = null

        fun get(context: Context): KitchenDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "kitchen-prep-board.db",
                ).build().also { instance = it }
            }
    }
}
