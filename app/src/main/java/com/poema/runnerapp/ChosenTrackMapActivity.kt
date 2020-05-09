package com.poema.runnerapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChosenTrackMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    private var myUserUid = ""
    private var b = ""
    private val COLOR_GREEN_ARGB = -0xc771c4
    private val COLOR_RED_ARGB = -0xff000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choosen_track_map2)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val position = intent.getIntExtra("position",0)
        println("!!! positionnumber: $position")
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            myUserUid = auth!!.currentUser!!.uid
        }
        getLocationObjects(position)
    }
    fun getLocationObjects(position: Int){
        val a = Datamanager.maps[position]
        if (a.id != null) {
            b = (a.id!!)
        }                                    // uid:t på map:pen ifråga -> ladda ner collection av mapObjects som hör till den map:pen.

        val docRef1 = db.collection("users").document(myUserUid).collection("maps").document(b)
            .collection("mapObjects").orderBy(
                "time", Query.Direction.ASCENDING
            )
        docRef1.get().addOnSuccessListener { documentSnapshot ->
            ObjectDataManager.locationObjects.clear()                                //töm ObjectDatamanager...
            for (document in documentSnapshot.documents) {
                val newLocationObject = document.toObject(LocationObject::class.java)

                if (newLocationObject != null) {
                    newLocationObject.id =
                        (document.id)                         //....lägg sedan till dessa mapObjects (som kommer från firestore till objektdatamanager med firestore id
                    ObjectDataManager.locationObjects.add(newLocationObject)
                }
            }
            for (locationObject in ObjectDataManager.locationObjects) {
                println("!!! ${locationObject.time} sekunder, plats:${locationObject.locLatLng}")
            }
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
    private fun stylePolyline(polyline: Polyline) {
        var type = ""
        // Get the data object stored with the polyline.
        if (polyline.tag != null) {
            type = polyline.tag.toString()
        }
        when (type) {
            "A" ->                 // Use a custom bitmap as the cap at the start of the line.
            {
                polyline.color = COLOR_GREEN_ARGB
                polyline.startCap = RoundCap()
                polyline.endCap = RoundCap()
            }

            "B" -> {
                polyline.color = COLOR_RED_ARGB
                polyline.startCap = RoundCap()
                polyline.endCap = CustomCap(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)
                )
            }
        }
        polyline.width = 8.toFloat()
        polyline.jointType = JointType.ROUND
    }
    fun drawPolylines() {

        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(5f)
        for (locationObject in ObjectDataManager.locationObjects) {
            //convertering av geopoint här
            options.add(locationObject.locLatLng)                                     // är här!!!!
        }
        map.addPolyline(options)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(locationObject.locLatLng[0], 15f))
        }


        }

}

