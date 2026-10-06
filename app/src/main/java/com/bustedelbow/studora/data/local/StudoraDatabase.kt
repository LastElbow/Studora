package com.bustedelbow.studora.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bustedelbow.studora.data.repository.RoomSessionRepository
import com.bustedelbow.studora.domain.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The single Room database (ADR-0002).
 *
 * Version 1 is the first schema, so there is deliberately no migration path. The schema is not
 * exported: no consumer reads it yet.
 */
@Database(
    entities = [SessionEntity::class, InProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StudoraDatabase : RoomDatabase() {

    /** Session history + in-progress access. */
    abstract fun sessionDao(): SessionDao

    companion object {
        /** On-disk database file name. */
        const val DATABASE_NAME: String = "studora.db"
    }
}

/**
 * Hilt bindings for the Room persistence stack.
 *
 * Kept beside the database definition so the composition wiring lives with the thing it wires.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStudoraDatabase(
        @ApplicationContext context: Context,
    ): StudoraDatabase =
        Room.databaseBuilder(context, StudoraDatabase::class.java, StudoraDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideSessionDao(database: StudoraDatabase): SessionDao = database.sessionDao()

    @Provides
    @Singleton
    fun provideSessionRepository(dao: SessionDao): SessionRepository = RoomSessionRepository(dao)
}
