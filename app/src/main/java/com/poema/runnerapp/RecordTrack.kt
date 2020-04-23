package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*
import kotlin.concurrent.timer


class RecordTrack : AppCompatActivity() {

    var timerStarted = false
    var timerOn : Timer? = null
    var tenthSeconds = 0
    var seconds = 0
    var minutes = 0
    var hours = 0
    var strTenths = "0"
    var strSec = "00:"
    var strMin = "00:"
    var strHour = "00:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_track)
        val button = findViewById<Button>(R.id.stopbutton)
        button.setOnClickListener {
            if (timerOn != null) {
                startTimer(false)
            }
            val intent = Intent(this, NamingTrack::class.java)
            startActivity(intent)

        }
        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {
            startTimer(true)
        }
    }
    fun startTimer(pressedStart : Boolean) {
        if (pressedStart && !timerStarted) {
            timerOn = timer(period = 100) {
                tenthSeconds += 1
                if (tenthSeconds > 9) {
                    seconds += 1
                    tenthSeconds = 0
                    strTenths = ".$tenthSeconds"
                } else {
                    strTenths = ".$tenthSeconds"
                }
                if (seconds > 59) {
                    seconds = 0
                    minutes += 1
                }
                if (seconds > 9) {
                    strSec = ":$seconds"
                } else {
                    strSec = ":0$seconds"
                }
                if (minutes > 59) {
                    minutes = 0
                    hours += 1
                }
                if (minutes > 9) {
                    strMin = ":$minutes"
                } else {
                    strMin = ":0$minutes"
                }
                if (hours > 9) {
                    strHour = "$hours"
                } else {
                    strHour = "0$hours"
                }
                val timerText = findViewById<TextView>(R.id.timer)
                timerText.text = strHour + strMin + strSec + strTenths
                timerStarted = true
            }
        }
        else {
            timerOn?.cancel()
            timerStarted = false
        }

    }
}
