package com.saza.commons.compose

/**
 * This file contains common opt-in annotations for experimental APIs used in the project.
 * These annotations can be used at the file level to suppress experimental API warnings.
 */

// Material 3 experimental APIs
@RequiresOptIn(message = "This material API is experimental and is likely to change or to be removed in the future.")
annotation class ExperimentalMaterial3Api

// Foundation experimental APIs
@RequiresOptIn(message = "This foundation API is experimental and is likely to change or be removed in the future.")
annotation class ExperimentalFoundationApi

// Glide Compose experimental APIs
@RequiresOptIn(message = "Glide's Compose integration is experimental. APIs may change or be removed without warning.")
annotation class ExperimentalGlideComposeApi
