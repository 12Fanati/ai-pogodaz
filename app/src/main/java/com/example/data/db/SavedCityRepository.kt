package com.example.data.db

import kotlinx.coroutines.flow.Flow

class SavedCityRepository(private val dao: SavedCityDao) {
    val allSavedCities: Flow<List<SavedCity>> = dao.getAllSavedCities()

    suspend fun insertCity(city: SavedCity) {
        dao.insertCity(city)
    }

    suspend fun deleteCity(city: SavedCity) {
        dao.deleteCity(city)
    }

    suspend fun deleteCityById(id: Int) {
        dao.deleteCityById(id)
    }

    suspend fun isCitySaved(id: Int): Boolean {
        return dao.getCityById(id) != null
    }
}
