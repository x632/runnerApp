package com.poema.runnerapp

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.type.Date
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

data class Map (
    var id: String? = null,
    var length : Double? = null,
    var name: String? = null,
    var time : String? = null,
    var timeStamp : String? = null)
{
}
