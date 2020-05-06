package com.poema.runnerapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter



class TracksActivity : AppCompatActivity() {

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
        val timeUnit = intent.getIntExtra("time2", -1)
        val name = intent.getStringExtra("name2")
        val distance  = intent.getDoubleExtra("distance2", 0.0)
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

            val timeStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(ZoneOffset.ofHours(+2)).
                format(Instant.now())

            println("!!! Klockan är : ${timeStamp}")

            val a = Map("", distance, name, timestr, timeStamp)
            println("!!! $a")
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
            getData()}
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
  /*  fun getDataWithoutAdding(){
        println("!!! varit i getDataWithoutAdding")
        val docRef = db.collection("users").document(myUserUid).collection("maps").orderBy("timeStamp", Query.Direction.DESCENDING)
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


    }*/
    override fun onBackPressed() {
        if(!createdTrack) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}