package com.fleetmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey
    val id: String,
    val name: String,
    val isActive: Boolean = true
)