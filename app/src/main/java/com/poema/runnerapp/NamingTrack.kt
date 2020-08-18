package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class NamingTrack : AppCompatActivity() , CoroutineScope {


    private var trackId : Long = 0
    private lateinit var job : Job
    private lateinit var db : AppDatabase
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    private var downloadedLocObjects = mutableListOf<LocationObject>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_track)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)

        val timeUnit = intent.getIntExtra("Time", 0)
        val distance: Double = intent.getDoubleExtra("Distance", 0.0)
        trackId = intent.getLongExtra("docUid",0)
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
            intent.putExtra("docUi", trackId)//trackId

            startActivity(intent)
        }
        cancelButton.setOnClickListener {
            findTrackToDelete()

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
            applicationContext,
            "You cannot go back here. You can always delete your track on the next page.",
            Toast.LENGTH_LONG
        ).show();
    }

    private fun eraseLocationObjects() {

                val allLocObj  = loadLocationObjectsByTrack(trackId)
                launch {
                    allLocObj.await().forEach {
                        downloadedLocObjects.add(it)
                    }
                    for (locationObject in downloadedLocObjects){
                        deleteLocationObject(locationObject)
                    }
                    switchToMain()
                }
    }

    fun goHome(){
        Toast.makeText(
            applicationContext, "Your recording has been deleted.",
            Toast.LENGTH_LONG).show(); goToStartPage()
    }
    fun goToStartPage(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    fun loadLocationObjectsByTrack(locObjTrackId: Long) : Deferred<List<LocationObject>> =
    async(Dispatchers.IO) {
        db.locationDao().findLocObjectsByTrackId(locObjTrackId)

    }
    fun deleteLocationObject(locationObject: LocationObject) {
        async(Dispatchers.IO) {   db.locationDao().delete(locationObject)
            println("!!!LocationObject with id: ${locationObject.locObjId} and track ID: ${locationObject.locObjTrackId} deleted!")
        }
    }

    private fun findTrackToDelete(){
        var track: Track
        async(Dispatchers.IO) {
            track = db.locationDao().findTrackById(trackId)
            db.locationDao().delete(track)
            println("!!!Track with ID ${track.trackId} located and deleted!!")
            eraseLocationObjects()
        }
    }
    private suspend fun switchToMain(){
        withContext(Dispatchers.Main){
            onPause()
            goHome()
        }
    }



}