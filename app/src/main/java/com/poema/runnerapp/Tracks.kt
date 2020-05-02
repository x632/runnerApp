package com.poema.runnerapp

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.type.Date
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries.offset


class Tracks : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    lateinit var recyclerView: RecyclerView
    var createdTrack: Boolean = false
    var adapter: MapRecycleAdapter? = null
    var docUid = ""
    var myUserUid = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            myUserUid = auth!!.currentUser!!.uid
        }
        val timeUnit = intent.getIntExtra("time", -1)
        val name = intent.getStringExtra("name")
        val timestr = makeTimeStr(timeUnit)

        if(timeUnit >= 0){
            createdTrack = true
        }

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MapRecycleAdapter(this, Datamanager.maps, myUserUid)

        //koppla ihop vår adapter med recyclerview:n
        recyclerView.adapter = adapter

        if (createdTrack){
            val timeStamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(UTC).format(Instant.now())

            println("!!! Klockan är : ${timeStamp}")
            val a = Map("", name, 5.5, timestr, timeStamp)

            db.collection("users").document(myUserUid).collection("maps").add(a)
                .addOnSuccessListener {uid->
                    docUid = uid.id
                    getData()
                }
                .addOnFailureListener {
                    println("!!!Dokumentet sparades INTE!")
                }

        }

        if(!createdTrack) {
        if (Datamanager.maps.size == 0){
            getDataWithoutAdding()}
        }
    }


    fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        val resultText = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
        return resultText
    }
    fun getData(){
        println("!!! varit i getDataWITHadding")
        val docRef = db.collection("users").document(myUserUid).collection("maps").orderBy("timeStamp",
            Query.Direction.DESCENDING)
        docRef.get().addOnSuccessListener { documentSnapshot ->
            Datamanager.maps.clear()
            for (document in documentSnapshot.documents) {
                val newMap = document.toObject(Map::class.java)

                if (newMap != null) {
                    newMap.id = (document.id)
                    Datamanager.maps.add(newMap)
                }
                adapter!!.notifyDataSetChanged()
            }
        }
    }
    fun getDataWithoutAdding(){
        println("!!! varit i getDataWithoutAdding")
        val docRef = db.collection("users").document(myUserUid).collection("maps").orderBy("timeStamp", Query.Direction.DESCENDING)
        docRef.get().addOnSuccessListener { documentSnapshot ->
            for (document in documentSnapshot.documents) {
                val newMap = document.toObject(Map::class.java)
                if (newMap != null) {
                    newMap.id = (document.id)
                    Datamanager.maps.add(newMap)
                }
                adapter!!.notifyDataSetChanged()
            }
        }


    }
}