package com.saza.intellical.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.saza.intellical.R
import com.saza.intellical.databinding.ActivityNaturalLanguageEventBinding
import com.saza.intellical.helpers.*
import com.saza.commons.extensions.getProperPrimaryColor
import com.saza.commons.extensions.getProperTextColor
import com.saza.commons.extensions.hideKeyboard
import com.saza.commons.extensions.showKeyboard
import com.saza.commons.extensions.toast
import com.saza.commons.extensions.viewBinding
import java.util.*

// Constants required for intent extras
const val TITLE = "title"
const val EVENT_REPEAT_INTERVAL = "repeat_interval"
const val EVENT_REPEAT_RULE = "repeat_rule"
const val NEW_EVENT_END_TS = "new_event_end_ts"
const val NEW_EVENT_START_TS = "new_event_start_ts"
const val EVENT_ID = "event_id"
const val NEW_EVENT_SET_HOUR_DURATION = "new_event_set_hour_duration"
const val LOCATION = "location"
const val DESCRIPTION = "description"

class NaturalLanguageEventActivity : SimpleActivity() {
    companion object {
        private const val SPEECH_REQUEST_CODE = 1000
        private const val PERMISSION_RECORD_AUDIO = 1001
    }

    private val binding by viewBinding(ActivityNaturalLanguageEventBinding::inflate)
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private var isProcessing = false
    private var contextTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Get the context timestamp passed from MainActivity (if available)
        contextTimestamp = intent.getLongExtra(NEW_EVENT_START_TS, System.currentTimeMillis())

        setupToolbar()
        updateColors()
        setupButtons()

        naturalLanguageParser = NaturalLanguageParser(this, contextTimestamp)
        binding.naturalLanguageInput.requestFocus()
        showKeyboard(binding.naturalLanguageInput)
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.create_event_with_text)
        binding.toolbar.setNavigationOnClickListener {
            hideKeyboard()
            finish()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.create_event -> {
                    processNaturalLanguageInput()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateColors() {
        val textColor = getProperTextColor()
        binding.micButton.backgroundTintList = android.content.res.ColorStateList.valueOf(textColor)
        binding.parseButton.backgroundTintList = android.content.res.ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun setupButtons() {
        binding.parseButton.setOnClickListener {
            processNaturalLanguageInput()
        }

        binding.micButton.setOnClickListener {
            //if (SpeechRecognizer.isRecognitionAvailable(this)) {
               checkMicrophonePermission()
            //} else {
            //    toast(R.string.voice_recognition_unavailable)
            //}
        }
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user
                toast(R.string.no_microphone_permission)
                // After the user sees the explanation, try again to request the permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_RECORD_AUDIO)
            } else {
                // No explanation needed, request the permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_RECORD_AUDIO)
            }
        } else {
            // Permission has already been granted
            startVoiceRecognition()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_RECORD_AUDIO -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted
                    startVoiceRecognition()
                } else {
                    // Permission denied
                    toast(R.string.no_microphone_permission)
                }
                return
            }
            else -> {
                // Ignore all other requests
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_event_details))
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            toast(R.string.voice_recognition_unavailable)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                binding.naturalLanguageInput.setText(results[0])
                processNaturalLanguageInput()
            }
        }
    }

    private fun processNaturalLanguageInput() {
        val input = binding.naturalLanguageInput.text.toString().trim()
        if (input.isEmpty()) {
            toast(R.string.please_enter_event_details)
            return
        }

        if (isProcessing) return

        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.parseButton.isEnabled = false
        binding.micButton.isEnabled = false

        naturalLanguageParser.parseEventText(input) { parsedEvent, error ->
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.parseButton.isEnabled = true
                binding.micButton.isEnabled = true
                isProcessing = false

                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (parsedEvent != null) {
                    openEventActivity(parsedEvent)
                } else {
                    Toast.makeText(this, R.string.could_not_parse_event, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openEventActivity(parsedEvent: NaturalLanguageParser.ParsedEvent) {
        hideKeyboard()

        // Get the current date and time
        val currentDateTime = DateTime.now()

        // For "tomorrow" in the input, use tomorrow's date
        val tomorrow = currentDateTime.plusDays(1)
        val inputLower = binding.naturalLanguageInput.text.toString().toLowerCase()

        // Create a DateTime object for the event start time
        val startDateTime = if (inputLower.contains("tomorrow")) {
            // If "tomorrow" is in the input, use tomorrow's date with the parsed time
            val hour = parsedEvent.hour
            val minute = parsedEvent.minute
            tomorrow.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0)
        } else {
            // Otherwise use today's date with the parsed time
            val hour = parsedEvent.hour
            val minute = parsedEvent.minute
            currentDateTime.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0)
        }

        // Calculate end time (default to 1 hour later)
        val endDateTime = startDateTime.plusHours(1)

        // Convert to seconds for EventActivity
        val startTS = startDateTime.seconds()
        val endTS = endDateTime.seconds()

        val intent = Intent(this, EventActivity::class.java).apply {
            putExtra(EVENT_ID, 0L)
            putExtra(NEW_EVENT_START_TS, startTS)
            putExtra(NEW_EVENT_END_TS, endTS)
            putExtra(NEW_EVENT_SET_HOUR_DURATION, false)
            putExtra(TITLE, parsedEvent.title)
            putExtra(LOCATION, parsedEvent.location)
            putExtra(DESCRIPTION, parsedEvent.description)

            if (parsedEvent.isRecurring) {
                putExtra(EVENT_REPEAT_INTERVAL, parsedEvent.repeatInterval)
                putExtra(EVENT_REPEAT_RULE, parsedEvent.repeatRule)
            }
        }

        startActivity(intent)
        finish()
    }
}
