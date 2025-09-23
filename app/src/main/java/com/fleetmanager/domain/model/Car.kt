package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Domain model representing a car entity stored in Firestore.
 */
data class Car(
    @get:PropertyName("id")
    val id: String = "",

    @get:PropertyName("userId")
    val userId: String = "",

    @get:PropertyName("nickname")
    val nickname: String = "",

    @get:PropertyName("make")
    val make: String = "",

    @get:PropertyName("model")
    val model: String = "",

    @get:PropertyName("year")
    val year: Int? = null,

    @get:PropertyName("licensePlate")
    val licensePlate: String = "",

    @get:PropertyName("color")
    val color: String = "",

    @get:PropertyName("isActive")
    val isActive: Boolean = true
) {
    val displayName: String
        get() = buildString {
            if (nickname.isNotBlank()) {
                append(nickname)
                if (make.isNotBlank() || model.isNotBlank()) {
                    append(" · ")
                }
            }
            if (make.isNotBlank() || model.isNotBlank()) {
                append(listOf(make, model).filter { it.isNotBlank() }.joinToString(" "))
            }
            if (isBlank()) {
                append("Unnamed Car")
            }
        }
}
