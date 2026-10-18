package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "task_executions")
data class TaskExecutionEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val status: String,
    val totalSteps: Int,
    val successfulSteps: Int,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val durationMs: Long,
    val failureReason: String?,
    val isLiveExecution: Boolean
)

@Entity(tableName = "step_records")
data class StepRecordEntity(
    @PrimaryKey val id: String,
    val executionId: String,
    val stepIndex: Int,
    val commandType: String,
    val commandSummary: String,
    val targetSelector: String?,
    val dispatchedAction: String?,
    val preSnapshotId: String? = null,
    val postSnapshotId: String? = null,
    val status: String,
    val verificationDetails: String,
    val durationMs: Long,
    val timestamp: Long
)

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val executionId: String?,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
)

@Dao
interface TaskExecutionDao {
    @Query("SELECT * FROM task_executions ORDER BY startTimestamp DESC")
    fun getAllExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE id = :id LIMIT 1")
    suspend fun getExecutionById(id: String): TaskExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: TaskExecutionEntity)

    @Update
    suspend fun updateExecution(execution: TaskExecutionEntity)

    @Query("DELETE FROM task_executions")
    suspend fun clearAll()
}

@Dao
interface StepRecordDao {
    @Query("SELECT * FROM step_records WHERE executionId = :executionId ORDER BY stepIndex ASC")
    fun getStepsForExecution(executionId: String): Flow<List<StepRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: StepRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<StepRecordEntity>)

    @Query("DELETE FROM step_records")
    suspend fun clearAll()
}

@Dao
interface DiagnosticLogDao {
    @Query("SELECT * FROM diagnostic_logs ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 300): Flow<List<DiagnosticLogEntity>>

    @Query("SELECT * FROM diagnostic_logs WHERE executionId = :executionId ORDER BY id ASC")
    fun getLogsForExecution(executionId: String): Flow<List<DiagnosticLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}

@Entity(tableName = "synthesized_tasks")
data class SynthesizedTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val commandCount: Int,
    val rawInteractionsCount: Int,
    val createdAt: Long,
    val commandsPayload: String = ""
)

@Dao
interface SynthesizedTaskDao {
    @Query("SELECT * FROM synthesized_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<SynthesizedTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: SynthesizedTaskEntity)

    @Query("DELETE FROM synthesized_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM synthesized_tasks")
    suspend fun clearAll()
}

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val triggerPhrases: String, // comma-separated
    val category: String,
    val commandsPayload: String,
    val isBuiltIn: Boolean,
    val createdAt: Long
)

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY isBuiltIn DESC, createdAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: String)

    @Query("SELECT COUNT(*) FROM skills")
    suspend fun getSkillCount(): Int
}

@Database(
    entities = [
        TaskExecutionEntity::class,
        StepRecordEntity::class,
        DiagnosticLogEntity::class,
        SynthesizedTaskEntity::class,
        SkillEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LocalAgentDatabase : RoomDatabase() {
    abstract fun taskExecutionDao(): TaskExecutionDao
    abstract fun stepRecordDao(): StepRecordDao
    abstract fun diagnosticLogDao(): DiagnosticLogDao
    abstract fun synthesizedTaskDao(): SynthesizedTaskDao
    abstract fun skillDao(): SkillDao

    companion object {
        @Volatile
        private var INSTANCE: LocalAgentDatabase? = null

        fun getInstance(context: Context): LocalAgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalAgentDatabase::class.java,
                    "local_agent.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
