package com.poema.runnerapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MapRecycleAdapter (private val context : Context, private val tracks: List<Track>): RecyclerView.Adapter<MapRecycleAdapter.ViewHolder>(), CoroutineScope {

    var downloadedLocObjects = mutableListOf<LocationObject>()
    private lateinit var job : Job
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var db : AppDatabase
    private val layoutInflater = LayoutInflater.from(context)
    var loggedIn = false
    var pos : Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        job = Job()
        db = DatabaseSource.getInstance(context)
        loggedIn = true

        val itemView = layoutInflater.inflate(R.layout.list_item, parent, false )
        return ViewHolder(itemView)
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]
        holder.textViewName.text = track.name
        val temp = "Length: " + String.format("%.0f", track.length)+" meters"
        holder.textViewLength.text = temp
        val temp2 ="Best time: " + track.time
        holder.textViewTime.text = temp2
        val b = track.timestamp
        val temp3 = "Set: " + b.substring(0,b.length-10)
        holder.textViewDate.text = temp3
        holder.mapPosition = position
    }
    // startar raderingsprocessen här - tar emot positionen som det tryckts på och tar fram roomid:t på den banan
    fun removeTrack(position : Int) {
        val a = Datamanager.tracks[position]
        val id = a.trackId
        pos = position
        eraseLocationObjects(id)

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

    fun findTrackToDelete(id : Long){
        var track: Track
        async(Dispatchers.IO) {
            track = db.locationDao().findTrackById(id)
            db.locationDao().delete(track)
            println("!!!Track with ID: ${track.trackId} located and deleted!!")
            eraseAttemptObjects(id)
        }
    }
    suspend fun notifyOnMain(){
        withContext(Dispatchers.Main){
            Datamanager.tracks.removeAt(pos)
            println("Detta är slutpositionen!")
            notifyDataSetChanged()
        }
    }
    fun loadLocationObjectsByTrack(locObjTrackId: Long) : Deferred<List<LocationObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findLocObjectsByTrackId(locObjTrackId)
        }
    fun deleteLocationObject(locationObject: LocationObject) {
        async(Dispatchers.IO) {
            db.locationDao().delete(locationObject)
            println("!!!LocationObject with id: ${locationObject.locObjId} and track ID: ${locationObject.locObjTrackId} deleted!")
        }
    }
    fun eraseAttemptObjects(id : Long) {
        val allAttObj = loadAttemptObjectsByTrack(id)
        launch {
            allAttObj.await().forEach {
                deleteAttemptObject(it)
            }
            notifyOnMain()
        }
    }
    fun loadAttemptObjectsByTrack(attObjTrackId: Long) : Deferred<List<AttemptObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findAttemptObjectsByTrackId(attObjTrackId)
        }
    fun deleteAttemptObject(attemptObject: AttemptObject) {
        async(Dispatchers.IO) {
            db.locationDao().delete(attemptObject)
            println("!!!AttemptObject with id: ${attemptObject.aoId} and track ID: ${attemptObject.aoTrackId} deleted!")
        }
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        val textViewName: TextView = itemView.findViewById<TextView>(R.id.textName)
        val textViewLength: TextView = itemView.findViewById<TextView>(R.id.textLength)
        val textViewTime: TextView = itemView.findViewById<TextView>(R.id.textTime)
        val textViewDate: TextView = itemView.findViewById<TextView>(R.id.textVdate)
        private val delButton: ImageView =  itemView.findViewById<ImageView>(R.id.deleteImage)
        private val chooseTrackBtn: ImageButton = itemView.findViewById<ImageButton>(R.id.chooseImgBtn)
        private val chooseStatsBtn: ImageButton = itemView.findViewById<ImageButton>(R.id.statsImgButton)
        var mapPosition = 0

        init{
            delButton.setOnClickListener{view ->

                val dialogBuilder = AlertDialog.Builder(context)

                dialogBuilder.setTitle("Delete Track")
                    .setMessage("Do you want to delete this track and all of its statistics?")
                    //.setCancelable(false)
                    .setPositiveButton("Delete", DialogInterface.OnClickListener {
                            _, _ ->
                        removeTrack(mapPosition)
                        Snackbar.make(view, "Track deleted", Snackbar.LENGTH_SHORT).show()
                    })
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                            dialog, _ -> dialog.cancel()
                    })

                val alert = dialogBuilder.create()

                alert.show()
            }
            chooseTrackBtn.setOnClickListener{
                val intent = Intent(context, ChosenTrackMapActivity::class.java)
                intent.putExtra("position", mapPosition)
                context.startActivity(intent)
            }
            chooseStatsBtn.setOnClickListener{
                val intent = Intent(context, StatsActivity::class.java)
                intent.putExtra("positi", mapPosition)
                context.startActivity(intent)
            }
        }
    }
}
