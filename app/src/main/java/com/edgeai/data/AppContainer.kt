package com.edgeai.data

import android.content.Context
import androidx.room.Room
import com.edgeai.data.db.AppDatabase
import com.edgeai.data.db.UserRepository
import com.edgeai.data.prefs.PrefsRepository
import com.edgeai.data.prefs.SessionRepository
import com.edgeai.net.InferenceClient

class AppContainer private constructor(ctx: Context) {
    private val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "edgeai.db").build()
    val users = UserRepository(db.userDao())
    val prefs = PrefsRepository(ctx)
    val session = SessionRepository(ctx)
    val inference = InferenceClient()

    companion object {
        @Volatile private var INSTANCE: AppContainer? = null
        fun get(ctx: Context): AppContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}