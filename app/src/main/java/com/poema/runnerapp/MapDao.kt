package com.poema.runnerapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query



@Dao
interface MapDao {

    @Insert
    fun insert(map: Map)

    @Delete
    fun delete(map: Map)

    @Query("SELECT * FROM map")
    fun getAll() : List<Map>

    @Query("SELECT * FROM map WHERE time LIKE :categoryName")
    fun findByCategory(categoryName: String) : List<Map>
}