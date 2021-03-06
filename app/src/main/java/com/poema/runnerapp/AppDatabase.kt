package com.poema.runnerapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Track::class, LocationObject::class, AttemptObject::class], version = 58)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao() : LocationDao
}
