package com.bustedelbow.studora.data

import com.bustedelbow.studora.data.local.InProgressEntity
import com.bustedelbow.studora.data.local.SessionEntity
import com.bustedelbow.studora.data.local.toDomain
import com.bustedelbow.studora.data.local.toEntity
import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Behaviour tests for the pure Room entity<->domain mapping seam.
 *
 * The mapper is deliberately side-effect free: these tests need no database, dispatcher, or
 * clock, and pin the round-trip identities the repository relies on.
 */
class SessionMapperTest {

    @Test
    fun `session record maps to an entity and back unchanged`() {
        val record = SessionRecord(startMillis = 1_000L, endMillis = 1_500_000L)

        val entity = record.toEntity()

        assertEquals(1_000L, entity.startMillis)
        assertEquals(1_500_000L, entity.endMillis)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `session entity maps to the domain record`() {
        val entity = SessionEntity(startMillis = 2_000L, endMillis = 3_000L)

        assertEquals(SessionRecord(startMillis = 2_000L, endMillis = 3_000L), entity.toDomain())
    }

    @Test
    fun `in-progress snapshot maps to a single-row entity and back unchanged`() {
        val inProgress = InProgress(startMillis = 42_000L, durationMinutes = 25)

        val entity = inProgress.toEntity()

        assertEquals(InProgressEntity.SINGLE_ROW_ID, entity.id)
        assertEquals(42_000L, entity.startMillis)
        assertEquals(25, entity.durationMinutes)
        assertEquals(inProgress, entity.toDomain())
    }

    @Test
    fun `in-progress entity maps to the domain snapshot`() {
        val entity =
            InProgressEntity(
                id = InProgressEntity.SINGLE_ROW_ID,
                startMillis = 7_000L,
                durationMinutes = 45,
            )

        assertEquals(InProgress(startMillis = 7_000L, durationMinutes = 45), entity.toDomain())
    }
}
