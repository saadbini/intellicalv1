package com.saza.intellical.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saza.intellical.extensions.config
import com.saza.intellical.extensions.eventsDB
import com.saza.intellical.extensions.rescheduleReminder
import com.saza.intellical.helpers.EVENT_ID
import com.saza.commons.extensions.showPickSecondsDialogHelper
import com.saza.commons.helpers.ensureBackgroundThread

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true, cancelCallback = { dialogCancelled() }) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = eventsDB.getEventOrTaskWithId(eventId)
                config.snoozeTime = it / 60
                rescheduleReminder(event, it / 60)
                runOnUiThread {
                    finishActivity()
                }
            }
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
