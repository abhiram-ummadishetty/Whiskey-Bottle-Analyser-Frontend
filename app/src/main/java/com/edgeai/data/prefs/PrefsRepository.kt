package com.edgeai.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefsStore by preferencesDataStore(name = "edgeai_prefs")

class PrefsRepository(private val ctx: Context) {
    private val backendKey = stringPreferencesKey("backend_url")

    // Per-launch URL is held in memory; persisted value is just the last
    // successful one so the input can pre-fill on next launch.
    @Volatile private var active: String? = null

    val backendUrl: Flow<String?> = ctx.prefsStore.data.map { active ?: it[backendKey] }

    val lastBackendUrl: Flow<String?> = ctx.prefsStore.data.map { it[backendKey] }

    suspend fun setActive(url: String) {
        active = url.trimEnd('/')
        ctx.prefsStore.edit { it[backendKey] = active!! }
    }

    fun clearActive() { active = null }

    fun activeNow(): String? = active
}