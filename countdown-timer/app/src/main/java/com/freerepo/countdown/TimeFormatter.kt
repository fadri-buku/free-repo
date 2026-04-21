package com.freerepo.countdown

import java.util.Locale
import kotlin.math.ceil

object TimeFormatter {

    // Formats a millisecond value as HH:MM:SS. Any remainder of a second
    // counts as a full second so the user sees "00:10" the moment they
    // press Start on a 10-second timer, not "00:09".
    fun format(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val totalSeconds = ceil(safeMillis / 1000.0).toLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
