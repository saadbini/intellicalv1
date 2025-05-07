package com.saza.intellical.dialogs

import com.saza.intellical.databinding.DatetimePatternInfoLayoutBinding
import com.saza.commons.activities.BaseSimpleActivity
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.setupDialogStuff
import com.saza.commons.extensions.viewBinding
import com.saza.intellical.R

class DateTimePatternInfoDialog(activity: BaseSimpleActivity) {
    val binding by activity.viewBinding(DatetimePatternInfoLayoutBinding::inflate)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
