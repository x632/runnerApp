package com.poema.runnerapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert
    fun insert(track: Track) : Long

    @Insert
    fun insert(locationObject : LocationObject)

    @Delete
    fun delete(track: Track)

    @Delete
    fun delete(locationObject: LocationObject)

    @Query("SELECT * FROM track ORDER BY trackId ASC")
    fun getAllTracks() : List<Track>

    @Query("SELECT * FROM LocationObject")
    fun getAllLocationObjects() : List<LocationObject>

    @Query("SELECT * FROM Track WHERE trackId LIKE :docUid")
    fun findTrackById(docUid: Long) : Track

    @Query("SELECT * FROM LocationObject WHERE locObjTrackId LIKE :trackNumber")
    fun findByTrack(trackNumber: Long) : List<LocationObject>
}