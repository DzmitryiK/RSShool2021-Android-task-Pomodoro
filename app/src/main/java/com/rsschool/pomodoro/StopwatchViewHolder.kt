package com.rsschool.pomodoro

import android.content.res.Resources
import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.rsschool.pomodoro.databinding.StopwatchItemBinding
import androidx.lifecycle.*

class StopwatchViewHolder(
    private val binding: StopwatchItemBinding,
    private val listener: StopwatchListener,
    private val resources: Resources
): LifecycleObserver,RecyclerView.ViewHolder(binding.root) {

    //Default bind - when refreshing list
    fun bind(stopwatch: Stopwatch) {
        refreshCurrentState(stopwatch)

        if (stopwatch.isStarted) {
            startTimer()
        } else {
            stopTimer()
        }

        initButtonsListeners(stopwatch)
    }

    //Payloads bind - when updating timer
    fun bind(stopwatch: Stopwatch, payloads: List<Any>){
        binding.progressCircular.isInvisible = stopwatch.currentMs == stopwatch.initMs
        for (e in payloads ) {
            when {
                (e as Bundle).containsKey("isStarted") -> {
                    if (e.getBoolean("isStarted")) {
                        startTimer()
                    } else {
                        stopTimer()
                    }
                    refreshCurrentState(stopwatch)
                }
                e.containsKey("currentMs") -> {
                    refreshCurrentState(stopwatch)
                }
                e.containsKey("isFinished") -> {
                    refreshCurrentState(stopwatch)
                    stopTimer()
                }
            }
        }

        initButtonsListeners(stopwatch)
    }

    private fun initButtonsListeners(stopwatch: Stopwatch) {
        binding.startStopButton.setOnClickListener {
            if (stopwatch.isStarted) {
                listener.stop(stopwatch.id, stopwatch.currentMs)
            } else {
                listener.start(stopwatch.id)
            }
        }

        binding.restartButton.setOnClickListener { listener.reset(stopwatch.id) }

        binding.deleteButton.setOnClickListener { listener.delete(stopwatch.id) }
    }

    private fun startTimer() {
        binding.startStopButton.text = resources.getString(R.string.btn_start_stop)

        binding.blinkingIndicator.isInvisible = false
        val myVec = AnimatedVectorDrawableCompat.create(this.binding.root.context,R.drawable.blinking_circle_vector)
        binding.blinkingIndicator.setImageDrawable(myVec)
        myVec?.start()
    }

    private fun stopTimer() {
        binding.startStopButton.text =  resources.getString(R.string.btn_start)

        binding.blinkingIndicator.isInvisible = true
        (binding.blinkingIndicator.background as? AnimatedVectorDrawableCompat)?.stop()
    }

    private fun refreshCurrentState(stopwatch: Stopwatch){
        binding.stopwatchTimer.text = stopwatch.currentMs.displayTime()

        binding.progressCircular.setPeriod(stopwatch.initMs)
        binding.progressCircular.setCurrent(stopwatch.currentMs)
        binding.progressCircular.isInvisible = stopwatch.currentMs == stopwatch.initMs

        if (stopwatch.isFinished)
            binding.timerConstraint.setBackgroundColor(resources.getColor(R.color.red_a200_dark))
        else
            binding.timerConstraint.setBackgroundColor(resources.getColor(R.color.transparent))

    }

}