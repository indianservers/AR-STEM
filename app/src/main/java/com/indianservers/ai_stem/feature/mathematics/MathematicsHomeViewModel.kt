package com.indianservers.ai_stem.feature.mathematics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.ai_stem.data.workspace.WorkspacePreferencesRepository
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MathematicsHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = WorkspacePreferencesRepository(application)

    val environmentMode: StateFlow<WorkspaceEnvironmentMode> = preferences.environmentMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkspaceEnvironmentMode.White
    )

    fun selectEnvironment(mode: WorkspaceEnvironmentMode) {
        viewModelScope.launch { preferences.setEnvironmentMode(mode) }
    }
}
