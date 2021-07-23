package com.rsschool.pomodoro

data class Stopwatch(
    val id: Int,
    val initMs: Long,
    var isStarted: Boolean){

    var currentMs = initMs
    var isFinished = false
}
