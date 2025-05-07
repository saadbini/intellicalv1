package com.saza.intellical.extensions

import com.saza.intellical.models.MonthViewEvent

fun MonthViewEvent.shouldStrikeThrough() = isTaskCompleted || isAttendeeInviteDeclined
