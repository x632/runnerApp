package com.poema.runnerapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class StatsActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    lateinit var series: LineGraphSeries<DataPoint>
    var x: Long = 0
    var y: Int = 0
    private lateinit var db: AppDatabase
    private var aoTrackId: Long = 0
    var downloadedAttObjects = mutableListOf<AttemptObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        job = Job()
        db = DatabaseSource.getInstance(applicationContext)
        val position = intent.getIntExtra("positi", -1)
        val trackObject = Datamanager.tracks[position]
        val trackName = trackObject.name
        val trackLength = trackObject.length
        val time = trackObject.time
        val trackNameTv = findViewById<TextView>(R.id.tvName)

        trackNameTv.text = trackName
        getAttemptObjects(position)

    }

    fun createGraph() {
        /*val calendar: Calendar = Calendar.getInstance()
        val d1: Date = calendar.time
        calendar.add(Calendar.DATE, 1)
     */

        val graph: GraphView = findViewById<GraphView>(R.id.graph)

        series = LineGraphSeries<DataPoint>()

        series.color = Color.GREEN;
        for ((index, attemptObject) in downloadedAttObjects.withIndex()) {
            val x = index + 1.toDouble()
            val y = attemptObject.aoTime.toDouble()
            series.appendData(DataPoint(x, y), false, downloadedAttObjects.size)

        }
        graph.gridLabelRenderer.numHorizontalLabels = downloadedAttObjects.size
        graph.addSeries(series)
        graph.gridLabelRenderer.setHumanRounding(true)


        // set date label formatter
        /*graph.gridLabelRenderer.labelFormatter = (DateAsXAxisLabelFormatter(this))
        graph.gridLabelRenderer.numHorizontalLabels = 2
        // set manual x bounds to have nice steps
        graph.viewport.setMinX(d1.time.toDouble())
        graph.viewport.setMaxX(d11.time.toDouble())
        graph.viewport.isXAxisBoundsManual = true
*/
        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        //graph.gridLabelRenderer.setHumanRounding(true)


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
                println("!!! $it")
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
            writeSomeStuff()
            createGraph()
        }
    }

    private fun writeSomeStuff() {
        val theText = findViewById<TextView>(R.id.tvAttObj)
        val theContent = getContent()
        theText.text = theContent
    }

    private fun getContent(): String {
        var fied: String = "Attempts made and avg speed:\n\n"
        for ((index, attemptObject) in downloadedAttObjects.withIndex()) {
            val temp = attemptObject.aoLength.roundToInt()
            val temp2 = attemptObject.aoTime
            val result1: Double = temp / temp2.toDouble()
            val finalResult: Double = (3600 * result1) * 0.001
            val b: String = attemptObject.aoTimestamp
            val short = " " + b.substring(0, b.length - 10)
            fied += ("$short : " + String.format("%.2f", (finalResult)) + " km/h\n")

        }
        fied += "\nTime and length of track:\n\n"
        for ((index, attemptObject) in downloadedAttObjects.withIndex()) {
            val temp = attemptObject.aoLength.roundToInt()
            val temp2 = makeTimeStr(attemptObject.aoTime)

            val b: String = attemptObject.aoTimestamp
            val short = " " + b.substring(0, b.length - 10)
            fied += ("$short: $temp2, $temp m\n")
        }
        return fied
    }
    fun makeTimeStr(timeUnit: Int): String {
        val hours = timeUnit / 36000
        val minutes = timeUnit % 36000 / 60
        val seconds: Int = timeUnit % 60
        return "%1$02d:%2$02d:%3$02d".format(hours, minutes, seconds)
    }
}










