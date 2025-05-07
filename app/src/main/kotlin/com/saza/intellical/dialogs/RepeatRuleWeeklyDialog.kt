package com.saza.intellical.dialogs

import android.app.Activity
import com.saza.intellical.databinding.DialogVerticalLinearLayoutBinding
import com.saza.intellical.databinding.MyCheckboxBinding
import com.saza.intellical.extensions.withFirstDayOfWeekToFront
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.setupDialogStuff
import com.saza.commons.extensions.viewBinding
import com.saza.commons.views.MyAppCompatCheckbox
import com.saza.intellical.R

class RepeatRuleWeeklyDialog(val activity: Activity, val curRepeatRule: Int, val callback: (repeatRule: Int) -> Unit) {
    private val binding by activity.viewBinding(DialogVerticalLinearLayoutBinding::inflate)

    init {
        val days = activity.resources.getStringArray(R.array.week_days)
        var checkboxes = ArrayList<MyAppCompatCheckbox>(7)
        for (i in 0..6) {
            val pow = Math.pow(2.0, i.toDouble()).toInt()
            MyCheckboxBinding.inflate(activity.layoutInflater).root.apply {
                isChecked = curRepeatRule and pow != 0
                text = days[i]
                id = pow
                checkboxes.add(this)
            }
        }

        checkboxes = activity.withFirstDayOfWeekToFront(checkboxes)
        checkboxes.forEach {
            binding.dialogVerticalLinearLayout.addView(it)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> callback(getRepeatRuleSum()) }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun getRepeatRuleSum(): Int {
        var sum = 0
        val cnt = binding.dialogVerticalLinearLayout.childCount
        for (i in 0 until cnt) {
            val child = binding.dialogVerticalLinearLayout.getChildAt(i)
            if (child is MyAppCompatCheckbox) {
                if (child.isChecked)
                    sum += child.id
            }
        }
        return sum
    }
}
