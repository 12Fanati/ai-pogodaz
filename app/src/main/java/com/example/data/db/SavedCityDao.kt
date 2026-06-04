package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCityDao {
    @Query("SELECT * FROM saved_cities ORDER BY timestamp DESC")
    fun getAllSavedCities(): Flow<List<SavedCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: SavedCity)

    @Delete
    suspend fun deleteCity(city: SavedCity)

    @Query("DELETE FROM saved_cities WHERE id = :id")
    suspend fun deleteCityById(id: Int)

    @Query("SELECT * FROM saved_cities WHERE id = :id LIMIT 1")
    suspend fun getCityById(id: Int): SavedCity?

    @Query("SELECT * FROM saved_cities ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSavedCity(): SavedCity?
}
