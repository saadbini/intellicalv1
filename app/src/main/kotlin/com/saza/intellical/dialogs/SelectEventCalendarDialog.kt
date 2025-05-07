package com.saza.intellical.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.saza.intellical.R
import com.saza.intellical.databinding.DialogSelectRadioGroupBinding
import com.saza.intellical.databinding.RadioButtonWithColorBinding
import com.saza.intellical.extensions.eventsHelper
import com.saza.intellical.helpers.STORED_LOCALLY_ONLY
import com.saza.intellical.models.CalDAVCalendar
import com.saza.commons.extensions.*
import com.saza.commons.helpers.ensureBackgroundThread

class SelectEventCalendarDialog(val activity: Activity, val calendars: List<CalDAVCalendar>, val currCalendarId: Int, val callback: (id: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val binding by activity.viewBinding(DialogSelectRadioGroupBinding::inflate)

    init {
        radioGroup = binding.dialogRadioGroup

        ensureBackgroundThread {
            calendars.forEach {
                val localEventType = activity.eventsHelper.getEventTypeWithCalDAVCalendarId(it.id)
                if (localEventType != null) {
                    it.color = localEventType.color
                }
            }

            activity.runOnUiThread {
                calendars.forEach {
                    addRadioButton(it.getFullTitle(), it.id, it.color)
                }
                addRadioButton(activity.getString(R.string.store_locally_only), STORED_LOCALLY_ONLY, Color.TRANSPARENT)
                wasInit = true
                activity.updateTextColors(binding.dialogRadioHolder)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addRadioButton(title: String, typeId: Int, color: Int) {
        val radioBinding = RadioButtonWithColorBinding.inflate(activity.layoutInflater)
        radioBinding.dialogRadioButton.apply {
            text = title
            isChecked = typeId == currCalendarId
            id = typeId
        }

        if (typeId != STORED_LOCALLY_ONLY) {
            radioBinding.dialogRadioColor.setFillWithStroke(color, activity.getProperBackgroundColor())
        }

        radioBinding.root.setOnClickListener { viewClicked(typeId) }
        radioGroup.addView(radioBinding.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(typeId: Int) {
        if (wasInit) {
            callback(typeId)
            dialog?.dismiss()
        }
    }
}
