package com.saza.intellical.services

import android.app.IntentService
import android.content.Intent
import com.saza.intellical.extensions.eventsDB
import com.saza.intellical.extensions.updateTaskCompletion
import com.saza.intellical.helpers.ACTION_MARK_COMPLETED
import com.saza.intellical.helpers.EVENT_ID

class MarkCompletedService : IntentService("MarkCompleted") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action == ACTION_MARK_COMPLETED) {
            val taskId = intent.getLongExtra(EVENT_ID, 0L)
            val task = eventsDB.getTaskWithId(taskId)
            if (task != null) {
                updateTaskCompletion(task, completed = true)
            }
        }
    }
}
