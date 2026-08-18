package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM saved_trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM saved_trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Int): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("SELECT * FROM saved_trips WHERE LOWER(destination) = LOWER(:destination) AND durationDays = :durationDays LIMIT 1")
    suspend fun findTripByDestinationAndDuration(destination: String, durationDays: Int): TripEntity?

    @Query("DELETE FROM saved_trips WHERE id = :id")
    suspend fun deleteTripById(id: Int)
}
