package com.poema.runnerapp

import android.content.Context
import androidx.room.Room

object DatabaseSource {
    private lateinit var INSTANCE: AppDatabase

    fun getInstance(context: Context): AppDatabase {
        if (!::INSTANCE.isInitialized) {
            synchronized(AppDatabase::class) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java,
                        "tracksRoom").fallbackToDestructiveMigration().build()
                }
            }
        }
        return INSTANCE
    }

}