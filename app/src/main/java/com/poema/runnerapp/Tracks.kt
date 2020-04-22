package com.poema.runnerapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Tracks : AppCompatActivity() {

    var maps = listOf<Map>(Map("Skogsslingan", 15.4,"01:15:11"),
        Map("Långrundan", 9.2,"00:45:22"), Map("Tunga löpet",4.2,"00:32:04"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // konstruera en adapter från vår adapter-klass, skicka med vår data
        val adapter = MapRecycleAdapter(this, maps)

        //koppla ihop vår adapter med recyclerview:n
        recyclerView.adapter = adapter
    }
}
