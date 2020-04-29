package com.poema.runnerapp

import android.content.ClipData
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import kotlin.coroutines.CoroutineContext



class Tracks : AppCompatActivity(), CoroutineScope {

    private lateinit var job : Job

    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job

    lateinit var recyclerView: RecyclerView

    lateinit var db : AppDatabase

    var createdTrack : Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)
        job = Job()

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "Track-maps")
            .fallbackToDestructiveMigration()
            .build()

        val timeUnit = intent.getIntExtra("time",-1 )
        val name = intent.getStringExtra("name")
        val timestr = makeTimeStr(timeUnit)

        if (timeUnit >= 0) { createdTrack = true}

        if (createdTrack) {
            val a = Map(0, name, 5.5, timestr)
            saveMap(a)
        }
           launch(Dispatchers.IO) {

                val maps = loadAllMaps()
                maps.await().forEach { map ->
                        Datamanager.maps.add(map)

                    println("!!! Namn: ${map.name} ID: ${map.id}  Längd: ${map.length} Tid: ${map.time}")
                }
               // vill ha in en uppdaterare av recyclerviewn här
            }

      sleep(500)
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // konstruera en adapter från vår adapter-klass, skicka med vår data
        val adapter = MapRecycleAdapter(this, Datamanager.maps)

        //koppla ihop vår adapter med recyclerview:n
        recyclerView.adapter = adapter

    }
    override fun onResume(){
        super.onResume()
       recyclerView.adapter?.notifyDataSetChanged()
    }
    fun saveMap(map: Map) {
        async(Dispatchers.IO) {   db.mapDao().insert(map) }
    }
    fun deleteMap(map: Map) {
        async(Dispatchers.IO) {   db.mapDao().delete(map) }
    }
    fun loadAllMaps() : Deferred<List<Map>> =
        async(Dispatchers.IO) {
            db.mapDao().getAll()
        }

    fun loadByCategory(category: String) :  Deferred<List<Map>>  =
        async(Dispatchers.IO) {
            db.mapDao().findByCategory(category)
        }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    fun makeTimeStr(timeUnit: Int) : String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        val resultText = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
        return resultText
    }
}

