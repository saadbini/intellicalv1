package com.saza.intellical.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.saza.intellical.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapter(applicationContext, intent)
}
