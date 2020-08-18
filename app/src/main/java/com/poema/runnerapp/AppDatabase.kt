package com.poema.runnerapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Track::class, LocationObject::class], version = 45)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao() : LocationDao
}
