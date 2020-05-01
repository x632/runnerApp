package com.poema.runnerapp

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MapRecycleAdapter (private val context : Context, private val maps: List<Map>, private val myUserUid: String): RecyclerView.Adapter<MapRecycleAdapter.ViewHolder>() {
    //inflator behövs för att skapa en view utifrån en layout (xml)

    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    private val layoutInflater = LayoutInflater.from(context)
    var loggedIn = false
    var myUserId = ""
    var b = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth !=null) {
            loggedIn = true
            myUserId = auth!!.currentUser!!.uid
        }
        //använder vår inflator för att skapa en view
        val itemView = layoutInflater.inflate(R.layout.list_item, parent, false )
        // skapar vi en viewHolder av vår egna klass ViewHolder (skriven längre ner här)
        return ViewHolder(itemView)
    }
    // hur många views ska recyclerviewn innehålla? så många som finns i persons!
    override fun getItemCount() = maps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //när en item_view ska befolkas tar vi rätt person från vår data
        val map = maps[position]
        // sätter in den personens uppgifter i vår view
        holder.textViewName.text = "Name: " + map.name
        holder.textViewLength.text = "Length: " + map.length.toString() + "km"
        holder.textViewTime.text = "Time: " + map.time
        holder.textViewId.text = "ID: " + map.id
        holder.mapPosition = position
    }
    fun removeTrack(position : Int) {
        val a= Datamanager.maps[position]
        if (a.id != null) {
            b = (a.id!!)
        }
        Datamanager.maps.removeAt(position)
        println("!!!  ID : "+ b +" och userID: "+myUserUid)
       db.collection("users").document(myUserUid).collection("maps").document(b).delete()
            .addOnSuccessListener {
                Log.d(TAG, "!!! Document successfully deleted!")
                onDeleteCompletion()
            }
            .addOnFailureListener {
                    e -> Log.w(TAG, "!!! Error deleting document", e)
            }



    }
    fun onDeleteCompletion(){
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        // när en viewholder skapas så letar vi reda på våra textview:s som finns i vår item_view
        val textViewName = itemView.findViewById<TextView>(R.id.textName)
        val textViewLength = itemView.findViewById<TextView>(R.id.textLength)
        val textViewTime = itemView.findViewById<TextView>(R.id.textTime)
        val textViewId = itemView.findViewById<TextView>(R.id.textId)
        val delButton =  itemView.findViewById<ImageView>(R.id.deleteImage)
        var mapPosition = 0

        init{
            delButton.setOnClickListener{view ->

                val dialogBuilder = AlertDialog.Builder(context)

                dialogBuilder.setTitle("Delete Track")
                    .setMessage("Do you want to delete this track?")
                    //.setCancelable(false)
                    .setPositiveButton("Delete", DialogInterface.OnClickListener {
                            dialog, id ->
                        removeTrack(mapPosition)
                        Snackbar.make(view, "Track deleted", Snackbar.LENGTH_SHORT).show()
                    })
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                            dialog, id -> dialog.cancel()
                    })

                val alert = dialogBuilder.create()

                alert.show()
            }


        }

    }

}
