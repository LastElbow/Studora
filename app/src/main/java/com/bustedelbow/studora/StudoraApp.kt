package com.bustedelbow.studora

import android.app.Application
import com.bustedelbow.studora.domain.Clock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application entry point and Hilt composition root (ADR-0001).
 *
 * Hilt builds the injector from this class; the manifest binds it via `android:name`.
 */
@HiltAndroidApp
class StudoraApp : Application()

/**
 * Supplies the production [Clock] for the domain timer seam.
 *
 * The domain deliberately never reads the wall clock itself; tests swap this binding for a
 * deterministic fake by constructing [TimerViewModel] directly. Because this is the composition
 * root rather than testable logic, the single real `System.currentTimeMillis()` call is allowed
 * here.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemWallClock
}

private object SystemWallClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
