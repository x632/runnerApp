package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import java.util.*
import kotlin.concurrent.timer


class RecordTrack : AppCompatActivity() {

    var timerStarted = false
    var timerOn : Timer? = null
    var halfSeconds = 0
    var seconds = 0
    var minutes = 0
    var hours = 0
    var pressedStart : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_track)
        val button = findViewById<Button>(R.id.stopbutton)
        button.setOnClickListener {
            startTimer(false)
            val intent = Intent(this, NamingTrack::class.java)
            startActivity(intent)

        }
        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {
            startTimer(true)
        }
    }
    fun startTimer(pressedStart : Boolean) {
        if (pressedStart && timerStarted == false) {
                timerOn = timer(period = 1000) {
                println("sekunder: $seconds")
                seconds += 1
                timerStarted = true
            }
        } else {
          timerOn = null
            timerStarted = false
        }

    }
}
