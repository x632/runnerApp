package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var myUserUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            myUserUid = auth.currentUser!!.uid

        }else{
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    myUserUid = if (task.isSuccessful) {
                        println("!!! signInAnonymously:success")
                        val user = auth.currentUser
                        auth.currentUser?.uid!!

                    } else {
                        // If sign in fails, display a message to the user.
                        println("SignInAnonymously:failure")
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                        ""
                    }


                }
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
    override fun onBackPressed() {

    }
}
