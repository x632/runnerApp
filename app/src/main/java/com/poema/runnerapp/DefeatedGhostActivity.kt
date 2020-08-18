package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class DefeatedGhostActivity : AppCompatActivity(), CoroutineScope {


    private var downloadedLocObjects = mutableListOf<LocationObject>()
    private lateinit var job : Job
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var db : AppDatabase
    private var docUid : Long = 0
    private var position = 0
    private var oldTrackId : Long = 0
    lateinit var a : Track
    private var trackName : String = ""
    private var timeUnit : Int = 0
    private var distance : Double = 0.0
    private var cancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defeated_ghost)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)

        //Värdena som tas emot
        timeUnit = intent.getIntExtra("Time", 0)
        distance = intent.getDoubleExtra("Distance", 0.0)
        docUid = intent.getLongExtra("docUid",0)
        position = intent.getIntExtra("posit",0)
        val size = ObjectDataManager.locationObjects.size
        a = Datamanager.tracks[position]
        oldTrackId = (a.trackId)

        println("!!! Positionen ${Datamanager.tracks[position]} och index är $size och id:t $oldTrackId")


        val resultTimeText = makeTimeStr(timeUnit)
        val saveButton = findViewById<Button>(R.id.save)
        val cancelButton = findViewById<Button>(R.id.cancelBtn)
        val timeText = findViewById<TextView>(R.id.timeValue)
        val lengthText = findViewById<TextView>(R.id.textView4)
        lengthText.text = String.format("%.0f", distance) + " meters"
        val trackNa = findViewById<TextView>(R.id.tvTrackName)
        trackNa.text = Datamanager.tracks[position].name
        timeText.text = resultTimeText
        trackName =  Datamanager.tracks[position].name


        //Savebutton
        saveButton.setOnClickListener {
            eraseLocationObjects(oldTrackId) //raderar gamla banan och dess locations gär sedan vidare till tracks
        }
        // Cancelbutton
        cancelButton.setOnClickListener {
            cancelled = true
            eraseLocationObjects(docUid)
        }

    }

    private fun moveIntoFunc(){
        val intent = Intent(this, TracksActivity::class.java)
        intent.putExtra("name2", trackName)
        intent.putExtra("time2", timeUnit)
        intent.putExtra("distance2", distance)
        intent.putExtra("docUi", docUid)
        startActivity(intent)
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

    private fun goHome(){
        println("Detta borde vara den sista texten!!")
        Toast.makeText(
            applicationContext, "Your recorded track has been deleted.",
            Toast.LENGTH_LONG).show(); goToStartPage()
    }
    private fun goToStartPage(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    fun eraseLocationObjects(id : Long) {

        val allLocObj = loadLocationObjectsByTrack(id)
        launch {
            allLocObj.await().forEach {
                downloadedLocObjects.add(it)
            }
            for (locationObject in downloadedLocObjects) {
                deleteLocationObject(locationObject)
            }
            findTrackToDelete(id)
        }
    }

    fun findTrackToDelete(id : Long){
        var track: Track
        async(Dispatchers.IO) {
            track = db.locationDao().findTrackById(id)
            println("!!!Track located!!")
            db.locationDao().delete(track)
            println("!!!Track deleted!!")
            switchToMain()
        }
    }
    fun deleteLocationObject(locationObject: LocationObject) {
        async(Dispatchers.IO) {   db.locationDao().delete(locationObject)
            println("!!!LocationObject deleted!!")
        }
    }
    //laddar ner LocObj för tracken i ordning(!!)
    fun loadLocationObjectsByTrack(locObjTrackId: Long) : Deferred<List<LocationObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findLocObjectsByTrackId(locObjTrackId)
        }

    private suspend fun switchToMain(){
        withContext(Dispatchers.Main){
            if (cancelled) {
                    goHome()
                }
                else{
                    moveIntoFunc()
                }
        }
    }


}
