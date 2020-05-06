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
import java.util.*
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
    private var distance : Float = 0.0f
    private var index : Int = 0
    private var location1 : Location? = null
    private var location2 : Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val stopButton = findViewById<Button>(R.id.stopbutton)
        stopButton.setOnClickListener {
            if (timerOn != null) {
                startTimer(false)
                onPause()
                val intent = Intent(this, NamingTrack::class.java)
                intent.putExtra("Time", timeUnit)
                intent.putExtra("Distance", totalDistance)
                startActivity(intent)
            }
        }

        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {
            if (timerOn == null) {
                val header = findViewById<TextView>(R.id.header)
                header.text = "Running.."
                startTimer(true)
                onResume()
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                doSomethingWithLastLocation(lastLocation)
                /*doSomethingWithLastLocation(
                    LatLng(
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                )*/

            }
        }
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
        map.setOnMarkerClickListener(this)


        val polyline1 = googleMap.addPolyline(
            PolylineOptions()
                .clickable(false)
                .add(
                    LatLng(59.235136, 17.997155),
                    LatLng(59.235432, 17.997665),
                    LatLng(59.234730, 17.998588),
                    LatLng(59.235537, 17.999822),
                    LatLng(59.235252, 18.001238),
                    LatLng(59.234775, 18.001978),
                    LatLng(59.232385, 18.004181),
                    LatLng(59.232209, 18.001628),
                    LatLng(59.231452, 18.001099),
                    LatLng(59.230673, 18.000230),
                    LatLng(59.230717, 17.998889),
                    LatLng(59.232144, 17.997752),
                    LatLng(59.233752, 17.997419),
                    LatLng(59.234185, 17.998138),
                    LatLng(59.234712, 17.998578))

        )
        // Store a data object with the polyline, used here to indicate an arbitrary type.
        polyline1.tag = "A"

        stylePolyline(polyline1)

        val polyline2 = googleMap.addPolyline(PolylineOptions().clickable(false).add(
                    LatLng(59.235185, 17.997256),
                    LatLng(59.235432, 17.997665),
                    LatLng(59.234730, 17.998588),
                    LatLng(59.235537, 17.999822),
                    LatLng(59.235252, 18.001238),
                    LatLng(59.234775, 18.001978)
                )
        )
        // Store a data object with the polyline, used here to indicate an arbitrary type.
        polyline2.tag = "B"

        stylePolyline(polyline2)
        //val huddinge = LatLng(59.2351, 17.9973)
        // mMap.addMarker(MarkerOptions().position(huddinge).title("Marker in Huddinge"))
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
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
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
                /*val header = findViewById<TextView>(R.id.header)
                header.text = "${currentLatLng}"*/

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            }
        }
    }
    override fun onMarkerClick(p0: Marker?) = false
    override fun onPolylineClick(p0: Polyline?) {
    }
    private fun doSomethingWithLastLocation(location:Location) {


       index++
        if (index%2 == 0){
            val loc2 = "!!! 2"
            location2 = location
        } else {val loc1 = "!!! 1"
            location1 = location}

        if (index>1 && location1 != null && location2 != null){
            distance = location1!!.distanceTo(location2!!)
            totalDistance += distance
            println ("!!! Distansen mellan koordinaten är: $distance meter. Den accumulerade distansen är : $totalDistance meter")
        }

        //val markerOptions = MarkerOptions().position(location)
        //markerOptions.title("Här nu!")
        //map.addMarker(markerOptions)

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
        distV.text = String.format("%.1f", totalDistance)+" meters";
        println("!!! Nuvarande kordinat $currentLatLng")
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
    }

}
