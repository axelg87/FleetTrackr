package com.fleetmanager.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.fleetmanager.domain.model.ProviderEarning
import com.fleetmanager.domain.model.ProviderType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class Converters {
    companion object {
        private const val TAG = "RoomConverters"
    }
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
                        amount = obj.optDouble("amount", 0.0).takeIf { it > 0 }
                            ?: (obj.optDouble("cardAmount", obj.optDouble("card", 0.0))
                            + obj.optDouble("cashAmount", obj.optDouble("cash", 0.0))
                            + obj.optDouble("tipsAmount", obj.optDouble("tips", 0.0))),
                        cardAmount = obj.optDouble("cardAmount", obj.optDouble("card", 0.0)),
                        cashAmount = obj.optDouble("cashAmount", obj.optDouble("cash", 0.0)),
                        tipsAmount = obj.optDouble("tipsAmount", obj.optDouble("tips", 0.0)),
                        hoursOnline = obj.optDouble("hoursOnline", Double.NaN).takeUnless { it.isNaN() },
                        currency = obj.optString("currency", "AED"),
                        tripsCount = if (obj.has("tripsCount")) obj.optInt("tripsCount") else null,
                        meta = null // Keep simple
                    )
                )
            }
            Log.d(TAG, "fromProvidersJson: Successfully parsed ${providers.size} providers")
            providers
        } catch (e: Exception) {
            Log.e(TAG, "fromProvidersJson: Failed to parse JSON. Input: '$json'", e)
            Log.e(TAG, "fromProvidersJson: Error was: ${e.javaClass.simpleName} - ${e.message}")
            emptyList()
        }
    }

    @TypeConverter
    fun toProvidersJson(providers: List<ProviderEarning>?): String {
        if (providers.isNullOrEmpty()) return "[]"
        
        return try {
            val jsonArray = JSONArray()
            var successCount = 0
            providers.forEach { provider ->
                try {
                    val obj = JSONObject()
                    obj.put("type", provider.type.name)
                    obj.put("amount", provider.totalAmount)
                    obj.put("currency", provider.currency)
                    obj.put("cardAmount", provider.cardAmount)
                    obj.put("card", provider.cardAmount)
                    obj.put("cashAmount", provider.cashAmount)
                    obj.put("cash", provider.cashAmount)
                    obj.put("tipsAmount", provider.tipsAmount)
                    obj.put("tips", provider.tipsAmount)
                    provider.hoursOnline?.let { obj.put("hoursOnline", it) }
                    provider.tripsCount?.let { obj.put("tripsCount", it) }
                    jsonArray.put(obj)
                    successCount++
                } catch (e: Exception) {
                    Log.w(TAG, "toProvidersJson: Failed to serialize provider ${provider.type}: ${e.message}")
                }
            }
            val result = jsonArray.toString()
            Log.d(TAG, "toProvidersJson: Serialized $successCount/${providers.size} providers")
            result
        } catch (e: Exception) {
            Log.e(TAG, "toProvidersJson: Failed to serialize ${providers.size} providers", e)
            Log.e(TAG, "toProvidersJson: Error was: ${e.javaClass.simpleName} - ${e.message}")
            "[]"
        }
    }
}