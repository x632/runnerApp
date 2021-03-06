package com.poema.runnerapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.poema.runnerapp.adapters.StatsAdapter
import kotlinx.android.synthetic.main.activity_stats.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class StatsActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var statsAdapter: StatsAdapter
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    lateinit var series: LineGraphSeries<DataPoint>
    private lateinit var db: AppDatabase
    private var aoTrackId: Long = 0
    private var downloadedAttObjects = mutableListOf<AttemptObject>()
    private var currentBest : String = ""
    private var totalLength = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)
        val position = intent.getIntExtra("positi", -1)
        val temp = Datamanager.tracks[position]
        currentBest = temp.time
        val trackObject = Datamanager.tracks[position]
        val trackName = trackObject.name
        val trackNameTv = findViewById<TextView>(R.id.tvName)
        initStatsRecyclerView()
        trackNameTv.text = trackName
        getAttemptObjects(position)

    }
    private fun initStatsRecyclerView(){
        println( "!!! Varit i init")
        stats_recycler_view.apply{
            layoutManager = LinearLayoutManager(this@StatsActivity)
            statsAdapter = StatsAdapter(this@StatsActivity)
            adapter = statsAdapter
        }
    }

    private fun addData(){
        statsAdapter.submitList(downloadedAttObjects,currentBest)
    }

    fun createGraph() {

        val graph: GraphView = findViewById<GraphView>(R.id.graph)
        series = LineGraphSeries<DataPoint>()
        series.color = Color.GREEN
        for ((index, attemptObject) in downloadedAttObjects.withIndex()) {
            val x = index + 1.toDouble()
            val y = attemptObject.aoTime.toDouble()
            series.appendData(DataPoint(x, y), false, downloadedAttObjects.size)

        }
        graph.gridLabelRenderer.numHorizontalLabels = downloadedAttObjects.size
        graph.addSeries(series)
        graph.gridLabelRenderer.setHumanRounding(true)
    }

    private fun getAttemptObjects(position: Int) {
        val a = Datamanager.tracks[position]
        aoTrackId = a.trackId
        loadAtt()
    }

    private fun loadAtt() {

        val allAttObj = loadAttemptObjectsByTrack(aoTrackId)
        launch {
            allAttObj.await().forEach {
                downloadedAttObjects.add(it)
            }
            switchToMain2()
        }
    }

    fun loadAttemptObjectsByTrack(aoTrackId: Long): Deferred<List<AttemptObject>> =
        async(Dispatchers.IO) {
            db.locationDao().findAttemptObjectsByTrackId(aoTrackId)
        }

    private suspend fun switchToMain2() {
        withContext(Dispatchers.Main) {
            addData()
            statsAdapter.notifyDataSetChanged()
            writeSomeStuff()
            createGraph()
        }
    }

    private fun writeSomeStuff() {
        val theText = findViewById<TextView>(R.id.tvAttObj)
        val theContent = "Total number of runs on this track: ${downloadedAttObjects.size}\nTotal length of all runs: ${getTotalLength()} meters"
        theText.text = theContent
    }

    private fun getTotalLength():Int {
        for (attemptObject in downloadedAttObjects) {
            val temp3 = attemptObject.aoLength.roundToInt()
            totalLength += temp3
        }
        return totalLength

    }
}











