package com.poema.runnerapp

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MapRecycleAdapter (private val context : Context, private val maps: List<Map>, private val myUserUid: String): RecyclerView.Adapter<MapRecycleAdapter.ViewHolder>() {
    //inflator behövs för att skapa en view utifrån en layout (xml)

    private var mapObjectUidIndex = -1
    private var idList = mutableListOf<String>()
    lateinit var db: FirebaseFirestore
    private var auth: FirebaseAuth? = null
    private val layoutInflater = LayoutInflater.from(context)
    var loggedIn = false
    var myUserId = ""
    var b = ""
    var d = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (auth !=null) {
            loggedIn = true
            myUserId = auth!!.currentUser!!.uid
        }
        val itemView = layoutInflater.inflate(R.layout.list_item, parent, false )
        return ViewHolder(itemView)
    }
    override fun getItemCount() = maps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val map = maps[position]
        holder.textViewName.text = "Name: " + map.name
        holder.textViewLength.text = "Length: " + String.format("%.0f", map.length)+" meters"//"Length: " + map.length.toString() + "m"
        holder.textViewTime.text = "Time: " + map.time
        //holder.textViewId.text = "ID: " + map.id
        holder.mapPosition = position
    }
    fun removeTrack(position : Int) {
        val a = Datamanager.maps[position]
        if (a.id != null) {
            b = (a.id!!)
        }                                    // uid:t på map:pen ifråga -> ladda ner collection av mapObjects som hör till den map:pen.

        val docRef1 = db.collection("users").document(myUserUid).collection("maps").document(b)
            .collection("mapObjects").orderBy(
                "time", Query.Direction.DESCENDING
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
            for (x in 0 until ObjectDataManager.locationObjects.size) {      //skapa därefter en lista med bara dessa firestore id:n
                val a = ObjectDataManager.locationObjects[x]
                idList.add("${a.id}")
            }
            indexingFunction()
            mapObjectUidIndex = -1
            deleteMap(position)

        }
    }
    private fun indexingFunction() {                     //gå igenom id-listan och aktivera radering av MapObjects för mappen...
        mapObjectUidIndex++
        if (mapObjectUidIndex <= idList.size-1) {
            deleteLocationObjects(mapObjectUidIndex)
        }
    }


    fun deleteLocationObjects(mapObjectUidIndex: Int) {             //själva raderingen
        db.collection("users").document(myUserUid).collection("maps").document(b).collection("mapObjects").document(idList[mapObjectUidIndex])
            .delete() .addOnSuccessListener {
                Log.d(TAG, "!!! Document successfully deleted!")
               indexingFunction()
            }
                .addOnFailureListener {
                        e -> Log.w(TAG, "!!! Error deleting document", e)

                }

    }
         private fun deleteMap(position: Int){
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

    private fun onDeleteCompletion(){
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        // när en viewholder skapas så letar vi reda på våra textview:s som finns i vår item_view
        val textViewName = itemView.findViewById<TextView>(R.id.textName)
        val textViewLength = itemView.findViewById<TextView>(R.id.textLength)
        val textViewTime = itemView.findViewById<TextView>(R.id.textTime)
       // val textViewId = itemView.findViewById<TextView>(R.id.textId)
        val delButton =  itemView.findViewById<ImageView>(R.id.deleteImage)
        val chooseTrackBtn = itemView.findViewById<ImageButton>(R.id.chooseImgBtn)
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
            chooseTrackBtn.setOnClickListener{
                val intent = Intent(context, MainActivity::class.java)
                //intent.putExtra("Time", timeUnit)
                context.startActivity(intent)
            }


        }

    }

}
