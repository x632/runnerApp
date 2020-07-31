package com.poema.runnerapp

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Switch
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
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnPolylineClickListener,
    GoogleMap.OnMarkerClickListener, CoroutineScope {

    var downloadedTracks = mutableListOf<Track>()
    private lateinit var job : Job
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var db : AppDatabase

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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var totalDistance = 0.0
    private var distance : Float = 0.0f
    private var index : Int = 0
    private var location1 : Location? = null
    private var location2 : Location? = null
    private var trackId : Long = 0
    private var havePressedStart = true
    private var havePressedStop = true
    private var zoomUpdate = true
    private var myLocationsList = mutableListOf<Location>()
    private var avgSpeed = 0.0
    private var statAvgSpeed = ""
    private var lastLoc = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //håller skrämen på!
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //firebase autentisering
       //raderad...

        //fixar switchknappen
        val mySwitchBtn = findViewById<Switch>(R.id.mySwitch)
        mySwitchBtn.setOnCheckedChangeListener{_, isChecked ->
            zoomUpdate = isChecked
        }

        //STOP-Knappen
        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener {

            if (timerOn != null && havePressedStop) {
                havePressedStop = false
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        if (location != null){
                            lastLoc = true
                    doSomethingWithLastLocation(location)
                        }
                        println("!!! I-samband-med-stop-location sparad")
                        endStoppingProcedure()
                    }
            }
        }

        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {

            if (timerOn == null && havePressedStart) {
                havePressedStart = false
                val myDate = getCurrentDateTime()
                val dateInString = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")

                //Ändrar här nedan till room - sparar tom bana

                //val a = Map("", 0.0, "", "", dateInString)
                val a = Track(0,0.0,"","",dateInString)
                roomSaveTrack(a)
                // doc Uid återstår att lösa

               /* db.collection("users").document(myUserUid).collection("maps").add(a)
                    .addOnSuccessListener { uid ->
                        docUid = uid.id
                        startingFunction() //OBS OBS denna är flyttad till completion handler i roomSaveTrack istället
                        println("!!! Tom bana sparades!")
                    }
                    .addOnFailureListener {
                        println("!!! Tomma banan sparades INTE!")
                    }*/

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

    //skickar över Tid, längd på banan, trackens UID, antal locations
    private fun endStoppingProcedure(){
        startTimer(false)
        onPause()
        val intent = Intent(this, NamingTrack::class.java)
        intent.putExtra("Time", timeUnit)
        intent.putExtra("Distance", totalDistance)
        //intent.putExtra("docUid", docUid)
        //Ändrat :
        intent.putExtra("docUid", trackId)
        intent.putExtra("ind",index)
        startActivity(intent)
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

        locationRequest = LocationRequest()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 4000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            if(timerOn != null){
            startLocationUpdates()}
        }
        task.addOnFailureListener { e ->

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
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    public override fun onResume() {
        super.onResume()
        if (timerOn!=null) {  //!locationUpdateState
           startLocationUpdates()
        }
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
                println("!!! FirstLocation: $currentLatLng")
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            }
        }
    }
    override fun onMarkerClick(p0: Marker?) = false
    override fun onPolylineClick(p0: Polyline?) {
    }
    private fun doSomethingWithLastLocation(location:Location) {
        myLocationsList.add(location)
        val speed = location.speed
        val speedText = findViewById<TextView>(R.id.speedvalue)
        speedText.text = String.format("%.1f", speed)+" m/sec"
        index++

        //räknar ut medelfarten

        for (location in myLocationsList){
            val a = location
            avgSpeed += a.speed
        }
        avgSpeed /= index
        val tvSpeedValue = findViewById<TextView>(R.id.tvAvgSpeedValue)
        tvSpeedValue.text = String.format("%.1f", avgSpeed)+" m/sec"
        statAvgSpeed = String.format("%.1f", avgSpeed)
        avgSpeed = 0.0

        //räknar ut distansen

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
            options.width(6f)
            for (LatLng in myLocLatLngList) {options.add(LatLng)}
            map.addPolyline(options)
        }

        val distV = findViewById<TextView>(R.id.distancevalue)
        distV.text = String.format("%.0f", totalDistance)+" meters"
        if (zoomUpdate) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
        }
       saveLocationObject(location)
    }

    private fun saveLocationObject(location: Location){
        val a : LocationObject
        val lat =  location.latitude
        val lng = location.longitude
        if (lastLoc) {
            //Ändrar här till room Obs : Sista location sätter noll på båda id:na tillsvidare
            //a = LocationObject("Sista location!!",locGeo, totalDistance, timeUnit)
            a = LocationObject(0,trackId,totalDistance,lat,lng,timeUnit)

            lastLoc = false
        }
        else {
            //Ändrar till Room....
            //a = LocationObject("", locGeo, totalDistance, timeUnit)
            a = LocationObject(0,trackId,totalDistance,lat,lng,timeUnit)
        }

        roomSaveLocationObject(a)
            /*db.collection("users").document(myUserUid).collection("maps").document(docUid)
                .collection("mapObjects").document("$index").set(a)
                .addOnSuccessListener {
                    println("!!! locationObject sparades på firestore")
                }
                .addOnFailureListener {
                    println("!!!LocationObject sparades INTE!")
                }*/

    }
    override fun onBackPressed() {
        if (timerOn == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun roomSaveLocationObject(locationObject: LocationObject){
        async(Dispatchers.IO) {   this@MapsActivity.db.locationDao().insert(locationObject)
            println("!!!LocationsObject saved!!")}

    }
    fun roomLoadTracks(){
        val allTracks  = loadAllTracks()
        launch {
            allTracks.await().forEach {
                downloadedTracks.add(it)
            }
        }
    }
    fun loadAllTracks() : Deferred<List<Track>>  =
        async(Dispatchers.IO) {
            db.locationDao().getAllTracks()
        }
    fun roomSaveTrack(track: Track) {

        async(Dispatchers.IO) {   trackId = db.locationDao().insert(track)
            println("!!!Track with trackID $trackId saved!!")
            startingFunction()}
    }
}
