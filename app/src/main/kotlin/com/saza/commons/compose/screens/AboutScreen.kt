package com.saza.commons.compose.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saza.intellical.R
import com.saza.commons.compose.extensions.MyDevices
import com.saza.commons.compose.lists.SimpleColumnScaffold
import com.saza.commons.compose.settings.SettingsGroup
import com.saza.commons.compose.settings.SettingsHorizontalDivider
import com.saza.commons.compose.settings.SettingsListItem
import com.saza.commons.compose.settings.SettingsTitleTextComponent
import com.saza.commons.compose.theme.AppThemeSurface
import com.saza.commons.compose.theme.SimpleTheme

private val startingTitlePadding = Modifier.padding(start = 60.dp)

@Composable
internal fun AboutScreen(
    goBack: () -> Unit,
    helpUsSection: @Composable () -> Unit,
    aboutSection: @Composable () -> Unit,
    socialSection: @Composable () -> Unit,
    otherSection: @Composable () -> Unit,
) {
    SimpleColumnScaffold(title = stringResource(id = R.string.about), goBack = goBack) {
        aboutSection()
        helpUsSection()
        socialSection()
        otherSection()
        SettingsListItem(text = stringResource(id = R.string.copyright))
    }
}

@Composable
internal fun HelpUsSection(
    onContributorsClick: () -> Unit,
    showInvite: Boolean,
) {
    SettingsGroup(title = {
        SettingsTitleTextComponent(text = stringResource(id = R.string.help_us), modifier = startingTitlePadding)
    }) {


        TwoLinerTextItem(
            click = onContributorsClick,
            text = stringResource(id = R.string.contributors),
            icon = R.drawable.ic_face_vector
        )

        SettingsHorizontalDivider()
    }
}

@Composable
internal fun OtherSection(
    showPrivacyPolicy: Boolean,
    onPrivacyPolicyClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    SettingsGroup(title = {
        SettingsTitleTextComponent(text = stringResource(id = R.string.other), modifier = startingTitlePadding)
    }) {


        if (showPrivacyPolicy) {
            TwoLinerTextItem(
                click = onPrivacyPolicyClick,
                text = stringResource(id = R.string.privacy_policy),
                icon = R.drawable.ic_unhide_vector
            )
        }
        TwoLinerTextItem(
            click = onLicenseClick,
            text = stringResource(id = R.string.third_party_licences),
            icon = R.drawable.ic_article_vector
        )
        TwoLinerTextItem(
            click = onVersionClick,
            text = R.string.copyright.toString(),
            icon = R.drawable.ic_info_vector
        )
        SettingsHorizontalDivider()
    }
}


@Composable
internal fun AboutSection(
    setupFAQ: Boolean,
    onFAQClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    SettingsGroup(title = {
        SettingsTitleTextComponent(text = stringResource(id = R.string.support), modifier = startingTitlePadding)
    }) {
        if (setupFAQ) {
            TwoLinerTextItem(
                click = onFAQClick,
                text = stringResource(id = R.string.frequently_asked_questions),
                icon = R.drawable.ic_question_mark_vector
            )
        }
        TwoLinerTextItem(
            click = onEmailClick,
            text = stringResource(id = R.string.my_status),
            icon = R.drawable.ic_mail_vector
        )
        SettingsHorizontalDivider()
    }
}

@Composable
internal fun SocialSection(
    onFacebookClick: () -> Unit,
    onGithubClick: () -> Unit,
    onRedditClick: () -> Unit,
    onTelegramClick: () -> Unit
) {
    SettingsGroup(title = {
        SettingsTitleTextComponent(text = stringResource(id = R.string.social), modifier = startingTitlePadding)
    }) {
        SocialText(
            click = onFacebookClick,
            text = stringResource(id = R.string.about),
            icon = R.drawable.ic_facebook_vector,
        )
        SocialText(
            click = onGithubClick,
            text = stringResource(id = R.string.my_status),
            icon = R.drawable.ic_github_vector,
            tint = SimpleTheme.colorScheme.onSurface
        )
        SocialText(
            click = onRedditClick,
            text = stringResource(id = R.string.copyright),
            icon = R.drawable.ic_reddit_vector,
        )
        SocialText(
            click = onTelegramClick,
            text = stringResource(id = R.string.copyright),
            icon = R.drawable.ic_telegram_vector,
        )
        SettingsHorizontalDivider()
    }
}

@Composable
internal fun SocialText(
    text: String,
    icon: Int,
    tint: Color? = null,
    click: () -> Unit
) {
    SettingsListItem(
        click = click,
        text = text,
        icon = icon,
        isImage = true,
        tint = tint,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun TwoLinerTextItem(text: String, icon: Int, click: () -> Unit) {
    SettingsListItem(
        tint = SimpleTheme.colorScheme.onSurface,
        click = click,
        text = text,
        icon = icon,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@MyDevices
@Composable
private fun AboutScreenPreview() {
    AppThemeSurface {
        AboutScreen(
            goBack = {},
            helpUsSection = {
                HelpUsSection(
                    onContributorsClick = {},
                    showInvite = true
                )
            },
            aboutSection = {
                AboutSection(setupFAQ = true, onFAQClick = {}, onEmailClick = {})
            },
            socialSection = {
                SocialSection(
                    onFacebookClick = {},
                    onGithubClick = {},
                    onRedditClick = {},
                    onTelegramClick = {}
                )
            }
        ) {
            OtherSection(
                showPrivacyPolicy = true,
                onPrivacyPolicyClick = {},
                onLicenseClick = {},
                onVersionClick = {}
            )
        }
    }
}
