package com.bustedelbow.studora.data.local

import androidx.room.Entity

/**
 * Room row for a completed focus session (ADR-0002, table `completed_sessions`).
 *
 * The domain [com.bustedelbow.studora.domain.SessionRecord] has only the two instants, so they
 * form the composite primary key; no synthetic id column is needed.
 */
@Entity(tableName = "completed_sessions", primaryKeys = ["startMillis", "endMillis"])
data class SessionEntity(
    val startMillis: Long,
    val endMillis: Long,
)
