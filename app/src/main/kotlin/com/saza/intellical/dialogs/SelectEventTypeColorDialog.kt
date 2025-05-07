package com.saza.intellical.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.saza.intellical.R
import com.saza.intellical.adapters.CheckableColorAdapter
import com.saza.intellical.databinding.DialogSelectColorBinding
import com.saza.intellical.views.AutoGridLayoutManager
import com.saza.commons.dialogs.ColorPickerDialog
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.setupDialogStuff
import com.saza.commons.extensions.viewBinding

class SelectEventTypeColorDialog(val activity: Activity, val colors: IntArray, var currentColor: Int, val callback: (color: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogSelectColorBinding::inflate)

    init {
        val colorAdapter = CheckableColorAdapter(activity, colors, currentColor) { color ->
            callback(color)
            dialog?.dismiss()
        }

        binding.colorGrid.apply {
            val width = activity.resources.getDimensionPixelSize(R.dimen.smaller_icon_size)
            val spacing = activity.resources.getDimensionPixelSize(R.dimen.small_margin) * 2
            layoutManager = AutoGridLayoutManager(context = activity, itemWidth = width + spacing)
            adapter = colorAdapter
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.color) {
                    dialog = it
                }

                if (colors.isEmpty()) {
                    showCustomColorPicker()
                }
            }
    }

    private fun showCustomColorPicker() {
        ColorPickerDialog(activity, currentColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                callback(color)
            }

            dialog?.dismiss()
        }
    }
}
