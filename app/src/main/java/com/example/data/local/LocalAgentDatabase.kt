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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetAppLabel: String,
    val resolvedPackageName: String,
    val intendedTask: String,
    val requiredPermissionsJson: String,
    val orderedExecutableStepsJson: String,
    val targetResolutionRulesJson: String,
    val preconditionsJson: String,
    val postconditionsJson: String,
    val verificationStrategy: String,
    val knownLimitationsJson: String,
    val evidenceSummary: String,
    val successfulExecutions: Int,
    val failedValidations: Int,
    val state: String,
    val userApprovalNote: String?,
    val createdTimestampMs: Long,
    val lastValidatedTimestampMs: Long?,
    val version: Int
)

@Dao
interface LearningRecordDao {
    @Query("SELECT * FROM learning_records ORDER BY createdTimestampMs DESC")
    fun getAllRecords(): Flow<List<LearningRecordEntity>>

    @Query("SELECT * FROM learning_records ORDER BY createdTimestampMs DESC")
    suspend fun getAllRecordsSnapshot(): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_records WHERE state = :state ORDER BY createdTimestampMs DESC")
    fun getRecordsByState(state: String): Flow<List<LearningRecordEntity>>

    @Query("SELECT * FROM learning_records WHERE state = :state ORDER BY createdTimestampMs DESC")
    suspend fun getRecordsByStateSnapshot(state: String): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): LearningRecordEntity?

    @Query("SELECT * FROM learning_records WHERE resolvedPackageName = :packageName AND intendedTask = :task LIMIT 1")
    suspend fun findByPackageAndTask(packageName: String, task: String): LearningRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LearningRecordEntity)

    @Update
    suspend fun updateRecord(record: LearningRecordEntity)

    @Query("DELETE FROM learning_records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    @Query("DELETE FROM learning_records")
    suspend fun clearAll()
}

@Database(
    entities = [
        TaskExecutionEntity::class,
        StepRecordEntity::class,
        DiagnosticLogEntity::class,
        SynthesizedTaskEntity::class,
        SkillEntity::class,
        LearningRecordEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class LocalAgentDatabase : RoomDatabase() {
    abstract fun taskExecutionDao(): TaskExecutionDao
    abstract fun stepRecordDao(): StepRecordDao
    abstract fun diagnosticLogDao(): DiagnosticLogDao
    abstract fun synthesizedTaskDao(): SynthesizedTaskDao
    abstract fun skillDao(): SkillDao
    abstract fun learningRecordDao(): LearningRecordDao

    companion object {
        @Volatile
        private var INSTANCE: LocalAgentDatabase? = null

        /**
         * Explicit non-destructive migration from Database v4 to v5.
         * Adds learning_records table while strictly preserving all existing task executions,
         * step records, diagnostic logs, synthesized tasks, and legacy skills.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `learning_records` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetAppLabel` TEXT NOT NULL,
                        `resolvedPackageName` TEXT NOT NULL,
                        `intendedTask` TEXT NOT NULL,
                        `requiredPermissionsJson` TEXT NOT NULL,
                        `orderedExecutableStepsJson` TEXT NOT NULL,
                        `targetResolutionRulesJson` TEXT NOT NULL,
                        `preconditionsJson` TEXT NOT NULL,
                        `postconditionsJson` TEXT NOT NULL,
                        `verificationStrategy` TEXT NOT NULL,
                        `knownLimitationsJson` TEXT NOT NULL,
                        `evidenceSummary` TEXT NOT NULL,
                        `successfulExecutions` INTEGER NOT NULL,
                        `failedValidations` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        `userApprovalNote` TEXT,
                        `createdTimestampMs` INTEGER NOT NULL,
                        `lastValidatedTimestampMs` INTEGER,
                        `version` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): LocalAgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalAgentDatabase::class.java,
                    "local_agent.db"
                )
                .addMigrations(MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
