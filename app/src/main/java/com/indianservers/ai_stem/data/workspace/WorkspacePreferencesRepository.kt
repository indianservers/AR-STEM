package com.indianservers.ai_stem.data.workspace

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.mathWorkspaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "math_workspace_preferences"
)

class WorkspacePreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.mathWorkspaceDataStore

    val environmentMode: Flow<WorkspaceEnvironmentMode> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[MODE_KEY]
                ?.let { saved -> WorkspaceEnvironmentMode.entries.firstOrNull { it.name == saved } }
                ?: WorkspaceEnvironmentMode.White
        }

    suspend fun setEnvironmentMode(mode: WorkspaceEnvironmentMode) {
        dataStore.edit { preferences -> preferences[MODE_KEY] = mode.name }
    }

    private companion object {
        val MODE_KEY = stringPreferencesKey("workspace_environment_mode")
    }
}
