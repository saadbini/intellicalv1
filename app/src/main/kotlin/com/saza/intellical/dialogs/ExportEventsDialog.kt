package com.saza.intellical.dialogs

import androidx.appcompat.app.AlertDialog
import com.saza.intellical.R
import com.saza.intellical.activities.SimpleActivity
import com.saza.intellical.adapters.FilterEventTypeAdapter
import com.saza.intellical.databinding.DialogExportEventsBinding
import com.saza.intellical.extensions.config
import com.saza.intellical.extensions.eventsHelper
import com.saza.commons.dialogs.FilePickerDialog
import com.saza.commons.extensions.*
import com.saza.commons.helpers.ensureBackgroundThread
import java.io.File

class ExportEventsDialog(
    val activity: SimpleActivity, val path: String, val hidePath: Boolean,
    val callback: (file: File, eventTypes: ArrayList<Long>) -> Unit
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config
    private val binding by activity.viewBinding(DialogExportEventsBinding::inflate)

    init {
        binding.apply {
            exportEventsFolder.setText(activity.humanizePath(realPath))
            exportEventsFilename.setText("${activity.getString(R.string.events)}_${activity.getCurrentFormattedDateTime()}")

            exportEventsCheckbox.isChecked = config.exportEvents
            exportEventsCheckboxHolder.setOnClickListener {
                exportEventsCheckbox.toggle()
            }
            exportTasksCheckbox.isChecked = config.exportTasks
            exportTasksCheckboxHolder.setOnClickListener {
                exportTasksCheckbox.toggle()
            }
            exportPastEventsCheckbox.isChecked = config.exportPastEntries
            exportPastEventsCheckboxHolder.setOnClickListener {
                exportPastEventsCheckbox.toggle()
            }

            if (hidePath) {
                exportEventsFolderHint.beGone()
                exportEventsFolder.beGone()
            } else {
                exportEventsFolder.setOnClickListener {
                    activity.hideKeyboard(exportEventsFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportEventsFolder.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }

            activity.eventsHelper.getEventTypes(activity, false) {
                val eventTypes = HashSet<String>()
                it.mapTo(eventTypes) { it.id.toString() }

                exportEventsTypesList.adapter = FilterEventTypeAdapter(activity, it, eventTypes)
                if (it.size > 1) {
                    exportEventsPickTypes.beVisible()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_events) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportEventsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.ics")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val exportEventsChecked = binding.exportEventsCheckbox.isChecked
                                val exportTasksChecked = binding.exportTasksCheckbox.isChecked
                                if (!exportEventsChecked && !exportTasksChecked) {
                                    activity.toast(R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.apply {
                                        lastExportPath = file.absolutePath.getParentPath()
                                        exportEvents = exportEventsChecked
                                        exportTasks = exportTasksChecked
                                        exportPastEntries = binding.exportPastEventsCheckbox.isChecked
                                    }

                                    val eventTypes = (binding.exportEventsTypesList.adapter as FilterEventTypeAdapter).getSelectedItemsList()
                                    callback(file, eventTypes)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
