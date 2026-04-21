package com.freerepo.countdown

// Pure-Kotlin countdown state machine. No Android dependencies so the
// behaviour can be exercised by plain JUnit tests.
//
// The caller feeds a monotonic clock (in milliseconds). In production
// wire this to SystemClock.elapsedRealtime(); in tests pass a fake.
class CountdownLogic(private val clock: () -> Long) {

    enum class State { IDLE, RUNNING, PAUSED, FINISHED }

    var state: State = State.IDLE
        private set

    var initialDurationMs: Long = 0L
        private set

    private var deadlineMs: Long = 0L
    private var pausedRemainingMs: Long = 0L

    // Remaining time at the current instant. Safe to call in any state.
    fun remainingMs(): Long = when (state) {
        State.IDLE -> initialDurationMs
        State.RUNNING -> (deadlineMs - clock()).coerceAtLeast(0L)
        State.PAUSED -> pausedRemainingMs
        State.FINISHED -> 0L
    }

    fun start(durationMs: Long) {
        require(durationMs > 0L) { "Duration must be positive" }
        initialDurationMs = durationMs
        deadlineMs = clock() + durationMs
        state = State.RUNNING
    }

    fun pause() {
        if (state != State.RUNNING) return
        pausedRemainingMs = remainingMs()
        state = if (pausedRemainingMs == 0L) State.FINISHED else State.PAUSED
    }

    fun resume() {
        if (state != State.PAUSED) return
        deadlineMs = clock() + pausedRemainingMs
        state = State.RUNNING
    }

    fun reset() {
        pausedRemainingMs = 0L
        deadlineMs = 0L
        state = State.IDLE
    }

    // Re-check whether a running timer has hit zero. Call from the UI tick.
    // Returns true the first time the timer transitions to FINISHED.
    fun poll(): Boolean {
        if (state != State.RUNNING) return false
        if (clock() >= deadlineMs) {
            state = State.FINISHED
            pausedRemainingMs = 0L
            return true
        }
        return false
    }
}
