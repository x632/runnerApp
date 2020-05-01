package com.poema.runnerapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

data class Map (
    var id: String? = null,
    var name: String? = null,
    var length : Double? = null,
    var time : String? = null){

}
