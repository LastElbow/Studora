package com.bustedelbow.studora.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bustedelbow.studora.domain.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Owns the Settings screen's destructive "clear all study data" action.
 *
 * The erase itself lives in [SessionRepository] (one transaction over both tables); this
 * view-model only invokes it and announces completion through [cleared] so the screen can show a
 * confirmation. Resetting any live timer is the repository's `clears` signal, not this class's job.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    private val _cleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once after a successful clear, for a one-shot UI confirmation. */
    val cleared: SharedFlow<Unit> = _cleared.asSharedFlow()

    /** Deletes every completed session and the in-progress snapshot. */
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _cleared.emit(Unit)
        }
    }
}
