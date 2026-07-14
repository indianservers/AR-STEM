package com.indianservers.ai_stem.feature.games.multiplayer

import android.content.Context

class LocalPlayerProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ar_math_arena_profile", Context.MODE_PRIVATE)

    fun load(): LocalPlayerProfile {
        val fallback = LocalPlayerProfile()
        return LocalPlayerProfile(
            playerId = prefs.getString("playerId", fallback.playerId) ?: fallback.playerId,
            displayName = prefs.getString("displayName", fallback.displayName) ?: fallback.displayName,
            avatarSeed = prefs.getInt("avatarSeed", fallback.avatarSeed),
            preferredRole = runCatching {
                ArenaRole.valueOf(prefs.getString("preferredRole", fallback.preferredRole.name) ?: fallback.preferredRole.name)
            }.getOrDefault(fallback.preferredRole),
            lastTeamColor = prefs.getString("lastTeamColor", fallback.lastTeamColor) ?: fallback.lastTeamColor,
            highContrast = prefs.getBoolean("highContrast", false),
            largeText = prefs.getBoolean("largeText", false)
        )
    }

    fun save(profile: LocalPlayerProfile): LocalPlayerProfile {
        val safe = profile.copy(displayName = ArenaTextValidator.cleanDisplayName(profile.displayName))
        prefs.edit()
            .putString("playerId", safe.playerId)
            .putString("displayName", safe.displayName)
            .putInt("avatarSeed", safe.avatarSeed)
            .putString("preferredRole", safe.preferredRole.name)
            .putString("lastTeamColor", safe.lastTeamColor)
            .putBoolean("highContrast", safe.highContrast)
            .putBoolean("largeText", safe.largeText)
            .apply()
        return safe
    }
}
