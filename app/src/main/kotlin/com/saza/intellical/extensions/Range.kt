package com.saza.intellical.extensions

import android.util.Range

fun Range<Int>.intersects(other: Range<Int>) = (upper >= other.lower && lower <= other.upper) || (other.upper >= lower && other.lower <= upper)
