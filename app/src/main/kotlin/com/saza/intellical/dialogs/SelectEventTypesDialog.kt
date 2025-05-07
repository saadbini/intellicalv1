package com.saza.intellical.dialogs

import androidx.appcompat.app.AlertDialog
import com.saza.intellical.activities.SimpleActivity
import com.saza.intellical.adapters.FilterEventTypeAdapter
import com.saza.intellical.databinding.DialogFilterEventTypesBinding
import com.saza.intellical.extensions.eventsHelper
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.setupDialogStuff
import com.saza.commons.extensions.viewBinding
import com.saza.intellical.R

class SelectEventTypesDialog(val activity: SimpleActivity, selectedEventTypes: Set<String>, val callback: (HashSet<String>) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogFilterEventTypesBinding::inflate)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
            binding.filterEventTypesList.adapter = FilterEventTypeAdapter(activity, it, selectedEventTypes)

            activity.getAlertDialogBuilder()
                .setPositiveButton(R.string.ok) { _, _ -> confirmEventTypes() }
                .setNegativeButton(R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        dialog = alertDialog
                    }
                }
        }
    }

    private fun confirmEventTypes() {
        val adapter = binding.filterEventTypesList.adapter as FilterEventTypeAdapter
        val selectedItems = adapter.getSelectedItemsList()
            .map { it.toString() }
            .toHashSet()
        callback(selectedItems)
        dialog?.dismiss()
    }
}
