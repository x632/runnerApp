package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)

        val prevTracksButton = findViewById<Button>(R.id.loginButton)
        button.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
        prevTracksButton.setOnClickListener {
            val intent = Intent(this, TracksActivity::class.java)
            startActivity(intent)
        }

    }
    override fun onBackPressed() {

    }
}
