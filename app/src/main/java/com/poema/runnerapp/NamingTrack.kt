package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class NamingTrack : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_track)

        val timeUnit = intent.getIntExtra("Time",0 )
        val distance : Double = intent.getDoubleExtra("Distance", 0.0)
        val docUid = intent.getStringExtra("docUid")
        val resultTimeText = makeTimeStr(timeUnit)
        val saveButton = findViewById<Button>(R.id.save)
        val timeText= findViewById<TextView>(R.id.timeValue)
        val lengthText = findViewById<TextView>(R.id.textView4)
        val cancelBtn = findViewById<Button>(R.id.cancel)
        lengthText.text = String.format("%.0f", distance)+" meters"
        timeText.text = resultTimeText

        saveButton.setOnClickListener {
            val intent = Intent(this, TracksActivity::class.java)
            val trackName = getName()
            intent.putExtra("name2", trackName)
            intent.putExtra("time2", timeUnit)
            intent.putExtra("distance2", distance)
            intent.putExtra("docUi", docUid)

            startActivity(intent)
        }
            cancelBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }
    override fun onBackPressed() {
            Toast.makeText(getApplicationContext(), "You cannot go back here. You can always delete your track on the next page. Or cancel",
                Toast.LENGTH_LONG).show();



    }
}
