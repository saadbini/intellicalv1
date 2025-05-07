package com.saza.commons.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.*
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.saza.intellical.R
import com.saza.commons.compose.alert_dialog.rememberAlertDialogState
import com.saza.commons.compose.extensions.enableEdgeToEdgeSimple
import com.saza.commons.compose.extensions.rateStarsRedirectAndThankYou
import com.saza.commons.compose.screens.*
import com.saza.commons.compose.theme.AppThemeSurface
import com.saza.commons.dialogs.ConfirmationAdvancedAlertDialog
import com.saza.commons.dialogs.RateStarsAlertDialog
import com.saza.commons.extensions.*
import com.saza.commons.helpers.*
import com.saza.commons.models.FAQItem

class AboutActivity : ComponentActivity() {
    private val appName get() = intent.getStringExtra(APP_NAME) ?: ""

    private var firstVersionClickTS = 0L
    private var clicksSinceFirstClick = 0

    companion object {
        private const val EASTER_EGG_TIME_LIMIT = 3000L
        private const val EASTER_EGG_REQUIRED_CLICKS = 7
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeSimple()
        setContent {
            val context = LocalContext.current
            val resources = context.resources
            AppThemeSurface {
                val showExternalLinks = remember { !resources.getBoolean(R.bool.hide_all_external_links) }
                val showGoogleRelations = remember { !resources.getBoolean(R.bool.hide_google_relations) }
                val onEmailClickAlertDialogState = getOnEmailClickAlertDialogState()
                val rateStarsAlertDialogState = getRateStarsAlertDialogState()
                val onRateUsClickAlertDialogState = getOnRateUsClickAlertDialogState(rateStarsAlertDialogState::show)
                AboutScreen(
                    goBack = ::finish,
                    helpUsSection = {
                        val showHelpUsSection =
                            remember { showGoogleRelations || !showExternalLinks }
                        HelpUsSection(
                            onContributorsClick = ::onContributorsClick,
                            showInvite = showHelpUsSection
                        )
                    },
                    aboutSection = {
                        val setupFAQ = rememberFAQ()
                        if (!showExternalLinks || setupFAQ) {
                            AboutSection(setupFAQ = setupFAQ, onFAQClick = ::launchFAQActivity, onEmailClick = {
                                onEmailClick(onEmailClickAlertDialogState::show)
                            })
                        }
                    },
                    socialSection = {
                        if (showExternalLinks) {
                            SocialSection(
                                onFacebookClick = ::onFacebookClick,
                                onGithubClick = ::onGithubClick,
                                onRedditClick = ::onRedditClick,
                                onTelegramClick = ::onTelegramClick
                            )
                        }
                    }
                ) {
                    OtherSection(
                        showPrivacyPolicy = showExternalLinks,
                        onPrivacyPolicyClick = ::onPrivacyPolicyClick,
                        onLicenseClick = ::onLicenseClick,
                        onVersionClick = ::onVersionClick
                    )
                }
            }
        }
    }

    @Composable
    private fun rememberFAQ() = remember { !(intent.getSerializableExtra(APP_FAQ) as? ArrayList<FAQItem>).isNullOrEmpty() }




    @Composable
    private fun getRateStarsAlertDialogState() =
        rememberAlertDialogState().apply {
            DialogMember {
                RateStarsAlertDialog(alertDialogState = this, onRating = ::rateStarsRedirectAndThankYou)
            }
        }

    @Composable
    private fun getOnEmailClickAlertDialogState() =
        rememberAlertDialogState().apply {
            DialogMember {
                ConfirmationAdvancedAlertDialog(
                    alertDialogState = this,
                    message = "${getString(R.string.before_asking_question_read_faq)}\n\n${getString(R.string.make_sure_latest)}",
                    messageId = null,
                    positive = R.string.read_faq,
                    negative = R.string.skip
                ) {
                        launchFAQActivity()

                }
            }
        }

    @Composable
    private fun getOnRateUsClickAlertDialogState(showRateStarsDialog: () -> Unit) =
        rememberAlertDialogState().apply {
            DialogMember {
                ConfirmationAdvancedAlertDialog(
                    alertDialogState = this,
                    message = "${getString(R.string.before_asking_question_read_faq)}\n\n${getString(R.string.make_sure_latest)}",
                    messageId = null,
                    positive = R.string.read_faq,
                    negative = R.string.skip
                ) {

                        launchFAQActivity()


                }
            }
        }

    private fun onEmailClick(
        showConfirmationAdvancedDialog: () -> Unit
    ) {
        if (intent.getBooleanExtra(SHOW_FAQ_BEFORE_MAIL, false) && !baseConfig.wasBeforeAskingShown) {
            baseConfig.wasBeforeAskingShown = true
            showConfirmationAdvancedDialog()
        }
    }

    private fun launchFAQActivity() {
        val faqItems = intent.getSerializableExtra(APP_FAQ) as ArrayList<FAQItem>
        Intent(applicationContext, FAQActivity::class.java).apply {
            putExtra(APP_ICON_IDS, intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList<String>())
            putExtra(APP_LAUNCHER_NAME, intent.getStringExtra(APP_LAUNCHER_NAME) ?: "")
            putExtra(APP_FAQ, faqItems)
            startActivity(this)
        }
    }


    private fun onRateUsClick(
        showConfirmationAdvancedDialog: () -> Unit,
        showRateStarsDialog: () -> Unit
    ) {

            baseConfig.wasBeforeRateShown = true
            showConfirmationAdvancedDialog()

    }




    private fun onContributorsClick() {
        return
    }



    private fun onFacebookClick() {
        return
    }

    private fun onGithubClick() {
        return
    }

    private fun onRedditClick() {
        return
    }


    private fun onTelegramClick() {
        return
    }


    private fun onWebsiteClick() {
        return
    }

    private fun onPrivacyPolicyClick() {
        return
    }

    private fun onLicenseClick() {
        return
    }

    private fun onVersionClick() {
        if (firstVersionClickTS == 0L) {
            firstVersionClickTS = System.currentTimeMillis()
            Handler(Looper.getMainLooper()).postDelayed({
                firstVersionClickTS = 0L
                clicksSinceFirstClick = 0
            }, EASTER_EGG_TIME_LIMIT)
        }

        clicksSinceFirstClick++
        if (clicksSinceFirstClick >= EASTER_EGG_REQUIRED_CLICKS) {
            toast(R.string.hello)
            firstVersionClickTS = 0L
            clicksSinceFirstClick = 0
        }
    }
}
