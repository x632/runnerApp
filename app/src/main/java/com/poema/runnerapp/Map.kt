package com.poema.runnerapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
class Map (
    @PrimaryKey(autoGenerate=true)val id: Int,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "length") var length : Double,
    @ColumnInfo(name = "time") var time : String){

}
