package com.poema.runnerapp

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NamingTrack : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    private var myUserUid = ""
    private lateinit var docUid : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_track)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            myUserUid = auth!!.currentUser!!.uid
        }

        val timeUnit = intent.getIntExtra("Time", 0)
        val distance: Double = intent.getDoubleExtra("Distance", 0.0)
        docUid = intent.getStringExtra("docUid")!!
        val index = intent.getIntExtra("ind", 0)
        val resultTimeText = makeTimeStr(timeUnit)
        val saveButton = findViewById<Button>(R.id.save)
        val cancelButton = findViewById<Button>(R.id.cancelBtn)
        val timeText = findViewById<TextView>(R.id.timeValue)
        val lengthText = findViewById<TextView>(R.id.textView4)
        lengthText.text = String.format("%.0f", distance) + " meters"
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
        cancelButton.setOnClickListener {
            eraseMapObjects(index)

        }
    }

    fun getName(): String {
        val answerText = findViewById<EditText>(R.id.trackName)
        val trackName = answerText.text.toString()
        return trackName
    }

    fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }

    override fun onBackPressed() {
        Toast.makeText(
            getApplicationContext(),
            "You cannot go back here. You can always delete your track on the next page.",
            Toast.LENGTH_LONG
        ).show();
    }

    private fun eraseMapObjects(index: Int) {

        for (i in 1..index) {
            db.collection("users").document(myUserUid).collection("maps").document(docUid)
                .collection("mapObjects").document("$i")
                .delete().addOnSuccessListener {
                    Log.d(ContentValues.TAG, "!!! Document successfully deleted!")
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "!!! Error deleting document", e)
                }

        }
        eraseTrack()
    }
    fun eraseTrack(){
        db.collection("users").document(myUserUid).collection("maps").document(docUid).delete()
            .addOnSuccessListener {
                println("!!! Tom bana raderades från firestore")
                onPause()
                goHome()
            }
            .addOnFailureListener {
                println("!!! Tomma banan raderades INTE!")
            }
    }
    fun goHome(){
        Toast.makeText(getApplicationContext(), "Your recording has been deleted.",
            Toast.LENGTH_LONG).show(); goToStartPage()
    }
    fun goToStartPage(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}