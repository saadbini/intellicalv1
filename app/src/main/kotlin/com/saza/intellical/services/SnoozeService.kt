package com.saza.intellical.services

import android.app.IntentService
import android.content.Intent
import com.saza.intellical.extensions.config
import com.saza.intellical.extensions.eventsDB
import com.saza.intellical.extensions.rescheduleReminder
import com.saza.intellical.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventOrTaskWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}
