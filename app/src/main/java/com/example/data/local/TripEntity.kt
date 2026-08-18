package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destination: String,
    val durationDays: Int,
    val travelerGroup: String,
    val travelStyle: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tripPlanJson: String // Serialized TripPlan object
)
