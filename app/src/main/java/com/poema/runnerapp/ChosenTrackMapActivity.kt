package com.poema.runnerapp

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
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
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext


class ChosenTrackMapActivity : AppCompatActivity(), OnMapReadyCallback, OnPolylineClickListener,
    GoogleMap.OnMarkerClickListener, TextToSpeech.OnInitListener, CoroutineScope {

    private var newTrackId : Long = 0
    var oldTrackId : Long = 0
    private lateinit var job : Job
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var db : AppDatabase
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private var tts: TextToSpeech? = null
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }
    var downloadedLocObjects = mutableListOf<LocationObject>()
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
    var docUid = ""
    //private var myUserUid = ""
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
    private var zoomUpdate = true
    private var speechIsInitialized = false
    private var trailing: Boolean = true
    private var voiceUpdates: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choosen_track_map2)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // initialisera speech
        tts = TextToSpeech(this,this)

        job = Job()
        db = DatabaseSource.getInstance(applicationContext)
        //ta emot position
        val position = intent.getIntExtra("position", 0)

        length = Datamanager.tracks[position].length

        val tvLength = findViewById<TextView>(R.id.tvDistanceLeft)
        tvLength.text = String.format("%.0f", (length))

        trackName = Datamanager.tracks[position].name

        // visar ghostmarkern för innevarande sekund och gömmer den förra. Gör detta OM sekunden har bytts
        val handler = Handler()
        val delay = 500 //milliseconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (markerList.size > 1 && timeUnit < markerList.size-1  && timerOn != null) {
                    if (timeUnit >= 0 && timeUnit != sec){
                        val a = markerList[timeUnit]
                        a.isVisible = true
                        val b = markerList[sec]
                        b.isVisible = false
                        sec = timeUnit
                    }

                }
                if (markerList.size > 1 && timeUnit >= markerList.size-1){haveLost("från handler") }
                if (!lost)
                {
                handler.postDelayed(this, delay.toLong())}
            }
        }, delay.toLong())
        //Skriver in tracknamnet i rubriken

        val header = findViewById<TextView>(R.id.header)
        header.text = trackName

        //fixar Zoom switchknappen
        val switchBtn = findViewById<Switch>(R.id.switch1)
        switchBtn.setOnCheckedChangeListener{_, isChecked ->
            zoomUpdate = isChecked
        }
        // fixar voiceupdates knappen
        val switchBtn2 = findViewById<Switch>(R.id.switch2)
        switchBtn2.setOnCheckedChangeListener{_, isChecked ->
            voiceUpdates = isChecked
        }

        //fixar stopknappen
        val stopButton = findViewById<Button>(R.id.stopbtn)
        stopButton.setOnClickListener {
            if (timerOn != null && havePressedStop == true) {
                havePressedStop = false
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        if (location != null) {
                            doSomethingWithLastLocation(location)
                            println("!!! I-samband-med-stop-location sparad")
                            endStoppingProcedure(position, location)
                        }
                    }
            }
        }
        //fixar startknappen
        val startButton = findViewById<Button>(R.id.startbtn)
        startButton.setOnClickListener {
            if (timerOn == null && havePressedStart) {
                havePressedStart = false
                if (voiceUpdates) speakOut("Start")
                val myDate = getCurrentDateTime()
                val dateInString = myDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS")
                val a = Track(0, 0.0, "", "", dateInString)
                roomSaveTrack(a)
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                println("!!! timeunit: $timeUnit")
                doSomethingWithLastLocation(lastLocation)
            }
        }
        getLocationObjects(position)
        createLocationRequest()
    }
    private fun endStoppingProcedure(position:Int, location: Location){
        startTimer(false)
        if (voiceUpdates){speakOut("stop")}
        onPause()
        val ghostGoalDistance = NewDataManager.newLocationObjects[NewDataManager.newLocationObjects.size-1].accDistance

            val a = 0.1 * ghostGoalDistance     //procentsatsen för när användaren ska anses vara tillräckligt nära mål mätt i ackumulerad distans
            // för att tiden ska kunna räknas som ev rekord. Dessutom ska användaren befinna sig högst 20m från slutpunkten på ghostbanan (målet)
            val latestLocation = NewDataManager.newLocationObjects[NewDataManager.newLocationObjects.size-1].locLat
            val latestLocation2 = NewDataManager.newLocationObjects[NewDataManager.newLocationObjects.size-1].locLng
            val endLocation = Location("Punkt")

                    endLocation.latitude = latestLocation
                    endLocation.longitude = latestLocation2

            if (timeUnit < markerList.size && totalDistance > (ghostGoalDistance - a) && location.distanceTo(endLocation) < 20.0) {
                // vad ska hända när man vunnit

                val intent = Intent(this, DefeatedGhostActivity::class.java)
                intent.putExtra("Time", timeUnit)
                intent.putExtra("Distance", totalDistance)
                intent.putExtra("docUid", newTrackId)
                intent.putExtra("posit", position)
                startActivity(intent)
            } else {
                eraseIfLostToGhost()
            }


    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            // set UK English as language for tts
            val result = tts!!.setLanguage(Locale.UK)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","!!! The Language specified is not supported!")
            } else {
                speechIsInitialized = true
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }

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
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 2000
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
            if (totalDistance < ghostAccDistance) {
                val differ = ghostAccDistance - totalDistance
                aot.setTextColor(Color.RED)
                aot.text = "trailing"
                val str = String.format("%.0f", (differ)) + "m"
                aotValue.setTextColor(Color.RED)
                aotValue.text = str
                val differInt= str.substring(0,str.length-1).toInt()
                var speakString = ""
                if (!trailing && index > 0) {
                    when (differInt) {
                        1 -> {
                            speakString =
                                "You are trailing by " + str.substring(0, str.length - 1) + " meter"
                        }
                        0 -> {
                            speakString =
                                "You are trailing by less than one meter"
                        }
                        else -> {
                            speakString = "You are trailing by " + str.substring(0,str.length-1) + " meters"
                        }
                    }
                    if (voiceUpdates){speakOut(speakString)}
                }
                trailing = true
                } else {
                val differ = totalDistance - ghostAccDistance
                aot.setTextColor(Color.GREEN)
                aot.text = "ahead"
                aotValue.setTextColor(Color.GREEN)
                val str = String.format("%.0f", (differ)) + "m"
                aotValue.text = str
                val differInt = str.substring(0,str.length-1).toInt()
                var speakString = "Start"
                if (trailing && index > 1) {
                    when (differInt) {
                        1 -> {
                            speakString = "You are ahead by " + str.substring(0,str.length-1) + " meter"
                        }
                        0 -> {
                            speakString =
                                "You are ahead by less than one meter"
                        }
                        else -> {
                            speakString = "You are ahead by " + str.substring(0,str.length-1) + " meters"
                        }
                    }
                    if (voiceUpdates){speakOut(speakString)}
                }
                trailing = false
            }
        } else {
            haveLost("från intervallet")
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
            options.width(12f)
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
        saveLocationObject(location)
    }
    private fun haveLost(text:String){
        println("!!! $text")
        val resultText = findViewById<TextView>(R.id.textView5)
        resultText.setTextColor(Color.RED)
        resultText.textSize = 22F
        resultText.text = "Your ghost won!! Better luck next time!"
        lost = true
    }
    private fun saveLocationObject(location:Location){
        val lat = location.latitude
        val lng = location.longitude
        //skapar NYA locationobjects trackID ??
        val a = LocationObject(0, newTrackId,totalDistance, lat,lng, timeUnit)
        saveLocationObj(a)
    }
    private fun saveLocationObj(locationObject: LocationObject) {
        async(Dispatchers.IO) {  db.locationDao().insert(locationObject)
            println("!!!LocationsObject with track # ${locationObject.locObjTrackId} saved!!")}
    }
    override fun onBackPressed() {
        // ser till så man inte kan lämna sidan om timern är på
        if (timerOn == null) {
            val intent = Intent(this, TracksActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getLocationObjects(position: Int) {
        val a = Datamanager.tracks[position]
           oldTrackId = a.trackId
        loadLoc()
    }
    private fun loadLoc(){
        ObjectDataManager.locationObjects.clear() //tömmer ObjectDatamanager...
        val allLocObj  = loadLocationObjectsByTrack(oldTrackId)
        launch {
            allLocObj.await().forEach {
               ObjectDataManager.locationObjects.add(it)  // lägger in alla locobjects för tracken i Objectdatamanager
               println("!!! $it")
            }
            switchToMain2()
        }
    }
    //laddar ner LocObj för tracken i ordning(!!)
    fun loadLocationObjectsByTrack(locObjTrackId: Long) : Deferred<List<LocationObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findLocObjectsByTrackId(locObjTrackId)
        }
    //rita ut den inlästa banan !!
    private fun drawPolylines() {
        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(6f)
        options.pattern(PATTERN_POLYLINE_DOTTED)
        for (locationObject in ObjectDataManager.locationObjects) {
                val lat: Double = locationObject.locLat
                val lng: Double = locationObject.locLng
                myLatLng = LatLng(lat, lng)
                options.add(myLatLng)
        }
        map.addPolyline(options)
    }

    private fun makeGhost() {

        NewDataManager.newLocationObjects.clear()
        println("!!! Storlek från början :${ObjectDataManager.locationObjects.size}")
        // kalkylerar fram och lägger till nya objekt för varje sekund istället för var fjärde eller femte
        for (x in 1 until ObjectDataManager.locationObjects.size) {

            val object1 = ObjectDataManager.locationObjects[x - 1]
            val object2 = ObjectDataManager.locationObjects[x]
            var latResult : Double
            var lngResult : Double
            var accDistResult : Double
            var timeResult: Int

            val lat1 = object1.locLat
            val lng1 = object1.locLng
            val time1 = object1.time
            val accDist1 = object1.accDistance
            val lat2 = object2.locLat
            val lng2 = object2.locLng
            val time2 = object2.time
            val accDist2 = object2.accDistance

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
                //val locGeo = GeoPoint(latResult, lngResult)
                val lo : Long = ind.toLong()
                val locObj = LocationObject(lo,oldTrackId, accDistResult,latResult,lngResult, timeResult)
                NewDataManager.newLocationObjects.add(locObj)
                println("!!! locObjID in newdatamanager lo = $lo")
            }
        }
        // tar de gamla värdena och lägger in dem i den nya listan på rätt platser - varje sekund får ett eget objekt
        for (locationObject in ObjectDataManager.locationObjects) {

                val pos = locationObject.time
            println("!!! ID ${locationObject.locObjId}, time in seconds: $pos")
                NewDataManager.newLocationObjects.add(pos, locationObject)

        }
        // lägger till markers enligt de skapade objekten, på kartan och gör dem osynliga tillsvidare - förutom start och slut marker
        for ((i, locationObject) in NewDataManager.newLocationObjects.withIndex()) {
            println("!!! $locationObject with index: $i")
            val lt1 = locationObject.locLat
            val lg1 = locationObject.locLng

            val icon = BitmapDescriptorFactory.fromResource(R.drawable.testmarkerii)
            when (i) {
                0 -> {
                 val marker = map.addMarker(MarkerOptions().position(LatLng(lt1, lg1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).visible(true))
                markerList.add(marker)
                }
                NewDataManager.newLocationObjects.size-1 -> {
                    val marker = map.addMarker(MarkerOptions().position(LatLng(lt1, lg1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).visible(true))
                markerList.add(marker)
                }
                else -> {
                    val marker = map.addMarker(
                        MarkerOptions().position(LatLng(lt1, lg1)).icon(icon).visible(false)
                    )
                    markerList.add(marker)
                }
            }
        }

    }

    private fun eraseIfLostToGhost(){

        eraseLocationObjects(newTrackId)

    }
    fun eraseLocationObjects(id : Long) {

        val allLocObj = loadLocationObjectsByTrack(id)
        launch {
            allLocObj.await().forEach {
                downloadedLocObjects.add(it)
            }
            for (locationObject in downloadedLocObjects) {
                deleteLocationObject(locationObject)
            }
            findTrackToDelete(id) //completionHandler efter radering av locobj
        }
    }
    fun deleteTrack(track: Track) {
        async(Dispatchers.IO) {   db.locationDao().delete(track)
            println("!!!Track deleted!!")
            switchToMain()
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
    fun deleteLocationObject(locationObject: LocationObject) {
        async(Dispatchers.IO) {   db.locationDao().delete(locationObject)
            println("!!!LocationObject deleted!!")
        }
    }
    private fun speakOut(text:String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
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
    public override fun onDestroy() {
        // Stäng av TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    private fun roomSaveTrack(track: Track){
        async(Dispatchers.IO) {
            newTrackId = db.locationDao().insert(track)
            println("!!!Track with trackID $newTrackId saved!!")
            switchToMain()
        }
    }
    private suspend fun switchToMain(){
        withContext(Dispatchers.Main){
            if (lost){onPause()
                println("!!! Detta bör vara sista texten som syns!!")
                goHome()}
            else{
            startingFunction()
            }
        }
    }
    private suspend fun switchToMain2(){
        withContext(Dispatchers.Main){
            drawPolylines()
            makeGhost()
        }
    }
}








