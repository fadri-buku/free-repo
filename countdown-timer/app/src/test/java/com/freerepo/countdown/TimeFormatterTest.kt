package com.freerepo.countdown

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun zero_formats_as_all_zeros() {
        assertEquals("00:00:00", TimeFormatter.format(0L))
    }

    @Test
    fun negative_input_clamps_to_zero() {
        assertEquals("00:00:00", TimeFormatter.format(-1234L))
    }

    @Test
    fun ten_seconds_formats_as_ten_seconds() {
        assertEquals("00:00:10", TimeFormatter.format(10_000L))
    }

    @Test
    fun sub_second_rounds_up_to_full_second() {
        // A running timer with 9.3s left should still show 00:10 briefly.
        assertEquals("00:00:10", TimeFormatter.format(9_300L))
    }

    @Test
    fun one_minute_boundary() {
        assertEquals("00:01:00", TimeFormatter.format(60_000L))
    }

    @Test
    fun hours_minutes_seconds_compose() {
        val ms = (2 * 3600 + 34 * 60 + 7) * 1000L
        assertEquals("02:34:07", TimeFormatter.format(ms))
    }
}
