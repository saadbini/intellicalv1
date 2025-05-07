package com.saza.intellical.helpers

import android.content.Context
import com.saza.intellical.BuildConfig
import com.saza.commons.extensions.showErrorToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern

/**
 * Natural Language Parser for event creation
 *
 * This class handles the parsing of natural language event descriptions into structured event data.
 * It uses Azure AI Text Analytics for Named Entity Recognition (NER) via REST API,
 * with a fallback to basic rule-based parsing when Azure is unavailable or not configured.
 *
 * The parser attempts to identify the following information:
 * - Event title (determined by subtracting recognized patterns from input)
 * - Date and time information
 * - Location information
 * - Recurrence information
 *
 * Azure AI Text Analytics Integration:
 * -----------------------------------
 * 1. Create an Azure account and Language resource (see azure_language_setup.md in assets)
 * 2. Set up a Text Analytics resource with Named Entity Recognition capabilities
 * 3. Configure the app with your API key and endpoint in local.properties
 *
 * The parser will extract these details from natural language inputs and
 * structure them into a ParsedEvent object that can be used to create calendar events.
 *
 * @param context Application context for accessing configuration
 * @param contextTimestamp Timestamp to use as reference for relative date expressions (e.g., "tomorrow")
 */
class NaturalLanguageParser(private val context: Context, private val contextTimestamp: Long = System.currentTimeMillis()) {
    companion object {
        private const val DEFAULT_EVENT_DURATION_MINUTES = 60
        private const val DEFAULT_MEETING_DURATION_MINUTES = 30

        // Entity categories used in parsing
        const val DATETIME = "DateTime"
        const val LOCATION = "Location"
        const val PERSON = "Person"
        const val QUANTITY = "Quantity"
        const val EVENT = "Event"
    }

    // Reference date/time context for relative time expressions (e.g., "tomorrow", "next week")
    private val referenceDateTime = DateTime(contextTimestamp)

    // Azure REST API credentials
    private val apiKey = "85xR39J5Uwt1aWkGYmKAvvZpAaq08hO5d1xNHV58lNwMK0OE3SBjJQQJ99BDACYeBjFXJ3w3AAAaACOGlUE1"
    private val endpoint = "https://intellical-lang-service.cognitiveservices.azure.com"

    /**
     * Represents a parsed event with all necessary fields to create a calendar event
     *
     * @property title Event title extracted from natural language description
     * @property startTime Start timestamp in seconds since epoch
     * @property endTime End timestamp in seconds since epoch
     * @property hour Hour of day (0-23) for the event
     * @property minute Minute (0-59) for the event
     * @property location Location of the event (if mentioned)
     * @property description Additional description details (if any)
     * @property isRecurring Whether the event repeats
     * @property repeatInterval Interval for repeating events (daily, weekly, monthly, yearly)
     * @property repeatRule Specific rule pattern for repeating events
     */
    data class ParsedEvent(
        val title: String,
        val startTime: Long,
        val endTime: Long,
        val hour: Int = 14, // Default to 2 PM
        val minute: Int = 0,
        val location: String = "",
        val description: String = "",
        val isRecurring: Boolean = false,
        val repeatInterval: Int = 0,
        val repeatRule: Int = 0
    )

