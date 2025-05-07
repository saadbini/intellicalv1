package com.saza.commons.dialogs

import android.view.animation.AnimationUtils
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saza.commons.activities.BaseSimpleActivity
import com.saza.commons.compose.alert_dialog.*
import com.saza.commons.compose.extensions.MyDevices
import com.saza.commons.compose.theme.AppThemeSurface
import com.saza.intellical.databinding.DialogCallConfirmationBinding
import com.saza.commons.extensions.applyColorFilter
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.getProperTextColor
import com.saza.commons.extensions.setupDialogStuff
import com.saza.intellical.R

class CallConfirmationDialog(val activity: BaseSimpleActivity, val callee: String, private val callback: () -> Unit) {
    private var view = DialogCallConfirmationBinding.inflate(activity.layoutInflater, null, false)

    init {
        view.callConfirmPhone.applyColorFilter(activity.getProperTextColor())
        activity.getAlertDialogBuilder()
            .setNegativeButton(R.string.cancel, null)
            .apply {
                val title = String.format(activity.getString(R.string.confirm_calling_person), callee)
                activity.setupDialogStuff(view.root, this, titleText = title) { alertDialog ->
                    view.callConfirmPhone.apply {
                        startAnimation(AnimationUtils.loadAnimation(activity, R.anim.shake_pulse_animation))
                        setOnClickListener {
                            callback.invoke()
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}

@Composable
fun CallConfirmationAlertDialog(
    alertDialogState: AlertDialogState,
    callee: String,
    modifier: Modifier = Modifier,
    callback: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        containerColor = dialogContainerColor,
        modifier = modifier
            .dialogBorder,
        onDismissRequest = {
            alertDialogState.hide()
            callback()
        },
        shape = dialogShape,
        tonalElevation = dialogElevation,
        confirmButton = {
            TextButton(onClick = {
                alertDialogState.hide()
                callback()
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        title = {
            val title = String.format(stringResource(R.string.confirm_calling_person, callee))
            Text(
                text = title,
                color = dialogTextColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            val scale by rememberInfiniteTransition("infiniteTransition").animateFloat(
                initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(1000)
                ), label = "scale anim"
            )
            val rotate by rememberInfiniteTransition("rotate").animateFloat(
                initialValue = -5f, targetValue = 5f, animationSpec = infiniteRepeatable(
                    animation = tween(200),
                    repeatMode = RepeatMode.Reverse
                ), label = "rotate anim"
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            alertDialogState.hide()
                            callback()
                        }
                        .padding(16.dp)
                ) {
                    Image(
                        Icons.Filled.Call,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(dialogTextColor),
                        modifier = Modifier
                            .matchParentSize()
                            .scale(scale)
                            .rotate(rotate),
                    )
                }
            }
        }
    )
}

@Composable
@MyDevices
private fun CallConfirmationAlertDialogPreview() {
    AppThemeSurface {
        CallConfirmationAlertDialog(
            alertDialogState = rememberAlertDialogState(),
            callee = "SAZA"
        ) {}
    }
}
