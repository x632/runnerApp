package com.poema.runnerapp

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DefeatedGhostActivity : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var myUserUid = ""
    private lateinit var docUid : String
    var position = 0
    private var oldMapId = ""
    lateinit var a : Map

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defeated_ghost)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            myUserUid = auth.currentUser!!.uid
        }

        val timeUnit = intent.getIntExtra("Time", 0)
        val distance: Double = intent.getDoubleExtra("Distance", 0.0)
        docUid = intent.getStringExtra("docUid")!!
        val index = intent.getIntExtra("ind", 0)
        position = intent.getIntExtra("posit",0)
        val size = ObjectDataManager.locationObjects.size
        a = Datamanager.maps[position]
        // Obs här finns det som behövs
        if (a.id != null) {
            oldMapId = (a.id!!)
        }
        println("!!! Positionen ${Datamanager.maps[position]} och index är $size och id:t $oldMapId")


        val resultTimeText = makeTimeStr(timeUnit)
        val saveButton = findViewById<Button>(R.id.save)
        val cancelButton = findViewById<Button>(R.id.cancelBtn)
        val timeText = findViewById<TextView>(R.id.timeValue)
        val lengthText = findViewById<TextView>(R.id.textView4)
        lengthText.text = String.format("%.0f", distance) + " meters"
        val trackNa = findViewById<TextView>(R.id.tvTrackName)
        trackNa.text = Datamanager.maps[position].name
        timeText.text = resultTimeText
        val trackName =  Datamanager.maps[position].name


        //Savebutton
        saveButton.setOnClickListener {
            deleteOldMapObjects(size)
            val intent = Intent(this, TracksActivity::class.java)
            intent.putExtra("name2", trackName)
            intent.putExtra("time2", timeUnit)
            intent.putExtra("distance2", distance)
            intent.putExtra("docUi", docUid)
            startActivity(intent)
        }
        // Cancelbutton
        cancelButton.setOnClickListener {
            eraseMapObjects(index)
            goHome()
        }
    }


    private fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }

    override fun onBackPressed() {
        Toast.makeText(
            applicationContext,
            "You cannot go back here. You can always delete your track on the next page, or press 'cancel'.",
            Toast.LENGTH_LONG
        ).show()
    }
    //raderar gamla banans locationsobjects
    private fun deleteOldMapObjects(index: Int) {
        val b = Datamanager.maps[position].id!!
        for (i in 1..index) {
            db.collection("users").document(myUserUid).collection("maps").document(b).collection("mapObjects").document("$i")
                .delete().addOnSuccessListener {
                    Log.d(ContentValues.TAG, "!!! Document successfully deleted!")
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "!!! Error deleting document", e)
                }


        }
        eraseTrack(b)

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
        eraseTrack(docUid)
    }
    private fun eraseTrack(doc:String){
        db.collection("users").document(myUserUid).collection("maps").document(doc).delete()
            .addOnSuccessListener {
                println("!!! Tom bana raderades från firestore")
                //goHome()
            }
            .addOnFailureListener {
                println("!!! Tomma banan raderades INTE!")
            }
    }
    private fun goHome(){
        Toast.makeText(
            applicationContext, "Your recording has been deleted.",
            Toast.LENGTH_LONG).show(); goToStartPage()
    }
    private fun goToStartPage(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}
