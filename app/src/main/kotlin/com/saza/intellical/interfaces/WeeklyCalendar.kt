package com.saza.intellical.interfaces

import com.saza.intellical.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
