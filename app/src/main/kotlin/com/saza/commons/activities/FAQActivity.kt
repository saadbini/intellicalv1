package com.saza.commons.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.saza.commons.compose.extensions.enableEdgeToEdgeSimple
import com.saza.commons.compose.screens.FAQScreen
import com.saza.commons.compose.theme.AppThemeSurface
import com.saza.commons.helpers.APP_FAQ
import com.saza.commons.models.FAQItem
import kotlinx.collections.immutable.toImmutableList

class FAQActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeSimple()
        setContent {
            AppThemeSurface {
                val faqItems = remember { intent.getSerializableExtra(APP_FAQ) as ArrayList<FAQItem> }
                FAQScreen(
                    goBack = ::finish,
                    faqItems = faqItems.toImmutableList()
                )
            }
        }
    }
}
