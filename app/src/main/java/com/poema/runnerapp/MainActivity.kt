package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    private var myUserUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            myUserUid = auth!!.currentUser!!.uid
        }

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
}
