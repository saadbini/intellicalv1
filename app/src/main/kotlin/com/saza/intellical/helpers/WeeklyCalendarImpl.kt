package com.saza.intellical.helpers

import android.content.Context
import com.saza.intellical.extensions.eventsHelper
import com.saza.intellical.interfaces.WeeklyCalendar
import com.saza.intellical.models.Event
import com.saza.commons.helpers.DAY_SECONDS
import com.saza.commons.helpers.WEEK_SECONDS
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Long) {
        val endTS = weekStartTS + 2 * WEEK_SECONDS
        context.eventsHelper.getEvents(weekStartTS - DAY_SECONDS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}
