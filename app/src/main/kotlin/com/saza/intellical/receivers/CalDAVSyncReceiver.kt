package com.saza.intellical.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saza.intellical.extensions.config
import com.saza.intellical.extensions.recheckCalDAVCalendars
import com.saza.intellical.extensions.refreshCalDAVCalendars
import com.saza.intellical.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars(true) {
            context.updateWidgets()
        }
    }
}
