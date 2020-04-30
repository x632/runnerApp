package com.poema.runnerapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Map::class),version = 2)
abstract class AppDatabase :RoomDatabase(){
    abstract fun mapDao() : MapDao
}