package com.saza.intellical.interfaces

import android.util.SparseArray
import com.saza.intellical.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
