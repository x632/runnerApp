package com.poema.runnerapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext


class TracksActivity : AppCompatActivity(), CoroutineScope {

    var pos = 0
    var downloadedLocObjects = mutableListOf<LocationObject>()
    private lateinit var job : Job
    private lateinit var db : AppDatabase
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    lateinit var recyclerView: RecyclerView
    var createdTrack: Boolean = false
    var adapter: MapRecycleAdapter? = null
    var trackId : Long = 0
    var name = ""
    var nam = ""
    var oldTrackId : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)
        // Dessa tas emot från antingen namingtrackAct eller defeatedghostAct
        val timeUnit = intent.getIntExtra("time2", -1)              //tiden i sekunder (INT)
        val name = intent.getStringExtra("name2")                           //namn på banan (String)
        val distance = intent.getDoubleExtra("distance2", 0.0)  //längden på banan (Double)
        val timestr = makeTimeStr(timeUnit)
        oldTrackId = intent.getLongExtra("oldTrackId",0)
        trackId = intent.getLongExtra("docUi",0) //trackID (nya banan)

        println("!!!OldTrackId från tracksActivity : $oldTrackId")
        if (oldTrackId > 0){      // är lika med : kommit från defeatedghostactivity
            loadAtt()
        }
        println("namn: $name från Tracksactivity")
        if (timeUnit >= 0) {
            createdTrack = true
        }


        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MapRecycleAdapter(this, Datamanager.tracks)

        recyclerView.adapter = adapter


        if (createdTrack) {

            val myDate = getCurrentDateTime()
            val timeStamp = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")
            nam = name ?: ""
            val newAttempt = AttemptObject(0,trackId, timeUnit, distance,true,timeStamp)
            updateTrack(trackId, distance, nam, timestr, timeStamp, newAttempt) //and save attemptObject
        }

        if (!createdTrack) {
            if (Datamanager.tracks.size == 0)
                getData()
        }
    }

    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }
    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }
    fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }

    //laddar ner alla tracks och lägger till dem i Datamanager i ordning(!!).
    fun getData() { //tar ner alla tracks
        Datamanager.tracks.clear()
        val allTracks  = loadAllTracks()
        launch {
            allTracks.await().forEach {
                Datamanager.tracks.add(it)
                notifyOnMain()
            }
          
        }
    }
    suspend fun notifyOnMain(){
        withContext(Main){adapter!!.notifyDataSetChanged()}
    }

    fun loadAllTracks() : Deferred<List<Track>> =
        async(Dispatchers.IO) {
            db.locationDao().getAllTracksInOrder()
        }

    private fun updateTrack(trackNumber : Long, distance : Double, name : String, time : String, timeStamp: String, newAttempt : AttemptObject) {
        async(Dispatchers.IO) {
            db.locationDao().updateTrackLength(trackNumber, distance)
            db.locationDao().updateTrackTime(trackNumber, time)
            db.locationDao().updateTrackName(trackNumber, name)
            db.locationDao().updateTrackTimestamp(trackNumber, timeStamp)
            db.locationDao().insert(newAttempt)
            println("!!!Trackfields updated and new attempt saved!!")
            getData()
        }
    }

    override fun onBackPressed() {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

    }
    fun removeTrackAndLocObjects (id : Long , position: Int){
        pos = position
        eraseLocationObjects(id)


    }
    fun eraseLocationObjects(id : Long) {

        val allLocObj = loadLocationObjectsByTrack(id)
        launch {
            allLocObj.await().forEach {
                downloadedLocObjects.add(it)

            }
            for (locationObject in downloadedLocObjects) {
                println(locationObject)
                deleteLocationObject(locationObject)
            }
            findTrackToDelete(id) //completionHandler efter radering av locobj
        }
    }
    fun deleteLocationObject(locationObject: LocationObject) {
        async(Dispatchers.IO) {   db.locationDao().delete(locationObject)
            println("!!!LocationObject deleted!!")
        }
    }
    fun loadLocationObjectsByTrack(locObjTrackId: Long) : Deferred<List<LocationObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findLocObjectsByTrackId(locObjTrackId)
        }
    fun deleteTrack(track: Track) {
        async(Dispatchers.IO) {   db.locationDao().delete(track)
            println("!!!Track deleted!!")
            Datamanager.tracks.removeAt(pos)
            adapter!!.notifyDataSetChanged()
        }
    }
    fun findTrackToDelete(id : Long){
        var track: Track
        async(Dispatchers.IO) {
            track = db.locationDao().findTrackById(id)

            println("!!!Track located!!")
            deleteTrack(track)
        }
    }
    private fun loadAtt(){
        println("!!! från loadAtt funktionen före loopen Tracks")
        val selAttObj  = loadAttemptObjectsByTrack(oldTrackId)
        launch {
            selAttObj.await().forEach {
                println("!!! $it")
                val oldId = it.aoTrackId
                updateAttObject(trackId,oldId)
                println("!!!AoTrackId for ${it.aoId} updated to trackId $trackId!!!")
            }
            switchToMain2()
        }
    }
    fun loadAttemptObjectsByTrack(aoOldTrackId: Long) : Deferred<List<AttemptObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findAttemptObjectsByTrackId(aoOldTrackId)
        }
    private suspend fun switchToMain2(){
        withContext(Dispatchers.Main){
            println("!!!Varit i c handlern efter att ha uppdaterat aotrackId..forts sedan förhoppningsvis onCreate ")
        }
    }
    private fun updateAttObject(trackId: Long, oldId: Long) {
        async(Dispatchers.IO) {
            db.locationDao().updateAttObjTrackId(trackId,oldId)
            println("!!! AObjectupdated")
        }
    }
}




