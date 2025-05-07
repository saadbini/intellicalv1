package com.saza.intellical.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.saza.intellical.databinding.DialogReminderWarningBinding
import com.saza.commons.extensions.*
import com.saza.intellical.R

class ReminderWarningDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogReminderWarningBinding::inflate)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNeutralButton(R.string.settings, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.disclaimer, cancelOnTouchOutside = false) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        redirectToSettings()
                    }
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }

    private fun redirectToSettings() {
        activity.hideKeyboard()
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)

            try {
                activity.startActivity(this)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }
}
