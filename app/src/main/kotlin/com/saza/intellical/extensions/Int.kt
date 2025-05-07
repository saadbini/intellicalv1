package com.saza.intellical.extensions

import com.saza.intellical.helpers.MONTH
import com.saza.intellical.helpers.WEEK
import com.saza.intellical.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
