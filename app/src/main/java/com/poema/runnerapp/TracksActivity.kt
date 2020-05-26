package com.poema.runnerapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*


class TracksActivity : AppCompatActivity() {

    var mapsUidIndex = -1
    private var idList = mutableListOf<String>()
    lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    lateinit var recyclerView: RecyclerView
    var createdTrack: Boolean = false
    var adapter: MapRecycleAdapter? = null
    var docUid = ""
    var myUserUid = ""

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
        val distance = intent.getDoubleExtra("distance2", 0.0)
        val timestr = makeTimeStr(timeUnit)
        val docUid2 = intent.getStringExtra("docUi")
        //val eraseSignal = intent.getBooleanExtra("erase",false)
        //val position = intent.getIntExtra("posit", 0)

        if (timeUnit >= 0) {
            createdTrack = true
        }


        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MapRecycleAdapter(this, Datamanager.maps, myUserUid)

        recyclerView.adapter = adapter


        if (createdTrack) {

            val myDate = getCurrentDateTime()
            val timeStamp = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")

            //updaterar mappen som skapades som tom tidigare, med de rätta värdena.
            db.collection("users").document(myUserUid).collection("maps").document(docUid2!!)
                .update("id", "")
                .addOnSuccessListener {
                    println("!!! id:t uppdaterades på firestore")
                }
                .addOnFailureListener {
                    println("!!! id:t uppdaterades INTE!")
                }
            db.collection("users").document(myUserUid).collection("maps").document(docUid2)
                .update("name", name)
                .addOnSuccessListener {
                    println("!!! name uppdaterades på firestore")
                }
                .addOnFailureListener {
                    println("!!! name uppdaterades INTE!")
                }
            db.collection("users").document(myUserUid).collection("maps").document(docUid2)
                .update("length", distance)
                .addOnSuccessListener {
                    println("!!! length uppdaterades på firestore")
                }
                .addOnFailureListener {
                    println("!!! length uppdaterades INTE!")
                }
            db.collection("users").document(myUserUid).collection("maps").document(docUid2)
                .update("time", timestr)
                .addOnSuccessListener {
                    println("!!! time uppdaterades på firestore")
                }
                .addOnFailureListener {
                    println("!!! time uppdaterades INTE!")
                }
            db.collection("users").document(myUserUid).collection("maps").document(docUid2)
                .update("timeStamp", timeStamp)
                .addOnSuccessListener {
                    println("!!! timeStamp uppdaterades på firestore")
                    getData()
                }
                .addOnFailureListener {
                    println("!!! timeStamp uppdaterades INTE!")
                }
        }

        if (!createdTrack) {
            if (Datamanager.maps.size == 0)
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
        //val resultText = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }

    //laddar ner alla maps och tar in deras uid:n och lägger till dem i Datamanager.
    fun getData() { //tar ner alla maps
        println("!!! Varit i getData!!")
        val docRef = db.collection("users").document(myUserUid).collection("maps").orderBy(
            "timeStamp", Query.Direction.DESCENDING
        )
        docRef.get().addOnSuccessListener { documentSnapshot ->
            Datamanager.maps.clear()
            for (document in documentSnapshot.documents) {
                val newMap = document.toObject(Map::class.java)

                if (newMap != null) {
                    newMap.id = (document.id)           //Lägger till alla mapsen i datamanager med firestore id
                    Datamanager.maps.add(newMap)
                }
            }
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

    }
}