package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        val prevTracksButton = findViewById<Button>(R.id.previousTracks)
        button.setOnClickListener {
            val intent = Intent(this, RecordTrack::class.java)
            startActivity(intent)
        }
        prevTracksButton.setOnClickListener {
            val intent = Intent(this, Tracks::class.java)
            startActivity(intent)
        }


    }
}
