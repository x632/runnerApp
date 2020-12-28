package com.poema.runnerapp

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Track (
    @PrimaryKey (autoGenerate = true) val trackId: Long,
    val length: Double,
    val name: String,
    val time: String,
    val timestamp: String
)