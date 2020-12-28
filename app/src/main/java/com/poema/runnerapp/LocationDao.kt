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
    fun insert(attemptObject: AttemptObject) : Long

    @Insert
    fun insert(locationObject : LocationObject): Long

    @Delete
    fun delete(track: Track)

    @Delete
    fun delete(locationObject: LocationObject)

    @Delete
    fun delete(attemptObject: AttemptObject)

    @Query("SELECT * FROM track ORDER BY trackId DESC")
    fun getAllTracksInOrder() : List<Track>

    @Query("SELECT * FROM LocationObject")
    fun getAllLocationObjects() : List<LocationObject>

    @Query("SELECT * FROM Track WHERE trackId LIKE :docUid")
    fun findTrackById(docUid: Long) : Track

    @Query("SELECT * FROM LocationObject WHERE locObjTrackId LIKE :trackNumber ORDER BY locObjId ASC")
    fun findLocObjectsByTrackId(trackNumber: Long) : List<LocationObject>

    @Query("UPDATE Track SET name = :name WHERE trackID = :uid")
    fun updateTrackName(uid: Long, name: String )

    @Query("UPDATE Track SET length = :distance WHERE trackID = :uid")
    fun updateTrackLength(uid: Long, distance: Double )

    @Query("UPDATE Track SET time = :timestr WHERE trackID = :uid")
    fun updateTrackTime(uid: Long, timestr: String )

    @Query("UPDATE Track SET timestamp = :timestamp WHERE trackID = :uid")
    fun updateTrackTimestamp(uid: Long, timestamp: String )

    @Query("SELECT * FROM AttemptObject WHERE aoTrackId LIKE :trackNumber ORDER BY aoId ASC")
    fun findAttemptObjectsByTrackId(trackNumber: Long) : List<AttemptObject>

    @Query("UPDATE AttemptObject SET aoTrackId = :trackId WHERE aoTrackId = :oldTrackId")
    fun updateAttObjTrackId(trackId: Long, oldTrackId: Long )
}