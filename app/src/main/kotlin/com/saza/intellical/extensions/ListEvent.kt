package com.saza.intellical.extensions

import com.saza.intellical.models.ListEvent

fun ListEvent.shouldStrikeThrough() = isTaskCompleted || isAttendeeInviteDeclined
