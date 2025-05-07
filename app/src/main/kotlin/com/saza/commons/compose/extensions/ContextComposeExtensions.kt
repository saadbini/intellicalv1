package com.saza.commons.compose.extensions

import android.app.Activity
import android.content.Context
import com.saza.intellical.R
import com.saza.commons.extensions.baseConfig
import com.saza.commons.extensions.redirectToRateUs
import com.saza.commons.extensions.toast
import com.saza.commons.helpers.BaseConfig

val Context.config: BaseConfig get() = BaseConfig.newInstance(applicationContext)

fun Activity.rateStarsRedirectAndThankYou(stars: Int) {
    if (stars == 5) {
        redirectToRateUs()
    }
    toast(R.string.thank_you)

}
