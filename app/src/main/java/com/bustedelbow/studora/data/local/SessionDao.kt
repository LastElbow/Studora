package com.bustedelbow.studora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room access for session persistence (ADR-0002).
 *
 * Behaviour is exercised by instrumented tests on a device later; JVM compilation of the
 * generated implementation is verified by `assembleDebug`. The queries are deliberately tiny and
 * map one-to-one onto [com.bustedelbow.studora.domain.SessionRepository].
 */
@Dao
interface SessionDao {

    /** Appends a completed session; an exact-duplicate interval replaces the existing row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompleted(session: SessionEntity)

    /** All completed sessions, oldest start first. */
    @Query("SELECT * FROM completed_sessions ORDER BY startMillis ASC")
    fun completedSessions(): Flow<List<SessionEntity>>

    /** Stores or replaces the single in-progress row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInProgress(inProgress: InProgressEntity)

    /** The current in-progress snapshot, or `null` when none is stored. */
    @Query("SELECT * FROM in_progress WHERE id = ${InProgressEntity.SINGLE_ROW_ID}")
    fun inProgress(): Flow<InProgressEntity?>

    /** Deletes the in-progress snapshot after completion or discard. */
    @Query("DELETE FROM in_progress")
    suspend fun clearInProgress()

    /** Deletes every completed session. */
    @Query("DELETE FROM completed_sessions")
    suspend fun deleteAllCompleted()

    /** Deletes all completed sessions and the in-progress snapshot atomically. */
    @Transaction
    suspend fun clearAll() {
        deleteAllCompleted()
        clearInProgress()
    }
}
