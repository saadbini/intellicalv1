package com.saza.commons.dialogs

import android.app.Activity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.saza.intellical.R
import com.saza.commons.compose.alert_dialog.*
import com.saza.commons.compose.components.LinkifyTextComponent
import com.saza.commons.compose.extensions.MyDevices
import com.saza.commons.compose.extensions.rememberMutableInteractionSource
import com.saza.commons.compose.theme.AppThemeSurface
import com.saza.commons.compose.theme.SimpleTheme
import com.saza.intellical.databinding.DialogFeatureLockedBinding
import com.saza.commons.extensions.*

class FeatureLockedDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = DialogFeatureLockedBinding.inflate(activity.layoutInflater, null, false)
        view.featureLockedImage.applyColorFilter(activity.getProperTextColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.purchase, null)
            .setNegativeButton(R.string.later) { _, _ -> dismissDialog() }
            .setOnDismissListener { dismissDialog() }
            .apply {
                activity.setupDialogStuff(view.root, this, cancelOnTouchOutside = false) { alertDialog ->
                    dialog = alertDialog
                    view.featureLockedDescription.text = Html.fromHtml(activity.getString(R.string.feature_locked))
                    view.featureLockedDescription.movementMethod = LinkMovementMethod.getInstance()


                }
            }
    }

    fun dismissDialog() {
        dialog?.dismiss()
        callback()
    }
}




