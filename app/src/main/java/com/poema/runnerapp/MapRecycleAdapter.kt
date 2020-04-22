package com.poema.runnerapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MapRecycleAdapter (private val context : Context, private val maps: List<Map>): RecyclerView.Adapter<MapRecycleAdapter.ViewHolder>() {
    //inflator behövs för att skapa en view utifrån en layout (xml)
    private val layoutInflater = LayoutInflater.from(context)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        // när en viewholder skapas så letar vi reda på våra två textview:s som finns i vår item_view
        val textViewName = itemView.findViewById<TextView>(R.id.textName)
        val textViewLength = itemView.findViewById<TextView>(R.id.textLength)
        val textViewTime = itemView.findViewById<TextView>(R.id.textTime)
    }

}
