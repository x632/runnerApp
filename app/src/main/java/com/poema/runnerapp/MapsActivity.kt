package com.poema.runnerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnPolylineClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.util.*
import kotlin.concurrent.timer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnPolylineClickListener {

    private lateinit var mMap: GoogleMap
    var timerStarted = false
    var timerOn : Timer? = null
    var timeUnit = -1
    private val COLOR_BLACK_ARGB = -0x1000000
    private val COLOR_GREEN_ARGB = -0xc771c4
    private val COLOR_RED_ARGB = -0xff000
    private val PATTERN_DASH_LENGTH_PX = 10
    private val PATTERN_GAP_LENGTH_PX = 10


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val stopButton = findViewById<Button>(R.id.stopbutton)
        stopButton.setOnClickListener {
            if (timerOn != null) {
                startTimer(false)
                val intent = Intent(this, NamingTrack::class.java)
                intent.putExtra("Time",timeUnit)
                startActivity(intent)
            }
        }

        val startButton = findViewById<Button>(R.id.startbutton)
        startButton.setOnClickListener {
            if (timerOn == null){
                val header = findViewById<TextView>(R.id.header)
                header.text = "Running.."
                startTimer(true)
            }
        }
    }
    fun startTimer(pressedStart : Boolean) {
        if (pressedStart && !timerStarted) {
            timerOn = timer(period = 1000) {

                timeUnit  ++

                val hours = timeUnit / 36000
                val minutes = timeUnit % 36000 / 60
                val seconds: Int = timeUnit % 60
                val strTime = "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
                val timerText = findViewById<TextView>(R.id.timer)

                timerText.text = strTime
                timerStarted = true
            }
        }
        else {
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
        mMap = googleMap

        // Add a marker in Huddinge and move the camera
        // Add polylines to the map.
        // Polylines are useful to show a route or some other connection between points.
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
                    LatLng(59.234712, 17.998578)
                )
        )
        // Store a data object with the polyline, used here to indicate an arbitrary type.
        polyline1.tag = "A"

        stylePolyline(polyline1)

        val polyline2 = googleMap.addPolyline(
            PolylineOptions()
                .clickable(false)
                .add(
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


        val huddinge = LatLng(59.2351, 17.9973)
       // mMap.addMarker(MarkerOptions().position(huddinge).title("Marker in Huddinge"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.2351, 17.9973), 16.0f),5000,null)

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this)

    }


    override fun onPolylineClick(p0: Polyline?) {

    }
    private fun stylePolyline(polyline: Polyline) {
        var type = ""
        // Get the data object stored with the polyline.
        if (polyline.tag != null) {
            type = polyline.tag.toString()
        }
        when (type) {
            "A" ->                 // Use a custom bitmap as the cap at the start of the line.
            {polyline.color = COLOR_GREEN_ARGB
                polyline.startCap = RoundCap()
                polyline.endCap = RoundCap()
            }
                /*polyline.startCap = CustomCap(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow))*/
            "B" ->                 // Use a round cap at the start of the line.
            {polyline.color = COLOR_RED_ARGB
            polyline.startCap = RoundCap()
                polyline.endCap = CustomCap(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow))}
        }
        polyline.width = 8.toFloat()
        polyline.jointType = JointType.ROUND
    }
}
