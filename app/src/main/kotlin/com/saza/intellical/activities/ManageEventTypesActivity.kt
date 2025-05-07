package com.saza.intellical.activities

import android.os.Bundle
import com.saza.intellical.R
import com.saza.intellical.adapters.ManageEventTypesAdapter
import com.saza.intellical.databinding.ActivityManageEventTypesBinding
import com.saza.intellical.dialogs.EditEventTypeDialog
import com.saza.intellical.extensions.eventsHelper
import com.saza.intellical.interfaces.DeleteEventTypesListener
import com.saza.intellical.models.EventType
import com.saza.commons.extensions.toast
import com.saza.commons.extensions.updateTextColors
import com.saza.commons.extensions.viewBinding
import com.saza.commons.helpers.NavigationIcon
import com.saza.commons.helpers.ensureBackgroundThread

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {

    private val binding by viewBinding(ActivityManageEventTypesBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        updateMaterialActivityViews(
            binding.manageEventTypesCoordinator,
            binding.manageEventTypesList,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.manageEventTypesList, binding.manageEventTypesToolbar)

        getEventTypes()
        updateTextColors(binding.manageEventTypesList)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.manageEventTypesToolbar, NavigationIcon.Arrow)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        EditEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        eventsHelper.getEventTypes(this, false) {
            val adapter = ManageEventTypesAdapter(this, it, this, binding.manageEventTypesList) {
                showEventTypeDialog(it as EventType)
            }
            binding.manageEventTypesList.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        binding.manageEventTypesToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_event_type -> showEventTypeDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean {
        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
            if (eventTypes.size == 1) {
                return false
            }
        }

        ensureBackgroundThread {
            eventsHelper.deleteEventTypes(eventTypes, deleteEvents)
        }

        return true
    }
}
