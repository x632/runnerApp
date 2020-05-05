package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class NamingTrack : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_track)

        val timeUnit = intent.getIntExtra("Time",0 )
        val resultTimeText = makeTimeStr(timeUnit)
        val saveButton = findViewById<Button>(R.id.save)
        val timeText= findViewById<TextView>(R.id.timeValue)
        timeText.text = resultTimeText

        saveButton.setOnClickListener {
            val intent = Intent(this, TracksActivity::class.java)
            val trackName = getName()
            intent.putExtra("name", trackName)
            intent.putExtra("time", timeUnit)
            startActivity(intent)
        }
    }
    fun getName():String{
        val answerText = findViewById<EditText>(R.id.trackName)
        val trackName = answerText.text.toString()
        return trackName
    }

    fun makeTimeStr(timeUnit: Int) : String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        val resultText = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
        return resultText
    }
}
