package com.freerepo.countdown

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Holds the countdown state across configuration changes (rotation). The
// state itself lives in CountdownLogic; this class adds a LiveData surface
// for the UI and a coroutine tick loop.
class CountdownViewModel : ViewModel() {

    private val logic = CountdownLogic(clock = { SystemClock.elapsedRealtime() })

    private val _remainingMs = MutableLiveData(0L)
    val remainingMs: LiveData<Long> = _remainingMs

    private val _state = MutableLiveData(CountdownLogic.State.IDLE)
    val state: LiveData<CountdownLogic.State> = _state

    // Fired once when a running timer hits zero. The Activity consumes this
    // to post a notification; it is cleared after the first read so a
    // rotation doesn't re-fire it.
    private val _finishedEvent = MutableLiveData<Boolean>()
    val finishedEvent: LiveData<Boolean> = _finishedEvent

    private var tickJob: Job? = null

    fun start(durationMs: Long) {
        logic.start(durationMs)
        publish()
        startTicker()
    }

    fun pause() {
        logic.pause()
        tickJob?.cancel()
        tickJob = null
        publish()
    }

    fun resume() {
        logic.resume()
        publish()
        startTicker()
    }

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        logic.reset()
        publish()
    }

    fun consumeFinishedEvent() {
        _finishedEvent.value = false
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive && logic.state == CountdownLogic.State.RUNNING) {
                val finishedNow = logic.poll()
                _remainingMs.value = logic.remainingMs()
                if (finishedNow) {
                    _state.value = logic.state
                    _finishedEvent.value = true
                    break
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun publish() {
        _state.value = logic.state
        _remainingMs.value = logic.remainingMs()
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }

    companion object {
        private const val TICK_INTERVAL_MS = 200L
    }
}
