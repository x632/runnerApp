package com.poema.runnerapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AttemptObject(
    @PrimaryKey(autoGenerate = true)  val aoId: Long,           //id:t på AOobjektet
    val aoTrackId: Long,                                        //Id:t på den bana som den tillhör
    val aoTime: Int,                                            //antal sekunder (tiden helt enkelt)
    val aoLength: Double,                                       //Längden på banan
    val win: Boolean,                                           //Är det en vinst eller en förlust
    val aoTimestamp: String                                     //datum p å försöket (String!!!)
)
