package com.poema.runnerapp

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timer



class ChosenTrackMapActivity : AppCompatActivity(), OnMapReadyCallback, OnPolylineClickListener,
    GoogleMap.OnMarkerClickListener
    {

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
    private var distance: Float = 0.0f
    private var index: Int = 0
    private var location1: Location? = null
    private var location2: Location? = null
    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    var docUid = ""
    private var myUserUid = ""
    private var trackName = ""
    private var myLatLng: LatLng? = null
    private var b = ""
    private val DOT: PatternItem = Dot()
    private val GAP: PatternItem = Gap(8F)
    private val PATTERN_POLYLINE_DOTTED = listOf(GAP, DOT)
    private val markerList = mutableListOf<Marker>()
    private var sec: Int = 0
    private var lost = false
    private var length = 0.0
    private var havePressedStart = true
    private var havePressedStop = true
    private var myLocationsList = mutableListOf<Location>()
    private var withinGoalRadius : Boolean = false
    private var zoomUpdate = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choosen_track_map2)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //ta emot position

        val position = intent.getIntExtra("position", 0)
        if (Datamanager.maps[position].length != null) {
        length = Datamanager.maps[position].length!!
        }
        val tvLength = findViewById<TextView>(R.id.tvDistanceLeft)
        tvLength.text = String.format("%.0f", (length))
        if (Datamanager.maps[position].name != null) {
            trackName = Datamanager.maps[position].name!!
        }
        val handler = Handler()
        val delay = 1000 //milliseconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (timeUnit < markerList.size && timerOn != null) {
                    if (timeUnit >= 0) {
                        val a = markerList[timeUnit]
                        a.isVisible = true
                        val b = markerList[sec]
                        b.isVisible = false
                    }
                    sec = timeUnit
                }
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
        //Skriver in tracknamnet i rubriken

        val header = findViewById<TextView>(R.id.header)
        header.text = trackName

        //initialiserar firestore och auth

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            myUserUid = auth.currentUser!!.uid
        }

        //fixar switchknappen
        val switchBtn = findViewById<Switch>(R.id.switch1)
        switchBtn.setOnCheckedChangeListener{_, isChecked ->
            zoomUpdate = isChecked
        }


        //fixar stopknappen
        val stopButton = findViewById<Button>(R.id.stopbtn)
        stopButton.setOnClickListener {
            if (timerOn != null && havePressedStop == true) {
                havePressedStop = false
                startTimer(false)
                onPause()
                println("!!! accumulerad distans:  ${NewDataManager.newLocationObjects[NewDataManager.newLocationObjects.size-1].accDistance}")
                var ghostGoalDistance = NewDataManager.newLocationObjects[NewDataManager.newLocationObjects.size-1].accDistance
                if (ghostGoalDistance != null) {
                    val a = 0.1 * ghostGoalDistance     //procentsatsen för när användaren ska anses vara tillräckligt nära mål mätt i ackumulerad distans
                                                                // för att tiden ska kunna räknas som ev rekord. Dessutom ska den befinna sig högst 60m från startpunkten
                                                                // när senaste location togs (4-5s intervaller)
                    if (timeUnit < markerList.size && totalDistance > (ghostGoalDistance - a) && myLocationsList[0].distanceTo(myLocationsList[index-1]) < 60.0) {
                        // vad ska hända när man vunnit
                        val intent = Intent(this, DefeatedGhostActivity::class.java)
                        intent.putExtra("Time", timeUnit)
                        intent.putExtra("Distance", totalDistance)
                        intent.putExtra("docUid", docUid)
                        intent.putExtra("ind",index)
                        intent.putExtra("posit", position)
                        startActivity(intent)
                    } else {
                        eraseIfLostToGhost()
                    }
                }
            }
        }
        //fixar startknappen

        val startButton = findViewById<Button>(R.id.startbtn)
        startButton.setOnClickListener {
            if (timerOn == null && havePressedStart == true) {
                havePressedStart = false
                val myDate = getCurrentDateTime()
                val dateInString = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")

                // Lägger till tom bana i firestore

                val a = Map("", 0.0, "", "", dateInString)
                println("!!! $a")
                db.collection("users").document(myUserUid).collection("maps").add(a)
                    .addOnSuccessListener { uid ->
                        docUid = uid.id
                        startingFunction()
                        println("!!! Tom bana sparades på firestore")
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
                //startGhost()
                println("!!! timeunit: $timeUnit")
                doSomethingWithLastLocation(lastLocation)
            }
        }
        getLocationObjects(position)
        createLocationRequest()
    }

    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }
    private fun startingFunction () {
        val header = findViewById<TextView>(R.id.header)
        header.text = "Running.."
        startTimer(true)
        onResume()
    }
    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
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
        map.uiSettings.isZoomControlsEnabled = false
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
            if (timerOn != null) {
                startLocationUpdates()
            }
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {

                try {
                    e.startResolutionForResult(
                        this@ChosenTrackMapActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                if (timerOn != null) {
                    startLocationUpdates()

                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (timerOn != null) {  //!locationUpdateState
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }

    override fun onPolylineClick(p0: Polyline?) {
    }

    private fun doSomethingWithLastLocation(location: Location) {
       var trailing: Boolean = false
        myLocationsList.add(location)

        //skapar ny location och senaste location - kollar distansen mellan dem, adderar till totaldistance.

        index++

        if (index % 2 == 0) {
            location2 = location
        } else {
            location1 = location
        }
        if (index > 1 && location1 != null && location2 != null) {
            distance = location1!!.distanceTo(location2!!)
            totalDistance += distance
        }
        //jämför ghostdistansen och den nuvarande distansen, skriver skillnaden på skärmen

        if (timeUnit < markerList.size) {
            val aot = findViewById<TextView>(R.id.aot)
            val aotValue = findViewById<TextView>(R.id.aotValue)
            val ghostAccDistance = NewDataManager.newLocationObjects[timeUnit].accDistance
            val distanceLeft = findViewById<TextView>(R.id.tvDistanceLeft)
            val distanceLeftStr = String.format("%.0f", (length - totalDistance))
            distanceLeft.text = distanceLeftStr
            if (totalDistance < ghostAccDistance!!) {
                trailing = true
                aot.setTextColor(Color.RED)
                aot.text = "trailing"
                val str = String.format("%.0f", (ghostAccDistance - totalDistance)) + "m"
                aotValue.setTextColor(Color.RED)
                aotValue.text = str
            } else {
                trailing = false
                aot.setTextColor(Color.GREEN)
                aot.text = "ahead"
                aotValue.setTextColor(Color.GREEN)
                val str = String.format("%.0f", (totalDistance - ghostAccDistance)) + "m"
                aotValue.text = str
            }
        } else {
            val resultText = findViewById<TextView>(R.id.textView5)
            resultText.setTextColor(Color.RED)
            resultText.textSize = 22F
            resultText.text = "Your ghost won!! Better luck next time!"
            lost = true
        }

        //fyller på lista med inkommande locationspunkter och ritar en polyline
        val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        myLocLatLngList.add(currentLatLng)

        if (myLocLatLngList.size > 1) {

            val options = PolylineOptions()
            if (trailing){
                options.color(Color.RED)}
            else{
                options.color(Color.GREEN)
            }
            options.width(9f)

            for (LatLng in myLocLatLngList) {
                options.add(LatLng)
            }
            // ritar ut polylinen
            map.addPolyline(options)
        }

        // uppdaterar kameran till nuvarande position
        if (zoomUpdate) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
        }
        //skapar och sparar LocationObjects till firestore till den tomma map:pen.
        val locGeo = GeoPoint(location.latitude, location.longitude)
        val a = LocationObject("", locGeo, totalDistance, timeUnit)
        db.collection("users").document(myUserUid).collection("maps").document(docUid)
            .collection("mapObjects").document("$index").set(a)
            .addOnSuccessListener {
                println("!!! locationObject sparades på firestore")
            }
            .addOnFailureListener {
                println("!!!LocationObject sparades INTE!")
            }
    }

    override fun onBackPressed() {
        // ser till så man inte kan lämna sidan om timern är på
        if (timerOn == null) {
            val intent = Intent(this, TracksActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getLocationObjects(position: Int) {
        val a = Datamanager.maps[position]
        if (a.id != null) {
            b = (a.id!!)
        }                                    // uid:t på map:pen ifråga -> laddar ner collection av mapObjects som hör till den map:pen.

        val docRef1 = db.collection("users").document(myUserUid).collection("maps").document(b)
            .collection("mapObjects").orderBy(
                "time", Query.Direction.ASCENDING
            )
        docRef1.get().addOnSuccessListener { documentSnapshot ->
            ObjectDataManager.locationObjects.clear()                                //tömmer ObjectDatamanager...
            for (document in documentSnapshot.documents) {
                val newLocationObject = document.toObject(LocationObject::class.java)

                if (newLocationObject != null) {
                   newLocationObject.id =
                       (document.id)                         //....lägger sedan till dessa mapObjects (som kommer från firestore till objektdatamanager)
                    ObjectDataManager.locationObjects.add(newLocationObject)
                }
            }
            drawPolylines()
            makeGhost()
        }
    }
    //rita ut den inlästa banan !!
    private fun drawPolylines() {
        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(6f)
        options.pattern(PATTERN_POLYLINE_DOTTED)
        for (locationObject in ObjectDataManager.locationObjects) {
            if (locationObject.locLatLng != null) {
                val lat: Double = locationObject.locLatLng!!.latitude
                val lng: Double = locationObject.locLatLng!!.longitude
                myLatLng = LatLng(lat, lng)
                options.add(myLatLng!!)
            }
        }
        map.addPolyline(options)
    }

    private fun makeGhost() {

        NewDataManager.newLocationObjects.clear()
        println("!!! Storlek från början :${ObjectDataManager.locationObjects.size}")
        for (locationObject in ObjectDataManager.locationObjects) {
            println("!!! Från ObjectDataManager Först:   $locationObject")
        }
        // kalkylerar fram och lägger till nya objekt för varje sekund istället för var fjärde eller femte
        for (x in 1 until ObjectDataManager.locationObjects.size) {

            val object1 = ObjectDataManager.locationObjects[x - 1]
            val object2 = ObjectDataManager.locationObjects[x]
            var latResult : Double
            var lngResult : Double
            var accDistResult : Double
            var timeResult: Int

            val lat1 = object1.locLatLng!!.latitude
            val lng1 = object1.locLatLng!!.longitude
            val time1 = object1.time!!
            val accDist1 = object1.accDistance!!
            val lat2 = object2.locLatLng!!.latitude
            val lng2 = object2.locLatLng!!.longitude
            val time2 = object2.time!!
            val accDist2 = object2.accDistance!!

            val latDiff = lat2 - lat1
            val lngDiff = lng2 - lng1
            val timeDiff = time2 - time1
            val accDistDiff = accDist2 - accDist1
            val latPiece = latDiff / timeDiff
            val lngPiece = lngDiff / timeDiff
            val accDistPiece = accDistDiff / timeDiff
            var latPieces = lat1
            var lngPieces = lng1
            var accDistPieces = accDist1
            for (ind in 1 until timeDiff) {
                latPieces += latPiece
                lngPieces += lngPiece
                accDistPieces += accDistPiece
                latResult = latPieces
                lngResult = lngPieces
                accDistResult = accDistPieces
                timeResult = time1 + ind
            //skapar de framkalkylerade objekten och lägger in i newdatamanager
                val locGeo = GeoPoint(latResult, lngResult)
                val locObj = LocationObject("$ind", locGeo, accDistResult, timeResult)
                NewDataManager.newLocationObjects.add(locObj)
            }
        }
        // tar de gamla värdena och lägger in dem i den nya listan på rätt platser - varje sekund får ett eget objekt
        for (locationObject in ObjectDataManager.locationObjects) {
            if (locationObject != null) {
                val pos = locationObject.time!!
                val ob = locationObject
                NewDataManager.newLocationObjects.add(pos, ob)
            }
        }
        // lägger till markers enligt de skapade objekten, på kartan och gör dem osynliga tillsvidare
        for (locationObject in NewDataManager.newLocationObjects) {
            println("!!! Sedan:   $locationObject")
            val lt1 = locationObject.locLatLng!!.latitude
            val lg1 = locationObject.locLatLng!!.longitude

            val icon = BitmapDescriptorFactory.fromResource(R.drawable.testmarkeriiliten)

            val marker = map.addMarker(MarkerOptions().position(LatLng(lt1, lg1)).icon(icon).visible(false))
            markerList.add(marker)
        }
        for (locationObject in ObjectDataManager.locationObjects) {
            println("!!! Från ObjectDataManager Sedan:   $locationObject")
        }
    }

    private fun eraseIfLostToGhost(){

        for (i in 1..index){
            db.collection("users").document(myUserUid).collection("maps").document(docUid).collection("mapObjects").document("$i")
                .delete() .addOnSuccessListener {
                    Log.d(ContentValues.TAG, "!!! Document successfully deleted!")
                }
                .addOnFailureListener {
                        e -> Log.w(ContentValues.TAG, "!!! Error deleting document", e)
                }

        }
        eraseCollection()
    }
    private fun eraseCollection(){
                db.collection("users").document(myUserUid).collection("maps").document(docUid).delete()
                    .addOnSuccessListener {
                        println("!!! Tom bana raderades från firestore")
                        onPause()
                        goHome()
                    }
                    .addOnFailureListener {
                        println("!!! Tomma banan raderades INTE!")
                    }
    }
    private fun goHome(){
        val a: String = if (lost) {
                "Your record attempt has been deleted"
            } else{
                "You seem to have pressed 'stop' prematurely. Your record attempt has been deleted."
            }
            Toast.makeText(
                applicationContext, a,
                Toast.LENGTH_LONG).show(); goToStartPage()
        }

    private fun goToStartPage(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}







