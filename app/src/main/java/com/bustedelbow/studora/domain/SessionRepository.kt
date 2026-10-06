package com.bustedelbow.studora.domain

import kotlinx.coroutines.flow.Flow

/**
 * Snapshot of a session that has started but not yet finished, used for kill-recovery.
 *
 * The stored interval is the *start* instant plus the configured length: the persistence schema
 * (ADR-0002) deliberately keeps a single row and does not model paused wall-clock time. A restored
 * session therefore resumes as if it had run continuously from [startMillis].
 *
 * @param startMillis wall-clock start of the session, milliseconds since the Unix epoch.
 * @param durationMinutes configured session length, in minutes.
 */
data class InProgress(
    val startMillis: Long,
    val durationMinutes: Int,
)

/**
 * Persistence boundary for focus sessions (ADR-0002).
 *
 * Implemented by Room in the `data` layer; the domain depends only on this interface so the
 * domain stays framework-free. Only completed sessions reach [recordCompleted]; at most one
 * [InProgress] snapshot exists at a time.
 */
interface SessionRepository {

    /** Appends a finished session to the completed-session history. */
    suspend fun recordCompleted(record: SessionRecord)

    /** Completed sessions, oldest first, as they change. */
    fun completedSessions(): Flow<List<SessionRecord>>

    /**
     * Stores (or replaces) the single in-progress snapshot so an uncompleted session survives
     * process death.
     */
    suspend fun saveInProgress(startMillis: Long, durationMinutes: Int)

    /** The current in-progress snapshot, or `null` when there is none, as it changes. */
    fun inProgress(): Flow<InProgress?>

    /** Removes the in-progress snapshot after it completes or is discarded. */
    suspend fun clearInProgress()
}
