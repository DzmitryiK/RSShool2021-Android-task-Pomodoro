package com.rsschool.pomodoro

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.rsschool.pomodoro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), StopwatchListener, LifecycleObserver {

    private lateinit var binding: ActivityMainBinding
    private val stopwatches = mutableListOf<Stopwatch>()
    private val stopwatchAdapter = StopwatchAdapter(this)
    private var nextId = 0

    private var timer: CountDownTimer? = null

    private var startTime = 0L
    private var runningId =-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopwatchAdapter
        }

        startTime = System.currentTimeMillis()

        binding.addNewTimerButton.setOnClickListener {
            if (binding.minutesEdit.text.isNullOrBlank()){
                Toast.makeText(this, resources.getString(R.string.timer_error_no_input), Toast.LENGTH_SHORT).show()
            }else{
                if ((binding.minutesEdit.text.toString().toIntOrNull() == null) || (binding.minutesEdit.text.toString().toInt() > 6000))
                {
                    Toast.makeText(this, resources.getString(R.string.timer_error_large_input), Toast.LENGTH_LONG).show()
                }else {
                    val time = binding.minutesEdit.text.toString().toLong() * 60000L
                    stopwatches.add(Stopwatch(nextId++, time, false))
                    stopwatchAdapter.submitList(stopwatches.toList())
                    binding.recycler.smoothScrollToPosition(stopwatches.size-1)
                }
            }
        }
    }

    override fun start(id: Int) {
        changeStopwatch(id, null, true)
    }

    override fun stop(id: Int, currentMs: Long) {
        changeStopwatch(id, currentMs, false)
    }

    override fun reset(id: Int) {
        changeStopwatch(id, -1, false)
    }

    override fun delete(id: Int) {
        //Stop CountDownTimer when delete
        stopwatches.find { it.id == id }?.currentMs  = -1L
        if (runningId == id) runningId = -1

        stopwatches.remove(stopwatches.find { it.id == id })
        stopwatchAdapter.submitList(stopwatches.toList())
    }

    private fun changeStopwatch(id: Int, currentMs: Long?, isStarted: Boolean) {
        var cms: Long?
        var startTime = 0L
        var otherFinished: Boolean


        for (i in stopwatches.indices){
            if (stopwatches[i].id == id){
                cms = if (currentMs == -1L) stopwatches[i].initMs else currentMs ?: stopwatches[i].currentMs
                stopwatches[i] = Stopwatch(id, stopwatches[i].initMs, isStarted)
                stopwatches[i].currentMs = cms
                stopwatches[i].isFinished = false

                startTime = stopwatches[i].currentMs

            }else if (stopwatches[i].isStarted && currentMs != -1L){
                cms = stopwatches[i].currentMs
                otherFinished = stopwatches[i].isFinished
                stopwatches[i] = Stopwatch(stopwatches[i].id, stopwatches[i].initMs, false)
                stopwatches[i].currentMs = cms
                stopwatches[i].isFinished = otherFinished
            }
        }

        stopwatchAdapter.submitList(stopwatches.toList())

        if (isStarted) {
            runningId = id

            timer?.cancel()
            timer = getCountDownTimer(stopwatches.find { it.id == id }!!, startTime)
            timer?.start()
        }
        else if (id == runningId) {
            timer?.cancel()

            runningId = -1
        }
    }

    private fun getCountDownTimer(stopwatch: Stopwatch, startTime: Long): CountDownTimer {
        return object : CountDownTimer(startTime , UNIT_ONE_S) {
            val diff = Bundle()
            override fun onTick(millisUntilFinished: Long) {
                if (stopwatch.currentMs == -1L || TASK_REMOVED)
                    cancel()
                else {
                    Log.d("TIMER", "millisUntilFinished="+millisUntilFinished.toString()
                            +" currentMs="+stopwatch.currentMs.toString())

                    stopwatch.currentMs = stopwatch.currentMs - UNIT_ONE_S//millisUntilFinished
                    diff.putLong("currentMs", millisUntilFinished)
                    stopwatchAdapter.notifyItemChanged(stopwatches.indexOf(stopwatch), diff)
                }
            }

            override fun onFinish() {
                Log.d("TIMER", "finish,"
                        +" currentMs="+stopwatch.currentMs.toString())

                stopwatch.currentMs = stopwatch.initMs
                stopwatch.isStarted = false
                stopwatch.isFinished = true
                diff.clear()
                diff.putBoolean("isFinished", true)
                finishToast(stopwatches.indexOf(stopwatches.find { it.id == runningId })+1)
                runningId = -1
                stopwatchAdapter.notifyItemChanged(stopwatches.indexOf(stopwatch), diff)
            }
        }
    }

    private fun finishToast(timerNo:Int){
        Toast.makeText(this,
            resources.getString(R.string.timer_finished, timerNo.toString()),
            Toast.LENGTH_SHORT).show()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (runningId != -1) { //do not start service when app launched on locked screen
            val startIntent = Intent(this, ForegroundService::class.java)
            startIntent.putExtra(COMMAND_ID, COMMAND_START)
            startIntent.putExtra(
                STARTED_TIMER_TIME_MS,
                stopwatches.find { it.id == runningId }?.currentMs
            )
            startIntent.putExtra(
                STARTED_TIMER_NO,
                stopwatches.indexOf(stopwatches.find { it.id == runningId })+1
            )
            startService(startIntent)

        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        if (stopwatches.size != 0) {
            val stopIntent = Intent(this, ForegroundService::class.java)
            stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
            startService(stopIntent)
        }
    }

    private val backInterval = UNIT_ONE_S+UNIT_ONE_S
    private var mBackPressed = 0L

    override fun onBackPressed() {
        if (mBackPressed + backInterval > System.currentTimeMillis()) {
            timer?.cancel()
            runningId = -1
            super.onBackPressed()
            return
        } else {
            Toast.makeText(baseContext, resources.getString(R.string.back_warning), Toast.LENGTH_SHORT)
                .show()
        }
        mBackPressed = System.currentTimeMillis()
    }
}