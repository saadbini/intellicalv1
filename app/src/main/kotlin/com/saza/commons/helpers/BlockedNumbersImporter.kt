package com.saza.commons.helpers

import android.app.Activity
import android.telephony.PhoneNumberUtils
import com.saza.commons.extensions.addBlockedNumber
import com.saza.commons.extensions.showErrorToast
import java.io.File

class BlockedNumbersImporter(
    private val activity: Activity,
) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK
    }

    fun importBlockedNumbers(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val numbers = inputStream.bufferedReader().use {
                val content = it.readText().trimEnd().split(BLOCKED_NUMBERS_EXPORT_DELIMITER)
                content.filter { text -> PhoneNumberUtils.isGlobalPhoneNumber(text) }
            }
            if (numbers.isNotEmpty()) {
                numbers.forEach { number ->
                    activity.addBlockedNumber(number)
                }
                ImportResult.IMPORT_OK
            } else {
                ImportResult.IMPORT_FAIL
            }

        } catch (e: Exception) {
            activity.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }
}
