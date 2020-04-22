package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class NamingTrack : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_track)

        val button = findViewById<Button>(R.id.save)
        button.setOnClickListener {
            val intent = Intent(this, Tracks::class.java)
            startActivity(intent)
        }
    }
}
