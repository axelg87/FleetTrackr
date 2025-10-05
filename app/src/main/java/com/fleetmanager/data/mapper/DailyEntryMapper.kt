package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.ProviderEarning
import com.fleetmanager.domain.model.ProviderType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mapper to convert between DailyEntry domain model and DailyEntryDto.
 * Handles conversion of providers list to/from JSON.
 */
object DailyEntryMapper {
    
    fun toDomain(dto: DailyEntryDto): DailyEntry {
        val providers = parseProvidersJson(dto.providersJson)
        
        return DailyEntry(
            id = dto.id,
            userId = dto.userId,
            date = dto.date,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            providers = providers,
            notes = dto.notes,
            photoUrls = dto.photoUrls,
            isSynced = dto.isSynced,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
    
    fun toDto(domain: DailyEntry): DailyEntryDto {
        val providersJson = serializeProvidersToJson(domain.providers)
        
        return DailyEntryDto(
            id = domain.id,
            userId = domain.userId,
            date = domain.date,
            driverId = domain.driverId,
            vehicleId = domain.vehicleId,
            providersJson = providersJson,
            notes = domain.notes,
            photoUrls = domain.photoUrls,
            isSynced = domain.isSynced,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    fun toDomainList(dtoList: List<DailyEntryDto>): List<DailyEntry> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<DailyEntry>): List<DailyEntryDto> {
        return domainList.map { toDto(it) }
    }
    
    /**
     * Parse providers from JSON string
     */
    private fun parseProvidersJson(json: String): List<ProviderEarning> {
        if (json.isBlank()) return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val providers = mutableListOf<ProviderEarning>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val typeString = jsonObject.optString("type", "OTHER").uppercase()
                val type = try {
                    ProviderType.valueOf(typeString)
                } catch (e: Exception) {
                    ProviderType.OTHER
                }
                
                val amount = jsonObject.optDouble("amount", 0.0)
                val currency = jsonObject.optString("currency", "AED")
                val tripsCount = if (jsonObject.has("tripsCount")) {
                    jsonObject.optInt("tripsCount")
                } else {
                    null
                }
                
                val meta = if (jsonObject.has("meta")) {
                    val metaJson = jsonObject.optJSONObject("meta")
                    metaJson?.let { jsonObjectToMap(it) }
                } else {
                    null
                }
                
                providers.add(
                    ProviderEarning(
                        type = type,
                        amount = amount,
                        currency = currency,
                        tripsCount = tripsCount,
                        meta = meta
                    )
                )
            }
            
            providers
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Serialize providers to JSON string
     */
    private fun serializeProvidersToJson(providers: List<ProviderEarning>): String {
        if (providers.isEmpty()) return "[]"
        
        return try {
            val jsonArray = JSONArray()
            
            providers.forEach { provider ->
                val jsonObject = JSONObject()
                jsonObject.put("type", provider.type.name)
                jsonObject.put("amount", provider.amount)
                jsonObject.put("currency", provider.currency)
                
                if (provider.tripsCount != null) {
                    jsonObject.put("tripsCount", provider.tripsCount)
                }
                
                if (provider.meta != null) {
                    val metaJson = mapToJsonObject(provider.meta)
                    jsonObject.put("meta", metaJson)
                }
                
                jsonArray.put(jsonObject)
            }
            
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }
    
    /**
     * Convert JSONObject to Map
     */
    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.opt(key)
        }
        return map
    }
    
    /**
     * Convert Map to JSONObject
     */
    private fun mapToJsonObject(map: Map<String, Any?>): JSONObject {
        val jsonObject = JSONObject()
        map.forEach { (key, value) ->
            jsonObject.put(key, value)
        }
        return jsonObject
    }
}
