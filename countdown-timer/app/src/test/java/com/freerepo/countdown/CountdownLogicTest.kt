package com.freerepo.countdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownLogicTest {

    // A FakeClock lets tests step time forward deterministically.
    private class FakeClock(var now: Long = 0L) {
        val lambda: () -> Long = { now }
        fun advance(ms: Long) {
            now += ms
        }
    }

    @Test
    fun starts_in_idle_state() {
        val logic = CountdownLogic(FakeClock().lambda)
        assertEquals(CountdownLogic.State.IDLE, logic.state)
        assertEquals(0L, logic.remainingMs())
    }

    @Test
    fun start_runs_and_ticks_down() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(10_000L)
        assertEquals(CountdownLogic.State.RUNNING, logic.state)
        assertEquals(10_000L, logic.remainingMs())

        clock.advance(3_000L)
        assertEquals(7_000L, logic.remainingMs())
    }

    @Test
    fun pause_freezes_remaining_time() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(10_000L)
        clock.advance(4_000L)
        logic.pause()

        assertEquals(CountdownLogic.State.PAUSED, logic.state)
        assertEquals(6_000L, logic.remainingMs())

        // Clock keeps moving but remaining does not.
        clock.advance(5_000L)
        assertEquals(6_000L, logic.remainingMs())
    }

    @Test
    fun resume_continues_from_paused_remainder() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(10_000L)
        clock.advance(4_000L)
        logic.pause()
        clock.advance(60_000L) // a long break
        logic.resume()

        assertEquals(CountdownLogic.State.RUNNING, logic.state)
        assertEquals(6_000L, logic.remainingMs())

        clock.advance(2_000L)
        assertEquals(4_000L, logic.remainingMs())
    }

    @Test
    fun reset_returns_to_initial_duration() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(10_000L)
        clock.advance(3_000L)
        logic.pause()
        logic.reset()

        assertEquals(CountdownLogic.State.IDLE, logic.state)
        assertEquals(10_000L, logic.remainingMs())
    }

    @Test
    fun poll_transitions_to_finished_exactly_once() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(5_000L)

        clock.advance(4_000L)
        assertFalse(logic.poll())
        assertEquals(CountdownLogic.State.RUNNING, logic.state)

        clock.advance(1_000L)
        assertTrue(logic.poll())
        assertEquals(CountdownLogic.State.FINISHED, logic.state)
        assertEquals(0L, logic.remainingMs())

        // Second poll should not re-fire the transition.
        assertFalse(logic.poll())
    }

    @Test
    fun pause_past_deadline_lands_on_finished() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.start(2_000L)
        clock.advance(5_000L)
        logic.pause()
        assertEquals(CountdownLogic.State.FINISHED, logic.state)
    }

    @Test
    fun start_rejects_zero_or_negative_duration() {
        val logic = CountdownLogic(FakeClock().lambda)
        var threw = false
        try {
            logic.start(0L)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun resume_while_not_paused_is_a_no_op() {
        val clock = FakeClock()
        val logic = CountdownLogic(clock.lambda)
        logic.resume()
        assertEquals(CountdownLogic.State.IDLE, logic.state)

        logic.start(1_000L)
        logic.resume()
        assertEquals(CountdownLogic.State.RUNNING, logic.state)
    }
}
