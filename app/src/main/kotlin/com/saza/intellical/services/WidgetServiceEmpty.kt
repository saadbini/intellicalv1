package com.saza.intellical.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.saza.intellical.adapters.EventListWidgetAdapterEmpty

class WidgetServiceEmpty : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapterEmpty(applicationContext)
}
