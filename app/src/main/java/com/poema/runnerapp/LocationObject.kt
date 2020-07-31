package com.poema.runnerapp

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class LocationObject(
    @PrimaryKey(autoGenerate = true) val locObjId: Long,
    val locObjTrackId: Long,
    val accDistance: Double,
    val locLat: Double,
    val locLng: Double,
    val time: Int
)