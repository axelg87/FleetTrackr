package com.fleetmanager.data.local

import androidx.room.TypeConverter
import com.fleetmanager.domain.model.ProviderEarning
import com.fleetmanager.domain.model.ProviderType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class Converters {
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
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromProvidersJson(json: String?): List<ProviderEarning> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val providers = mutableListOf<ProviderEarning>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", "OTHER").uppercase()
                val type = try { ProviderType.valueOf(typeStr) } catch (e: Exception) { ProviderType.OTHER }
                
                providers.add(
                    ProviderEarning(
                        type = type,
                        amount = obj.optDouble("amount", 0.0),
                        currency = obj.optString("currency", "AED"),
                        tripsCount = if (obj.has("tripsCount")) obj.optInt("tripsCount") else null,
                        meta = null // Keep simple
                    )
                )
            }
            providers
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toProvidersJson(providers: List<ProviderEarning>?): String {
        if (providers.isNullOrEmpty()) return "[]"
        
        return try {
            val jsonArray = JSONArray()
            providers.forEach { provider ->
                val obj = JSONObject()
                obj.put("type", provider.type.name)
                obj.put("amount", provider.amount)
                obj.put("currency", provider.currency)
                provider.tripsCount?.let { obj.put("tripsCount", it) }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }
}