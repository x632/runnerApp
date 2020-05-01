package com.poema.runnerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.concurrent.timer


class RecordTrack : AppCompatActivity() {

    var timerStarted = false
    var timerOn : Timer? = null
    var timeUnit = -1
    var uid : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_track)
        uid = intent.getStringExtra("fromStartPageToRecord")

        val stopButton = findViewById<Button>(R.id.stopbutton)
        stopButton.setOnClickListener {
            if (timerOn != null) {
                startTimer(false)
                val intent = Intent(this, NamingTrack::class.java)
                intent.putExtra("Time",timeUnit)
                intent.putExtra("fromRecordToNaming",uid)
                startActivity(intent)
            }
        }

        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {
            if (timerOn == null){
            val header = findViewById<TextView>(R.id.header)
            header.text = "Running.."
            startTimer(true)
            }
        }
    }
    fun startTimer(pressedStart : Boolean) {
        if (pressedStart && !timerStarted) {
            timerOn = timer(period = 1000) {

                timeUnit  ++

                val hours = timeUnit / 36000
                val minutes = timeUnit % 36000 / 60
                val seconds: Int = timeUnit % 60
                val strTime = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
               val timerText = findViewById<TextView>(R.id.timer)

                timerText.text = strTime
                timerStarted = true
            }
        }
        else {
            timerOn?.cancel()
            timerStarted = false
        }

    }

}
