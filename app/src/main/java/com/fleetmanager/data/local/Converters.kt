package com.fleetmanager.data.local

import androidx.room.TypeConverter
import com.fleetmanager.data.dto.EarningBreakdownDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromEarningBreakdownList(value: List<EarningBreakdownDto>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEarningBreakdownList(value: String): List<EarningBreakdownDto> {
        val listType = object : TypeToken<List<EarningBreakdownDto>>() {}.type
        return gson.fromJson<List<EarningBreakdownDto>>(value, listType) ?: emptyList()
    }
}
