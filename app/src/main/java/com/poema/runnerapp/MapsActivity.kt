package com.poema.runnerapp

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnPolylineClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*
import kotlin.concurrent.timer


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnPolylineClickListener,
    GoogleMap.OnMarkerClickListener  {

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }
    var myLocLatLngList = mutableListOf<LatLng>()
    private lateinit var map: GoogleMap
    private var timerStarted = false
    private var timerOn: Timer? = null
    private var timeUnit = -1
    private val COLOR_GREEN_ARGB = -0xc771c4
    private val COLOR_RED_ARGB = -0xff000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var totalDistance = 0.0
    private var distance : Float = 0.0f
    private var index : Int = 0
    private var location1 : Location? = null
    private var location2 : Location? = null
    lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    var docUid = ""
    private var myUserUid = ""
    private var havePressedStart = true
    private var havePressedStop = true


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()
        db.firestoreSettings = settings
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            myUserUid = auth.currentUser!!.uid
        }


                // ...



        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener {

            if (timerOn != null && havePressedStop == true) {
                havePressedStop = false
                startTimer(false)
                onPause()
                val intent = Intent(this, NamingTrack::class.java)
                intent.putExtra("Time", timeUnit)
                intent.putExtra("Distance", totalDistance)
                intent.putExtra("docUid", docUid)
                intent.putExtra("ind",index)
                startActivity(intent)
            }
        }

        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {

            if (timerOn == null && havePressedStart == true) {
                havePressedStart = false
                val myDate = getCurrentDateTime()
                val dateInString = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")

                val a = Map("", 0.0, "", "", dateInString)
                db.collection("users").document(myUserUid).collection("maps").add(a)
                    .addOnSuccessListener { uid ->
                        docUid = uid.id
                        startingFunction()
                        println("!!! Tom bana sparades!")
                    }
                    .addOnFailureListener {
                        println("!!! Tomma banan sparades INTE!")
                    }

            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                doSomethingWithLastLocation(lastLocation)
            }
        }
        createLocationRequest()
    }
    private fun getCurrentDateTime(): Date {
        return getInstance().time
    }
    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }
    private fun startingFunction (){
        val header = findViewById<TextView>(R.id.header)
        header.text = "Running.."
        startTimer(true)
        onResume()

    }

    private fun startTimer(pressedStart: Boolean) {
        if (pressedStart && !timerStarted) {
            timerOn = timer(period = 1000) {

                timeUnit++

                val hours = timeUnit / 36000
                val minutes = timeUnit % 36000 / 60
                val seconds: Int = timeUnit % 60
                val strTime = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
                val timerText = findViewById<TextView>(R.id.timer)

                timerText.text = strTime
                timerStarted = true
            }
        } else {
            timerOn?.cancel()
            timerStarted = false
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        setUpMap()
    }
    private fun startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }
    private fun createLocationRequest() {
       // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 5000
        // 3
        locationRequest.fastestInterval = 4000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            if(timerOn != null){
            startLocationUpdates()}

        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {

                try {

                    e.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
               locationUpdateState = true
                if(timerOn != null){
                startLocationUpdates()}
            }
        }

    }
    // 2
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    // 3
    public override fun onResume() {
        super.onResume()
        if (timerOn!=null) {  //!locationUpdateState
           startLocationUpdates()
        }
    }
    private fun stylePolyline(polyline: Polyline) {
        var type = ""
        // Get the data object stored with the polyline.
        if (polyline.tag != null) {
            type = polyline.tag.toString()
        }
        when (type) {
            "A" ->
            {
                polyline.color = COLOR_GREEN_ARGB
                //polyline.startCap = RoundCap()
                //polyline.endCap = RoundCap()
            }

            "B" -> {
                polyline.color = COLOR_RED_ARGB
                //polyline.startCap = RoundCap()
               // polyline.endCap = CustomCap(
                 //   BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)
                //)
            }
        }
        polyline.width = 8.toFloat()
        polyline.jointType = JointType.ROUND
    }
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                println("!!! FirstLocation: ${currentLatLng}")
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            }
        }
    }
    override fun onMarkerClick(p0: Marker?) = false
    override fun onPolylineClick(p0: Polyline?) {
    }
    private fun doSomethingWithLastLocation(location:Location) {

        val speed = location.speed
        val speedText = findViewById<TextView>(R.id.avgspeedvalue)
        speedText.text = String.format("%.2f", speed)+" m/sec"
       index++
        if (index%2 == 0){
            location2 = location
        } else {
            location1 = location}
        if (index>1 && location1 != null && location2 != null){
            distance = location1!!.distanceTo(location2!!)
            totalDistance += distance
        }


        val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        myLocLatLngList.add(currentLatLng)
        if (myLocLatLngList.size>1){
            val options = PolylineOptions()
            options.color(Color.BLUE)
            options.width(5f)
            for (LatLng in myLocLatLngList) {options.add(LatLng)}
            map.addPolyline(options)
        }

        val distV = findViewById<TextView>(R.id.distancevalue)
        distV.text = String.format("%.1f", totalDistance)+" meters"
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

        //spara till firestore
        val locGeo =  GeoPoint(location.latitude, location.longitude)
        val a = LocationObject("",locGeo, totalDistance, timeUnit)
        db.collection("users").document(myUserUid).collection("maps").document(docUid).collection("mapObjects").document("$index").set(a)
            .addOnSuccessListener {
                println("!!! locationObject sparades på firestore")
            }
            .addOnFailureListener {
                println("!!!LocationObject sparades INTE!")
            }
    }
    override fun onBackPressed() {
        if (timerOn == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
