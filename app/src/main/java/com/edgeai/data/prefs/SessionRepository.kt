package com.edgeai.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore(name = "edgeai_session")

data class SessionUser(val id: Long, val firstName: String)

class SessionRepository(private val ctx: Context) {
    private val idKey = longPreferencesKey("user_id")
    private val nameKey = stringPreferencesKey("user_first")

    val user: Flow<SessionUser?> = ctx.sessionStore.data.map { p ->
        val id = p[idKey] ?: return@map null
        SessionUser(id, p[nameKey] ?: "")
    }

    suspend fun setUser(id: Long, firstName: String) {
        ctx.sessionStore.edit { it[idKey] = id; it[nameKey] = firstName }
    }

    suspend fun clear() {
        ctx.sessionStore.edit { it.clear() }
    }
}