    /**
     * Main method to parse natural language event text
     *
     * Uses Azure Text Analytics REST API with a fallback to basic parsing.
     *
     * @param text The natural language description of the event
     * @param callback Callback with parsed event data or error message
     */
    fun parseEventText(text: String, callback: (ParsedEvent?, String?) -> Unit) {
        if (text.isEmpty()) {
            callback(null, "Please enter event details")
            return
        }

        // Check if Azure credentials are properly configured
        if (apiKey.isEmpty() || endpoint.isEmpty()) {
            // Fall back to basic parsing with a notification about Azure not being configured
            val basicParsedEvent = parseBasic(text)
            callback(basicParsedEvent, "Basic parsing is active (No Azure)")
            return
        }

        // Use Azure NER for parsing via REST API, with fallback to basic parsing
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val parsedEvent = withContext(Dispatchers.IO) {
                    parseUsingAzureRestApi(text)
                }
                callback(parsedEvent, null)
            } catch (e: Exception) {
                // If Azure processing fails, fall back to basic parsing
                val basicParsedEvent = parseBasic(text)
                callback(basicParsedEvent, "Azure processing failed, using basic parsing: ${e.message}")
            }
        }
    }

    /**
     * Data class representing Azure recognized entity
     */
    private data class AzureEntity(
        val text: String,
        val category: String,
        val offset: Int,
        val length: Int,
        val confidenceScore: Double = 0.0,
        val subcategory: String? = null
    )

    /**
     * Parse event text using Azure Text Analytics NER via REST API
     */
    private fun parseUsingAzureRestApi(text: String): ParsedEvent {
        // Prepare the request to Azure Language Service API
        val url = URL("$endpoint/language/:analyze-text?api-version=2023-04-01")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
        connection.doOutput = true

        // Prepare the request body according to the updated API format
        val requestBody = JSONObject().apply {
            put("kind", "EntityRecognition")
            put("analysisInput", JSONObject().apply {
                put("documents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "documentId")
                        put("text", text)
                        put("language", "en")
                    })
                })
            })
        }

        // Send the request
        val outputStream = connection.outputStream
        val writer = OutputStreamWriter(outputStream, "UTF-8")
        writer.write(requestBody.toString())
        writer.flush()
        writer.close()

        // Read the response
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("Azure API request failed with code $responseCode: ${connection.responseMessage}")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonResponse = JSONObject(response)

        // Parse the entities from the response (updated for new API format)
        val entities = mutableListOf<AzureEntity>()
        val results = jsonResponse.optJSONObject("results")

        if (results != null) {
            val documents = results.optJSONArray("documents")
            if (documents != null && documents.length() > 0) {
                val document = documents.getJSONObject(0)
                val entitiesArray = document.optJSONArray("entities")

                if (entitiesArray != null) {
                    for (i in 0 until entitiesArray.length()) {
                        val entity = entitiesArray.getJSONObject(i)
                        entities.add(
                            AzureEntity(
                                text = entity.getString("text"),
                                category = entity.getString("category"),
                                offset = entity.getInt("offset"),
                                length = entity.getInt("length"),
                                confidenceScore = entity.optDouble("confidenceScore", 0.0),
                                subcategory = entity.optString("subcategory")
                            )
                        )
                    }
                }
            }
        }

        // Process the entities to extract event details
        // Categorize entities by type
        val dateTimeEntities = entities.filter { it.category == DATETIME }
        val locationEntities = entities.filter { it.category == LOCATION }
        val personEntities = entities.filter { it.category == PERSON }
        val quantityEntities = entities.filter { it.category == QUANTITY }
        val eventEntities = entities.filter { it.category == EVENT }

        // Extract only one entity of each type (as requested)
        val eventEntity = eventEntities.maxByOrNull { it.confidenceScore }
        val locationEntity = locationEntities.maxByOrNull { it.confidenceScore }
        val personEntity = personEntities.maxByOrNull { it.confidenceScore }

        // Process datetime entities - we need to combine date and time if they exist separately
        val timeInfo = if (dateTimeEntities.isNotEmpty()) {
            processDateTimeEntities(dateTimeEntities, text, quantityEntities)
        } else {
            // If no datetime entity found, use current date and default duration
            val startTime = referenceDateTime.withTimeAtStartOfDay().plusHours(12).millis // Default to noon today
            EventTimeInfo(
                startTime = startTime,
                endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000),
                recurrence = null
            )
        }

        // Extract location (use only the highest confidence one)
        val location = locationEntity?.text ?: ""

        // Extract title - prefer the recognized event entity if available
        val title = if (eventEntity != null) {
            // Use event entity as the basis for the title
            val personText = personEntity?.text?.let { " with $it" } ?: ""
            val locationText = locationEntity?.text?.let { " at $it" } ?: ""
            "${eventEntity.text}$personText$locationText"
        } else {
            extractTitle(text, entities)
        }

        // Build description with person entity
        val description = if (personEntity != null && !title.contains(personEntity.text, ignoreCase = true)) {
            "Meeting with ${personEntity.text}"
        } else {
            buildDescription(text, personEntities)
        }

        // Extract hour and minute from the startTime
        val startDateTime = DateTime(timeInfo.startTime)
        val hour = startDateTime.hourOfDay
        val minute = startDateTime.minuteOfHour

        // Create the parsed event with timestamps in seconds and explicit hour/minute
        return ParsedEvent(
            title = title,
            startTime = timeInfo.startTime / 1000,
            endTime = timeInfo.endTime / 1000,
            hour = hour,
            minute = minute,
            location = location,
            description = description,
            isRecurring = timeInfo.recurrence?.isRecurring ?: false,
            repeatInterval = timeInfo.recurrence?.interval ?: 0,
            repeatRule = timeInfo.recurrence?.rule ?: 0
        )
    }

    /**
     * Data class to hold event time and recurrence information
     */
    private data class EventTimeInfo(
        val startTime: Long,
        val endTime: Long,
        val recurrence: RecurrenceInfo?
    )

    /**
     * Data class to hold recurrence information
     */
    private data class RecurrenceInfo(
        val isRecurring: Boolean = false,
        val interval: Int = 0,
        val rule: Int = 0
    )

    /**
     * Process DateTime entities to extract start and end times, and recurrence information
     */
    private fun processDateTimeEntities(
        dateTimeEntities: List<AzureEntity>,
        originalText: String,
        quantityEntities: List<AzureEntity>
    ): EventTimeInfo {
        // Default to current time if no date/time entities are found
        var startTime = referenceDateTime.millis
        var endTime = referenceDateTime.plusMinutes(DEFAULT_EVENT_DURATION_MINUTES).millis

        // Check for relative dates like "tomorrow" or "today" directly in the text
        val tomorrowPattern = Pattern.compile("\\btomorrow\\b", Pattern.CASE_INSENSITIVE)
        val tomorrowMatcher = tomorrowPattern.matcher(originalText)
        val todayPattern = Pattern.compile("\\btoday\\b", Pattern.CASE_INSENSITIVE)
        val todayMatcher = todayPattern.matcher(originalText)

        // Check for time pattern
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE)
        val timeMatcher = timePattern.matcher(originalText)

        if ((tomorrowMatcher.find() || todayMatcher.find()) && timeMatcher.find()) {
            // We have both a relative date and a specific time
            var hour = timeMatcher.group(1).toInt()
            val minute = timeMatcher.group(2)?.toInt() ?: 0
            val ampm = timeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

            // Adjust hour for AM/PM
            if (ampm != null) {
                if (ampm.contains("pm") && hour < 12) {
                    hour += 12
                } else if (ampm.contains("am") && hour == 12) {
                    hour = 0
                }
            } else if (hour < 12) {
                // If no AM/PM specified and hour is less than 12, assume PM for business hours
                if (hour >= 1 && hour <= 6) {
                    hour += 12
                }
            }

            // Set the base date (today or tomorrow)
            val baseDate = if (originalText.toLowerCase(Locale.getDefault()).contains("tomorrow")) {
                referenceDateTime.plusDays(1).withTimeAtStartOfDay()
            } else {
                referenceDateTime.withTimeAtStartOfDay()
            }

            // Combine date and time
            startTime = baseDate.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
            endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)

            return EventTimeInfo(startTime, endTime, detectRecurrence(originalText, quantityEntities))
        }

        if (dateTimeEntities.isEmpty()) {
            // Try to extract date/time directly from the original text
            try {
                // Look for specific date formats in the original text
                val datePattern = Pattern.compile(
                    "(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s*,?\\s*(\\d{4}))?|" +
                    "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4}))?",
                    Pattern.CASE_INSENSITIVE
                )
                val dateMatcher = datePattern.matcher(originalText)

                if (dateMatcher.find()) {
                    // Extract date components
                    val day: Int
                    val month: Int
                    val year: Int

                    if (dateMatcher.group(1) != null) {
                        // Format: "1st May 2025"
                        day = dateMatcher.group(1).toInt()
                        month = parseMonth(dateMatcher.group(2))
                        year = dateMatcher.group(3)?.toInt() ?: referenceDateTime.year
                    } else {
                        // Format: "May 1, 2025"
                        month = parseMonth(dateMatcher.group(4))
                        day = dateMatcher.group(5).toInt()
                        year = dateMatcher.group(6)?.toInt() ?: referenceDateTime.year
                    }

                    // Create DateTime with the parsed date
                    val dateComponent = DateTime(year, month, day, 0, 0)

                    // Look for time in the text
                    val specificTimeMatcher = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE).matcher(originalText)

                    if (specificTimeMatcher.find()) {
                        var hour = specificTimeMatcher.group(1).toInt()
                        val minute = specificTimeMatcher.group(2)?.toInt() ?: 0
                        val ampm = specificTimeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

                        // Adjust for AM/PM
                        if (!ampm.isNullOrEmpty()) {
                            if (ampm.contains("pm") && hour < 12) {
                                hour += 12
                            } else if (ampm.contains("am") && hour == 12) {
                                hour = 0
                            }
                        } else if (hour < 12) {
                            // If no AM/PM specified and hour is less than 12, assume PM for business hours
                            if (hour >= 1 && hour <= 6) {
                                hour += 12
                            }
                        }

                        startTime = dateComponent.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
                    } else {
                        // No time found, use noon as default
                        startTime = dateComponent.withHourOfDay(12).withMinuteOfHour(0).withSecondOfMinute(0).millis
                    }

                    endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)
                    return EventTimeInfo(startTime, endTime, detectRecurrence(originalText, quantityEntities))
                }
            } catch (e: Exception) {
                // If direct parsing fails, continue with default handling
            }

            return EventTimeInfo(startTime, endTime, null)
        }

        // Check for recurrence indicators in the text
        val recurrenceInfo = detectRecurrence(originalText, quantityEntities)

        // Separate date and time entities based on subcategory
        val dateEntities = dateTimeEntities.filter {
            it.subcategory?.contains("Date", ignoreCase = true) == true ||
            (it.subcategory.isNullOrEmpty() && !it.text.matches(Regex("\\d{1,2}(?::\\d{2})?(?:\\s*[ap]m)?", RegexOption.IGNORE_CASE)))
        }
        val timeEntities = dateTimeEntities.filter {
            it.subcategory?.contains("Time", ignoreCase = true) == true ||
            (it.subcategory.isNullOrEmpty() && it.text.matches(Regex("\\d{1,2}(?::\\d{2})?(?:\\s*[ap]m)?", RegexOption.IGNORE_CASE)))
        }

        // Handle date component - use the highest confidence date entity if available
        var dateComponent = referenceDateTime.withTimeAtStartOfDay() // Default to today
        if (dateEntities.isNotEmpty()) {
            val bestDateEntity = dateEntities.maxByOrNull { it.confidenceScore }
            if (bestDateEntity != null) {
                val dateTime = parseTimeEntity(bestDateEntity.text, referenceDateTime)
                dateComponent = DateTime(dateTime).withTimeAtStartOfDay()
            }
        }

        // Handle time component - use the highest confidence time entity if available
        if (timeEntities.isNotEmpty()) {
            val bestTimeEntity = timeEntities.maxByOrNull { it.confidenceScore }
            if (bestTimeEntity != null) {
                val timeText = bestTimeEntity.text
                val timeMatcher = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE).matcher(timeText)

                if (timeMatcher.find()) {
                    var hour = timeMatcher.group(1).toInt()
                    val minute = timeMatcher.group(2)?.toInt() ?: 0
                    val ampm = timeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

                    // Adjust for AM/PM
                    if (!ampm.isNullOrEmpty()) {
                        if (ampm.contains("pm") && hour < 12) {
                            hour += 12
                        } else if (ampm.contains("am") && hour == 12) {
                            hour = 0
                        }
                    } else if (hour < 12) {
                        // If no AM/PM specified and hour is less than 12, assume PM for business hours
                        if (hour >= 1 && hour <= 6) {
                            hour += 12
                        }
                    }

                    // Combine date and time components
                    startTime = dateComponent.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
                }
            }
        } else {
            // No specific time found, use noon as default time
            startTime = dateComponent.withHourOfDay(12).withMinuteOfHour(0).withSecondOfMinute(0).millis
        }

        // Set end time based on default duration
        endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)

        return EventTimeInfo(startTime, endTime, recurrenceInfo)
    }

    /**
     * Extract a title from text by removing recognized entities
     */
    private fun extractTitle(text: String, entities: List<AzureEntity>): String {
        // If no entities were recognized, use the whole text as title
        if (entities.isEmpty()) {
            return text.trim()
        }

        // Sort entities by position in text
        val sortedEntities = entities.sortedBy { it.offset }

        // Create a set of character ranges to exclude from title
        val excludeRanges = sortedEntities.map { it.offset until (it.offset + it.length) }

        // Build the title by including only text that's not part of an entity
        val titleBuilder = StringBuilder()
        var currentPosition = 0

        for (char in text.withIndex()) {
            val position = char.index
            // Check if this position is part of any entity
            val isInEntity = excludeRanges.any { position in it }
            if (!isInEntity) {
                titleBuilder.append(char.value)
            } else if (position > currentPosition && titleBuilder.isNotEmpty() && titleBuilder.last() != ' ') {
                // Add a space where we removed an entity if needed
                titleBuilder.append(' ')
            }
            currentPosition = position + 1
        }

        // Clean up the title - remove extra spaces and trim
        var title = titleBuilder.toString().replace(Regex("\\s+"), " ").trim()

        // If title is empty or too short, use a default or the first part of the text
        if (title.length < 3) {
            // Try to extract a meaningful title from the first line or sentence
            title = text.split("\n").firstOrNull()?.trim() ?: text
            if (title.length > 30) {
                title = title.substring(0, 30) + "..."
            }
        }

        return title
    }

    /**
     * Detect recurrence information from text and quantity entities
     */
    private fun detectRecurrence(text: String, quantityEntities: List<AzureEntity>): RecurrenceInfo {
        val lowerText = text.toLowerCase(Locale.getDefault())

        // Check for recurring event indicators
        val isRecurring = lowerText.contains("every") ||
                          lowerText.contains("weekly") ||
                          lowerText.contains("monthly") ||
                          lowerText.contains("daily") ||
                          lowerText.contains("yearly")

        if (!isRecurring) {
            return RecurrenceInfo()
        }

        // Determine repeat interval based on text
        val repeatInterval = when {
            lowerText.contains("daily") || lowerText.contains("every day") -> 1 // Daily
            lowerText.contains("weekly") || lowerText.contains("every week") -> 2 // Weekly
            lowerText.contains("monthly") || lowerText.contains("every month") -> 3 // Monthly
            lowerText.contains("yearly") || lowerText.contains("every year") || lowerText.contains("annually") -> 4 // Yearly
            else -> 0
        }

        // Determine repeat rule for monthly/yearly recurrence
        val repeatRule = 0 // Default, can be enhanced for more complex rules

        return RecurrenceInfo(isRecurring, repeatInterval, repeatRule)
    }

    /**
     * Build additional description from person entities and other details
     */
    private fun buildDescription(originalText: String, personEntities: List<AzureEntity>): String {
        val description = StringBuilder()

        // Add attendees information if available
        if (personEntities.isNotEmpty()) {
            description.append("Attendees: ")
            description.append(personEntities.joinToString(", ") { it.text })
            description.append("\n\n")
        }

        // Add original text as reference
        description.append("Original description: ")
        description.append(originalText)

        return description.toString()
    }

    /**
     * Parse a time entity into a timestamp
     */
    private fun parseTimeEntity(entityText: String, reference: DateTime): Long {
        val lowerText = entityText.toLowerCase(Locale.getDefault())

        // Try to parse specific date formats first (e.g., "1st May 2025")
        try {
            // Match patterns like "1st May 2025", "May 1, 2025", "1 May 2025", etc.
            val datePattern = Pattern.compile(
                "(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s*,?\\s*(\\d{4}))?|" +
                "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4}))?",
                Pattern.CASE_INSENSITIVE
            )
            val dateMatcher = datePattern.matcher(lowerText)

            if (dateMatcher.find()) {
                // Extract date components
                val day: Int
                val month: Int
                val year: Int

                if (dateMatcher.group(1) != null) {
                    // Format: "1st May 2025"
                    day = dateMatcher.group(1).toInt()
                    month = parseMonth(dateMatcher.group(2))
                    year = dateMatcher.group(3)?.toInt() ?: reference.year
                } else {
                    // Format: "May 1, 2025"
                    month = parseMonth(dateMatcher.group(4))
                    day = dateMatcher.group(5).toInt()
                    year = dateMatcher.group(6)?.toInt() ?: reference.year
                }

                // Create DateTime with the parsed date
                return DateTime(year, month, day, 0, 0).millis
            }
        } catch (e: Exception) {
            // If date parsing fails, continue with other methods
        }

        // Check for time pattern in the text
        val entityTimePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE)
        val entityTimeMatcher = entityTimePattern.matcher(lowerText)

        // Handle common relative time expressions
        when {
            lowerText.contains("today") -> {
                val baseDate = reference.withTimeAtStartOfDay()
                if (entityTimeMatcher.find()) {
                    var hour = entityTimeMatcher.group(1).toInt()
                    val minute = entityTimeMatcher.group(2)?.toInt() ?: 0
                    val ampm = entityTimeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

                    // Adjust for AM/PM
                    if (!ampm.isNullOrEmpty()) {
                        if (ampm.contains("pm") && hour < 12) {
                            hour += 12
                        } else if (ampm.contains("am") && hour == 12) {
                            hour = 0
                        }
                    } else if (hour < 12) {
                        // If no AM/PM specified and hour is less than 12, assume PM for business hours
                        if (hour >= 1 && hour <= 6) {
                            hour += 12
                        }
                    }

                    return baseDate.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
                }
                return baseDate.millis
            }
            lowerText.contains("tomorrow") -> {
                val baseDate = reference.plusDays(1).withTimeAtStartOfDay()
                if (entityTimeMatcher.find()) {
                    var hour = entityTimeMatcher.group(1).toInt()
                    val minute = entityTimeMatcher.group(2)?.toInt() ?: 0
                    val ampm = entityTimeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

                    // Adjust for AM/PM
                    if (!ampm.isNullOrEmpty()) {
                        if (ampm.contains("pm") && hour < 12) {
                            hour += 12
                        } else if (ampm.contains("am") && hour == 12) {
                            hour = 0
                        }
                    } else if (hour < 12) {
                        // If no AM/PM specified and hour is less than 12, assume PM for business hours
                        if (hour >= 1 && hour <= 6) {
                            hour += 12
                        }
                    }

                    return baseDate.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
                }
                return baseDate.millis
            }
            lowerText.contains("next week") -> {
                return reference.plusWeeks(1).withDayOfWeek(1).withTimeAtStartOfDay().millis
            }
            lowerText.contains("next month") -> {
                return reference.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay().millis
            }
            lowerText.contains("monday") || lowerText.contains("mon") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 1)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("tuesday") || lowerText.contains("tue") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 2)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("wednesday") || lowerText.contains("wed") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 3)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("thursday") || lowerText.contains("thu") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 4)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("friday") || lowerText.contains("fri") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 5)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("saturday") || lowerText.contains("sat") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 6)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
            lowerText.contains("sunday") || lowerText.contains("sun") -> {
                val daysToAdd = calculateDaysToNextDayOfWeek(reference, 7)
                return reference.plusDays(daysToAdd).withTimeAtStartOfDay().millis
            }
        }

        // Try to parse time expressions using regex patterns

        // Check for specific time patterns (e.g., "3pm", "15:30")
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE)
        val timeMatcher = timePattern.matcher(lowerText)

        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1).toInt()
            val minute = timeMatcher.group(2)?.toInt() ?: 0
            val ampm = timeMatcher.group(3)?.toLowerCase(Locale.getDefault())

            // Adjust hour for AM/PM
            if (ampm != null) {
                if (ampm.contains("pm") && hour < 12) {
                    hour += 12
                } else if (ampm.contains("am") && hour == 12) {
                    hour = 0
                }
            } else if (hour < 12) {
                // If no AM/PM specified and hour is less than 12, assume PM for business hours
                if (hour >= 1 && hour <= 6) {
                    hour += 12
                }
            }

            return reference.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
        }

        // Check for date patterns (e.g., "May 15", "05/15/2023")
        val datePattern = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?")
        val dateMatcher = datePattern.matcher(lowerText)

        if (dateMatcher.find()) {
            val month = dateMatcher.group(1).toInt()
            val day = dateMatcher.group(2).toInt()
            var year = dateMatcher.group(3)?.toInt() ?: reference.year

            // Adjust two-digit year
            if (year < 100) {
                year += 2000
            }

            return DateTime(year, month, day, 0, 0).millis
        }

        // If all parsing attempts fail, use the reference time
        return reference.millis
    }

    /**
     * Calculate days to add to reach the next occurrence of a specific day of week
     */
    private fun calculateDaysToNextDayOfWeek(reference: DateTime, targetDayOfWeek: Int): Int {
        val currentDayOfWeek = reference.dayOfWeek
        return if (targetDayOfWeek > currentDayOfWeek) {
            targetDayOfWeek - currentDayOfWeek
        } else {
            7 - (currentDayOfWeek - targetDayOfWeek)
        }
    }

    /**
     * Basic rule-based parser as fallback
     */
    private fun parseBasic(text: String): ParsedEvent {
        var title = text
        var location = ""
        var startTime = referenceDateTime.millis
        var endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)
        var isRecurring = false
        var repeatInterval = 0

        // Extract location if specified with "at" or "in"
        val locationPattern = Pattern.compile("(?:at|in)\\s+([\\w\\s]+(?:St(?:\\.|reet)?|Ave(?:\\.|nue)?|Rd|Road|Dr(?:\\.|ive)?|Place|Plaza|Square|Mall|Center|Building|Room|Hall|Park))", Pattern.CASE_INSENSITIVE)
        val locationMatcher = locationPattern.matcher(text)
        if (locationMatcher.find()) {
            location = locationMatcher.group(1)
            title = title.replace(locationMatcher.group(0), " ").trim()
        }

        // Check for relative dates like "tomorrow" or "today" directly in the text
        val tomorrowPattern = Pattern.compile("\\btomorrow\\b", Pattern.CASE_INSENSITIVE)
        val tomorrowMatcher = tomorrowPattern.matcher(text)
        val todayPattern = Pattern.compile("\\btoday\\b", Pattern.CASE_INSENSITIVE)
        val todayMatcher = todayPattern.matcher(text)

        // Check for time pattern
        val basicTimePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE)
        val basicTimeMatcher = basicTimePattern.matcher(text)

        if ((tomorrowMatcher.find() || todayMatcher.find()) && basicTimeMatcher.find()) {
            // We have both a relative date and a specific time
            var hour = basicTimeMatcher.group(1).toInt()
            val minute = basicTimeMatcher.group(2)?.toInt() ?: 0
            val ampm = basicTimeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

            // Adjust hour for AM/PM
            if (ampm != null) {
                if (ampm.contains("pm") && hour < 12) {
                    hour += 12
                } else if (ampm.contains("am") && hour == 12) {
                    hour = 0
                }
            } else if (hour < 12) {
                // If no AM/PM specified and hour is less than 12, assume PM for business hours
                if (hour >= 1 && hour <= 6) {
                    hour += 12
                }
            }

            // Set the base date (today or tomorrow)
            val baseDate = if (text.toLowerCase(Locale.getDefault()).contains("tomorrow")) {
                referenceDateTime.plusDays(1).withTimeAtStartOfDay()
            } else {
                referenceDateTime.withTimeAtStartOfDay()
            }

            // Combine date and time
            startTime = baseDate.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
            endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)

            // Remove the processed parts from the title
            title = title.replaceFirst(if (text.toLowerCase(Locale.getDefault()).contains("tomorrow")) "tomorrow" else "today", " ")
            .replaceFirst(basicTimeMatcher.group(0), " ")
                .trim()
        } else {
            // Extract date and time information using the regular pattern
            val dateTimePattern = Pattern.compile("(?:on|at)\\s+(.+?)(?:\\s+at|$)", Pattern.CASE_INSENSITIVE)
            val dateTimeMatcher = dateTimePattern.matcher(text)
            if (dateTimeMatcher.find()) {
                val dateTimeText = dateTimeMatcher.group(1)

                // Process date/time text to set startTime
                startTime = parseBasicDateTime(dateTimeText, referenceDateTime)
                endTime = startTime + (DEFAULT_EVENT_DURATION_MINUTES * 60 * 1000)

                title = title.replace(dateTimeMatcher.group(0), " ").trim()
            }
        }

        // Check for recurring event indicators
        val recurringPattern = Pattern.compile("(?:every|each)\\s+(day|week|month|year|monday|tuesday|wednesday|thursday|friday|saturday|sunday)", Pattern.CASE_INSENSITIVE)
        val recurringMatcher = recurringPattern.matcher(text)
        if (recurringMatcher.find()) {
            isRecurring = true

            // Set repeat interval based on recurrence text
            val recurrenceText = recurringMatcher.group(1).toLowerCase(Locale.getDefault())
            repeatInterval = when {
                recurrenceText == "day" -> 1 // Daily
                recurrenceText == "week" ||
                recurrenceText in listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday") -> 2 // Weekly
                recurrenceText == "month" -> 3 // Monthly
                recurrenceText == "year" -> 4 // Yearly
                else -> 0
            }

            title = title.replace(recurringMatcher.group(0), " ").trim()
        }

        // Clean up the title
        title = title.trim()
        if (title.isEmpty()) {
            title = "New Event"
        }

        // Extract hour and minute from the startTime
        val startDateTime = DateTime(startTime)
        val hour = startDateTime.hourOfDay
        val minute = startDateTime.minuteOfHour

        // Convert milliseconds to seconds for compatibility with EventActivity
        return ParsedEvent(
            title = title,
            startTime = startTime / 1000,
            endTime = endTime / 1000,
            hour = hour,
            minute = minute,
            location = location,
            isRecurring = isRecurring,
            repeatInterval = repeatInterval
        )
    }

    /**
     * Parse date/time text in the basic parser
     */
    private fun parseBasicDateTime(dateTimeText: String, reference: DateTime): Long {
        val lowerText = dateTimeText.toLowerCase(Locale.getDefault())

        // Try to parse specific date formats first (e.g., "1st May 2025")
        try {
            // Match patterns like "1st May 2025", "May 1, 2025", "1 May 2025", etc.
            val datePattern = Pattern.compile(
                "(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s*,?\\s*(\\d{4}))?|" +
                "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4}))?",
                Pattern.CASE_INSENSITIVE
            )
            val dateMatcher = datePattern.matcher(lowerText)

            if (dateMatcher.find()) {
                // Extract date components
                val day: Int
                val month: Int
                val year: Int

                if (dateMatcher.group(1) != null) {
                    // Format: "1st May 2025"
                    day = dateMatcher.group(1).toInt()
                    month = parseMonth(dateMatcher.group(2))
                    year = dateMatcher.group(3)?.toInt() ?: reference.year
                } else {
                    // Format: "May 1, 2025"
                    month = parseMonth(dateMatcher.group(4))
                    day = dateMatcher.group(5).toInt()
                    year = dateMatcher.group(6)?.toInt() ?: reference.year
                }

                // Create DateTime with the parsed date
                val dateComponent = DateTime(year, month, day, 0, 0)
                return extractSpecificTimeIfPresent(lowerText, dateComponent)
            }
        } catch (e: Exception) {
            // If specific date parsing fails, continue with other methods
        }

        // Handle common time expressions
        when {
            lowerText.contains("today") -> {
                // For "today", use noon as default time unless a specific time is mentioned
                val baseTime = reference.withTimeAtStartOfDay().plusHours(12)
                return extractSpecificTimeIfPresent(lowerText, baseTime)
            }
            lowerText.contains("tomorrow") -> {
                // For "tomorrow", use noon as default time unless a specific time is mentioned
                val baseTime = reference.plusDays(1).withTimeAtStartOfDay().plusHours(12)
                return extractSpecificTimeIfPresent(lowerText, baseTime)
            }
            lowerText.contains("next week") -> {
                // For "next week", use Monday noon as default time unless a specific time is mentioned
                val baseTime = reference.plusWeeks(1).withDayOfWeek(1).withTimeAtStartOfDay().plusHours(12)
                return extractSpecificTimeIfPresent(lowerText, baseTime)
            }
            // Add more cases as needed
        }

        // Check for date patterns (e.g., "05/15/2023")
        val numericDatePattern = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?")
        val numericDateMatcher = numericDatePattern.matcher(lowerText)

        if (numericDateMatcher.find()) {
            val month = numericDateMatcher.group(1).toInt()
            val day = numericDateMatcher.group(2).toInt()
            var year = numericDateMatcher.group(3)?.toInt() ?: reference.year

            // Adjust two-digit year
            if (year < 100) {
                year += 2000
            }

            val dateComponent = DateTime(year, month, day, 0, 0)
            return extractSpecificTimeIfPresent(lowerText, dateComponent)
        }

        // Try to extract time if specified (e.g., "3pm", "15:30")
        return extractSpecificTimeIfPresent(lowerText, reference)
    }

    /**
     * Extract a specific time from text if present, otherwise use the base time
     */
    private fun extractSpecificTimeIfPresent(text: String, baseTime: DateTime): Long {
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(\\s*[ap]m)?", Pattern.CASE_INSENSITIVE)
        val timeMatcher = timePattern.matcher(text)

        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1).toInt()
            val minute = timeMatcher.group(2)?.toInt() ?: 0
            val ampm = timeMatcher.group(3)?.trim()?.toLowerCase(Locale.getDefault())

            // Adjust hour for AM/PM
            if (ampm != null) {
                if (ampm.contains("pm") && hour < 12) {
                    hour += 12
                } else if (ampm.contains("am") && hour == 12) {
                    hour = 0
                }
            } else if (hour < 12) {
                // If no AM/PM specified and hour is less than 12, assume PM for business hours
                if (hour >= 1 && hour <= 6) {
                    hour += 12
                }
            }

            return baseTime.withHourOfDay(hour).withMinuteOfHour(minute).withSecondOfMinute(0).millis
        }

        // Default to base time if no specific time is found
        return baseTime.millis
    }

    /**
     * Parse month name to month number (1-12)
     */
    private fun parseMonth(monthText: String): Int {
        return when (monthText.toLowerCase(Locale.getDefault())) {
            "jan", "january" -> 1
            "feb", "february" -> 2
            "mar", "march" -> 3
            "apr", "april" -> 4
            "may" -> 5
            "jun", "june" -> 6
            "jul", "july" -> 7
            "aug", "august" -> 8
            "sep", "september" -> 9
            "oct", "october" -> 10
            "nov", "november" -> 11
            "dec", "december" -> 12
            else -> throw IllegalArgumentException("Invalid month name: $monthText")
        }
    }
}
