package com.bustedelbow.studora.data.local

import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord

/**
 * Pure Room entity <-> domain mapping.
 *
 * No database, dispatcher, or clock is involved, so the mapping is trivially unit-testable and
 * the repository stays a thin translation layer.
 */

/** Maps a completed-session row to its domain record. */
fun SessionEntity.toDomain(): SessionRecord = SessionRecord(startMillis = startMillis, endMillis = endMillis)

/** Maps a completed-session domain record to its Room row. */
fun SessionRecord.toEntity(): SessionEntity = SessionEntity(startMillis = startMillis, endMillis = endMillis)

/** Maps the single in-progress row to its domain snapshot. */
fun InProgressEntity.toDomain(): InProgress = InProgress(startMillis = startMillis, durationMinutes = durationMinutes)

/** Maps an in-progress domain snapshot to its singleton Room row. */
fun InProgress.toEntity(): InProgressEntity =
    InProgressEntity(
        id = InProgressEntity.SINGLE_ROW_ID,
        startMillis = startMillis,
        durationMinutes = durationMinutes,
    )
