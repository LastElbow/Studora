package com.bustedelbow.studora.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for the single in-progress session snapshot (ADR-0002, table `in_progress`).
 *
 * Exactly one row is ever stored, pinned to [SINGLE_ROW_ID]; saving again replaces it.
 */
@Entity(tableName = "in_progress")
data class InProgressEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val startMillis: Long,
    val durationMinutes: Int,
) {
    companion object {
        /** The only primary-key value used; the snapshot is a singleton row. */
        const val SINGLE_ROW_ID: Int = 1
    }
}
