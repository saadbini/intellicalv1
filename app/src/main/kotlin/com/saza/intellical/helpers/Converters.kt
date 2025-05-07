package com.saza.intellical.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import com.saza.intellical.models.Attendee

class Converters {
    private val gson = Gson()

    // Define type tokens for Kotlin
    private inline fun <reified T> typeToken(): Type = object : TypeToken<T>() {}.type

    private val stringType: Type = typeToken<List<String>>()
    private val attendeeType: Type = typeToken<List<Attendee>>()

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val newValue = if (value.isNotEmpty() && !value.startsWith("[")) {
            "[$value]"
        } else {
            value
        }

        return try {
            gson.fromJson(newValue, stringType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun stringListToJson(list: List<String>) = gson.toJson(list)

    @TypeConverter
    fun attendeeListToJson(list: List<Attendee>): String = gson.toJson(list)

    @TypeConverter
    fun jsonToAttendeeList(value: String): List<Attendee> {
        if (value.isEmpty()) {
            return emptyList()
        }

        return try {
            gson.fromJson<ArrayList<Attendee>>(value, attendeeType) ?: ArrayList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
