package com.poema.runnerapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.poema.runnerapp.AttemptObject
import com.poema.runnerapp.R
import kotlin.math.roundToInt

class StatsAdapter (private val context : Context): RecyclerView.Adapter<StatsAdapter.ViewHolder>(){

    private var attemptObjects: List<AttemptObject> = ArrayList()
    private var currentBest : String = ""

    fun submitList(newList: List<AttemptObject>,currentBest:String) {
        attemptObjects = newList
        this.currentBest = currentBest
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.attempt_item,parent,false)
        )
    }

    override fun onBindViewHolder(holder: StatsAdapter.ViewHolder, position: Int) {
        val currAttObj = attemptObjects[position]
        //date:
        val b: String = currAttObj.aoTimestamp
        val date = b.substring(0, b.length - 10)
        //avgspeed:
        val temp = currAttObj.aoLength.roundToInt()
        val temp2 = currAttObj.aoTime
        val result1: Double = temp / temp2.toDouble()
        val finalResult: Double = (3600 * result1) * 0.001
        val avgSpeedStr = String.format("%.2f", (finalResult)) + " km/h"
        //length
        val length = "${currAttObj.aoLength.roundToInt()}"

        holder.tvDate.text = "Date: $date"
        holder.tvAvgSpeed.text = "Avg speed: $avgSpeedStr"
        holder.tvTime.text = "Time: ${makeTimeStr(currAttObj.aoTime)}"
        holder.tvLength.text = "Length: ${length}m"
        var str = ""
        for ((index, attemptObject) in attemptObjects.withIndex()){
            val temp2 = makeTimeStr(attemptObject.aoTime)
            if (temp2 == currentBest && index == position) {
                    str = "current best"
            }
        }
        holder.currBest.text = str

        holder.mapPosition = position
    }

    override fun getItemCount(): Int {
        return attemptObjects.size
    }

    fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }


inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

    val tvDate: TextView = itemView.findViewById(R.id.date)
    val tvAvgSpeed :TextView = itemView.findViewById(R.id.avgSpeed)
    val tvTime: TextView = itemView.findViewById(R.id.time)
    val tvLength :TextView = itemView.findViewById(R.id.length)
    val currBest : TextView = itemView.findViewById(R.id.tvCurrentBest)
    var mapPosition = 0

    init {
        /*startGameButton.setOnClickListener {
            val intent = Intent(context, GameBoardScreen::class.java)
            intent.putExtra("id", uidList[mapPosition])
            intent.putExtra("gameSeq", gameSequencePosition)
            context.startActivity(intent)*/
        }
    }
}

