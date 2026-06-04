package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cities")
data class SavedCity(
    @PrimaryKey val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val countryCode: String?,
    val admin1: String?,
    val timestamp: Long = System.currentTimeMillis()
)
