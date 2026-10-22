package com.example.core.memory

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Cross-App Memory Stack Entity.
 * Represents persistent, transactional fragments of extracted data (e.g. OTP codes, account numbers,
 * user notes, deep links) captured during inter-app transitions on Android 8.1 / MediaTek Helio P23.
 */
@Entity(tableName = "memory_stack")
data class MemoryStackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourcePackage: String,
    val fragmentText: String,
    val contextualTag: String,
    val stateMetadata: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Room Data Access Object for cross-app memory transactions.
 * Supports FIFO/LIFO stack operations, observation streams, and tag-filtered lookups.
 */
@Dao
interface MemoryStackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pushMemory(entity: MemoryStackEntity): Long

    @Query("SELECT * FROM memory_stack ORDER BY timestamp DESC LIMIT 1")
    suspend fun peekTopMemory(): MemoryStackEntity?

    @Query("SELECT * FROM memory_stack WHERE contextualTag = :tag ORDER BY timestamp DESC LIMIT 1")
    suspend fun peekByTag(tag: String): MemoryStackEntity?

    @Query("SELECT * FROM memory_stack WHERE sourcePackage = :packageName ORDER BY timestamp DESC")
    fun getFragmentsForPackage(packageName: String): Flow<List<MemoryStackEntity>>

    @Query("SELECT * FROM memory_stack ORDER BY timestamp DESC")
    fun getAllFragmentsFlow(): Flow<List<MemoryStackEntity>>

    @Query("SELECT * FROM memory_stack ORDER BY timestamp DESC")
    suspend fun getAllFragmentsSnapshot(): List<MemoryStackEntity>

    @Query("DELETE FROM memory_stack WHERE id = (SELECT id FROM memory_stack ORDER BY timestamp DESC LIMIT 1)")
    suspend fun popMemory(): Int

    @Query("DELETE FROM memory_stack WHERE id = :id")
    suspend fun deleteMemoryById(id: Long): Int

    @Query("DELETE FROM memory_stack")
    suspend fun clearStack(): Int

    @Query("SELECT COUNT(*) FROM memory_stack")
    suspend fun getStackDepth(): Int
}

/**
 * High-level orchestration manager for cross-app clipboard and contextual data handshake.
 * Allows LocalAgent to extract verification tokens from SMS/messaging apps and inject
 * them directly into banking, authentication, or browser targets without RAM bloat.
 */
class CrossAppMemoryManager(private val dao: MemoryStackDao) {

    suspend fun pushFragment(
        sourcePackage: String,
        text: String,
        tag: String = "clipboard_transfer",
        metadata: String = "{}"
    ): Long {
        val entity = MemoryStackEntity(
            sourcePackage = sourcePackage,
            fragmentText = text.trim(),
            contextualTag = tag,
            stateMetadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        return dao.pushMemory(entity)
    }

    suspend fun peekLatestFragment(): MemoryStackEntity? {
        return dao.peekTopMemory()
    }

    suspend fun popLatestFragment(): MemoryStackEntity? {
        val top = dao.peekTopMemory()
        if (top != null) {
            dao.deleteMemoryById(top.id)
        }
        return top
    }

    suspend fun resolveCrossAppTransfer(targetPackage: String, expectedTag: String? = null): String? {
        val fragment = if (expectedTag != null) {
            dao.peekByTag(expectedTag)
        } else {
            dao.peekTopMemory()
        }
        return fragment?.fragmentText
    }
}
