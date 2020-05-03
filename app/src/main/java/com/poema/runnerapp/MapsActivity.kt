package com.poema.runnerapp

import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.concurrent.timer


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var timerStarted = false
    var timerOn : Timer? = null
    var timeUnit = -1

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
        val huddinge = LatLng(59.2351, 17.9973)
        mMap.addMarker(MarkerOptions().position(huddinge).title("Marker in Huddinge"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.2351, 17.9973), 16.0f),5000,null)

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
}
