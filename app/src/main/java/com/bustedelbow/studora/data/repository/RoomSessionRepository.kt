package com.bustedelbow.studora.data.repository

import com.bustedelbow.studora.data.local.SessionDao
import com.bustedelbow.studora.data.local.toDomain
import com.bustedelbow.studora.data.local.toEntity
import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [SessionRepository] (ADR-0002).
 *
 * A thin translation layer: the DAO does the I/O and [toDomain]/[toEntity] do the pure mapping,
 * so this class holds no business rules of its own.
 */
class RoomSessionRepository @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {

    private val clearsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val clears: Flow<Unit> = clearsFlow.asSharedFlow()

    override suspend fun recordCompleted(record: SessionRecord) {
        dao.insertCompleted(record.toEntity())
    }

    override fun completedSessions(): Flow<List<SessionRecord>> =
        dao.completedSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveInProgress(startMillis: Long, durationMinutes: Int) {
        dao.insertInProgress(
            InProgress(startMillis = startMillis, durationMinutes = durationMinutes).toEntity(),
        )
    }

    override fun inProgress(): Flow<InProgress?> =
        dao.inProgress().map { entity -> entity?.toDomain() }

    override suspend fun clearInProgress() {
        dao.clearInProgress()
    }

    override suspend fun clearAll() {
        dao.clearAll()
        clearsFlow.emit(Unit)
    }
}
