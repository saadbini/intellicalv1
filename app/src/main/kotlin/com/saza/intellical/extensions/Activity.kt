package com.saza.intellical.extensions

import android.app.Activity
import android.net.Uri
import com.saza.intellical.BuildConfig
import com.saza.intellical.activities.SimpleActivity
import com.saza.intellical.dialogs.CustomEventRepeatIntervalDialog
import com.saza.intellical.dialogs.ImportEventsDialog
import com.saza.intellical.helpers.*
import com.saza.intellical.models.Event
import com.saza.commons.activities.BaseSimpleActivity
import com.saza.commons.dialogs.RadioGroupDialog
import com.saza.commons.extensions.*
import com.saza.commons.helpers.ensureBackgroundThread
import com.saza.commons.models.RadioItem
import com.saza.intellical.R
import java.io.File
import java.io.FileOutputStream
import java.util.TreeSet

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsOrTasksWithIds(ids) as ArrayList<Event>
        if (events.isEmpty()) {
            toast(R.string.no_items_found)
        }

        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter(this).exportEvents(it, events, false) { result ->
                if (result == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) {
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}

fun SimpleActivity.tryImportEventsFromFile(uri: Uri, callback: (Boolean) -> Unit = {}) {
    when (uri.scheme) {
        "file" -> showImportEventsDialog(uri.path!!, callback)
        "content" -> {
            val tempFile = getTempFile()
            if (tempFile == null) {
                toast(R.string.unknown_error_occurred)
                return
            }

            try {
                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream!!.copyTo(out)
                showImportEventsDialog(tempFile.absolutePath, callback)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

        else -> toast(R.string.invalid_file_format)
    }
}

fun SimpleActivity.showImportEventsDialog(path: String, callback: (Boolean) -> Unit) {
    ImportEventsDialog(this, path, callback)
}
