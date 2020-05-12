package com.poema.runnerapp

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
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
import org.w3c.dom.Text
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    var myLocList = mutableListOf<Location>()
    private lateinit var map: GoogleMap
    private var timerStarted = false
    private var timerOn: Timer? = null
    private var timeUnit = -1
    private val COLOR_GREEN_ARGB = -0xc771c4
    private val COLOR_RED_ARGB = -0xff000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var totalDistance = 0.0
    private var distance: Float = 0.0f
    private var index: Int = 0
    private var location1: Location? = null
    private var location2: Location? = null
    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choosen_track_map2)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //ta emot position

        val position = intent.getIntExtra("position", 0)
        if (Datamanager.maps[position].name != null) {
            trackName = Datamanager.maps[position].name!!
        }

        //Skriv in tracknamnet i rubriken

        val header = findViewById<TextView>(R.id.header)
        header.text = trackName
        println("!!! positionnumber: $position")

        //initialisera firestore och auth

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            myUserUid = auth!!.currentUser!!.uid
        }
        // gör timestamp

        val timeStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.ofHours(+2)).format(Instant.now())

        // Lägg till tom bana i firestore

        val a = Map("", 0.0, "", "", timeStamp)
        println("!!! $a")
        db.collection("users").document(myUserUid).collection("maps").add(a)
            .addOnSuccessListener { uid ->
                docUid = uid.id
                println("!!! Tom bana sparades på firestore")
            }
            .addOnFailureListener {
                println("!!! Tomma banan sparades INTE!")
            }

        //fixa stopknappen
        val stopButton = findViewById<Button>(R.id.stopbtn)
        stopButton.setOnClickListener {
            if (timerOn != null) {
                startTimer(false)
                onPause()
                val intent = Intent(this, NamingTrack::class.java)
                intent.putExtra("Time", timeUnit)
                intent.putExtra("Distance", totalDistance)
                intent.putExtra("docUid", docUid)
                startActivity(intent)
            }
        }
        //fixa startknappen
        val startButton = findViewById<Button>(R.id.startbtn)
        startButton.setOnClickListener {

            if (timerOn == null) {
                val headerTextV = findViewById<TextView>(R.id.header)
                headerTextV.text = "Running.."
                startTimer(true)
                onResume()
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                startGhost()
                println("!!! timeunit: $timeUnit")
                doSomethingWithLastLocation(lastLocation)
                /*doSomethingWithLastLocation(
                    LatLng(
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                )*/

            }
        }
        getLocationObjects(position)
        createLocationRequest()
    }

    fun startTimer(pressedStart: Boolean) {
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

    fun startGhost() {
        if (timeUnit < markerList.size) {
            if (timeUnit >= 0) {
                val a = markerList[timeUnit]
                a.isVisible = true
                val b = markerList[sec]
                b.isVisible = false
            }
            sec = timeUnit
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
        map.getUiSettings().setZoomControlsEnabled(true)
        //map.setOnMarkerClickListener(this)


        /* val polyline1 = googleMap.addPolyline(
            PolylineOptions().clickable(false).add(
                LatLng(59.235136, 17.997155),
                LatLng(59.235432, 17.997665))
        )

        polyline1.tag = "A"

        stylePolyline(polyline1)

        val polyline2 = googleMap.addPolyline(PolylineOptions().clickable(false).add(
            LatLng(59.235185, 17.997256),
            LatLng(59.235432, 17.997665))
        )

        polyline2.tag = "B"

        stylePolyline(polyline2)*/
        //val huddinge = LatLng(59.2351, 17.9973)
        //map.addMarker(MarkerOptions().position(59.2351, 17.9973).title("Marker in Huddinge"))
        // Map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.2351, 17.9973), 16.0f),5000,null)

        // Set listeners for click events.
        //googleMap.setOnPolylineClickListener(this)

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
                startGhost()
            }
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@ChosenTrackMapActivity,
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
            startGhost()
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
            // Got last known location. In some rare situations this can be null.
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

    private fun doSomethingWithLastLocation(location: Location) {

        //skapar ny location och senaste location kollar distansen mellan dem, adderar till totaldistance.

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
        //jämför ghostdistansen och den nuvarande distansen -> skriver skillnaden på skärmen

        if ((timeUnit < markerList.size)) {
            val aot = findViewById<TextView>(R.id.aot)
            val aotValue = findViewById<TextView>(R.id.aotValue)
            val ghostAccDistance = NewDataManager.newLocationObjects[timeUnit].accDistance
            if (totalDistance < ghostAccDistance!!) {
                aot.setTextColor(Color.RED)
                aot.text = "trailing"
                val str = String.format("%.1f", (ghostAccDistance - totalDistance)) + " m"
                aotValue.setTextColor(Color.RED)
                aotValue.text = str
                //String.format("%.2f", speed)+" m/sec"
            } else {
                aot.setTextColor(Color.GREEN)
                aot.text = "ahead"
                aotValue.setTextColor(Color.GREEN)
                val str = String.format("%.1f", (totalDistance - ghostAccDistance)) + " m"
                aotValue.text = str
            }
        } else {
            val resultText = findViewById<TextView>(R.id.textView5)
            resultText.setTextColor(Color.RED)
            resultText.textSize = 22F
            resultText.text = "Your ghost won!! Better luck next time!"
        }

        //fyller på lista med inkommande locationspunkter och ritar en polyline
        val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        myLocLatLngList.add(currentLatLng)
        if (myLocLatLngList.size > 1) {
            val options = PolylineOptions()
            options.color(Color.GREEN)
            options.width(7f)
            for (LatLng in myLocLatLngList) {
                options.add(LatLng)
            }
            // ritar ut polylinen
            map.addPolyline(options)
        }

        // uppdaterar kameran till nuvarande position

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))

        //skapar och sparar LocationObjects till firestore till den - i nuläget tomma map:pen.
        val locGeo = GeoPoint(location.latitude, location.longitude)
        val a = LocationObject("", locGeo, totalDistance, timeUnit)
        db.collection("users").document(myUserUid).collection("maps").document(docUid)
            .collection("mapObjects").add(a)
            .addOnSuccessListener {
                println("!!! locationObject sparades på firestore")
            }
            .addOnFailureListener {
                println("!!!LocationObject sparades INTE!")
            }
    }

    override fun onBackPressed() {
        // ser till så man inte kan lämna sidan om timern är på - om den inte är på så raderas den tomma banan från firestore först
        if (timerOn == null) {
            db.collection("users").document(myUserUid).collection("maps").document(docUid).delete()
                .addOnSuccessListener {
                    println("!!! Tom bana raderades från firestore")
                    onPause()
                }
                .addOnFailureListener {
                    println("!!! Tomma banan raderades INTE!")
                }

            val intent = Intent(this, TracksActivity::class.java)
            startActivity(intent)
        }
    }

    fun getLocationObjects(position: Int) {
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
            drawPolylines()
            makeGhost()
        }
    }
    //rita ut den inlästa banan !!
    fun drawPolylines() {
        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(6f)
        options.pattern(PATTERN_POLYLINE_DOTTED)
        for (locationObject in ObjectDataManager.locationObjects) {
            if (locationObject.locLatLng != null) {
                val lat: Double = locationObject.locLatLng!!.getLatitude()
                val lng: Double = locationObject.locLatLng!!.getLongitude()
                myLatLng = LatLng(lat, lng)
                options.add(myLatLng!!)
            }
        }
        map.addPolyline(options)
    }

    fun makeGhost() {

        NewDataManager.newLocationObjects.clear()

        println("!!! Storlek från början :${ObjectDataManager.locationObjects.size}")
        for (locationObject in ObjectDataManager.locationObjects) {
            println("!!! Från ObjectDataManager Först:   $locationObject")
        }
        // kalkylerar fram och lägger till nya objekt för varje sekund istället för var fjärde-femte
        for (x in 1 until ObjectDataManager.locationObjects.size) {

            val object1 = ObjectDataManager.locationObjects[x - 1]
            val object2 = ObjectDataManager.locationObjects[x]
            var latResult = 0.0
            var lngResult = 0.0
            var accDistResult = 0.0
            var timeResult: Int = 0

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

                val locGeo = GeoPoint(latResult, lngResult)
                val locObj = LocationObject("$ind", locGeo, accDistResult, timeResult)
                NewDataManager.newLocationObjects.add(locObj)
            }
        }
        // tar de (nya) gamla värdena och lägger in dem i den nya listan på rätt platser. Varje sekunde får ett eget objekt
        for (locationObject in ObjectDataManager.locationObjects) {
            if (locationObject != null) {
                val pos = locationObject.time!!
                val ob = locationObject!!
                NewDataManager.newLocationObjects.add(pos, ob)
            }
        }
        // lägger till markers på varje sekund och gör dem osynliga tills vidare
        for (locationObject in NewDataManager.newLocationObjects) {
            println("!!! Sedan:   $locationObject")
            val lt1 = locationObject.locLatLng!!.latitude
            val lg1 = locationObject.locLatLng!!.longitude
            val marker = map.addMarker(MarkerOptions().position(LatLng(lt1, lg1)).visible(false))
            markerList.add(marker)
        }
        for (locationObject in ObjectDataManager.locationObjects) {
            println("!!! Från ObjectDataManager Sedan:   $locationObject")
        }
    }

}




