package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.WeatherApiClient
import com.example.data.db.AppDatabase
import com.example.data.db.SavedCity
import com.example.data.db.SavedCityRepository
import com.example.data.gemini.GeminiClient
import com.example.data.model.DailyWeather
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyWeather
import com.example.data.model.WeatherResponse
import com.example.ui.effects.WeatherVisualType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(
        val city: SavedCity,
        val currentWeather: com.example.data.model.CurrentWeather,
        val hourly: HourlyWeather?,
        val daily: DailyWeather?,
        val aiSummary: String,
        val isSaved: Boolean
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(
    application: Application,
    private val savedCityRepository: SavedCityRepository
) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(0) // 0: Weather, 1: FX Sandbox, 2: Favorites & Search, 3: AI Assistant
    val currentTab = _currentTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    private val _sandboxFx = MutableStateFlow<WeatherVisualType?>(null)
    val sandboxFx = _sandboxFx.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setSandboxFx(fx: WeatherVisualType?) {
        _sandboxFx.value = fx
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val savedCities: StateFlow<List<SavedCity>> = savedCityRepository.allSavedCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating = _isAiGenerating.asStateFlow()

    init {
        // Load default/last searched city if possible
        viewModelScope.launch {
            val latest = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(application).savedCityDao().getLatestSavedCity()
            }
            if (latest != null) {
                fetchWeatherForCity(latest)
            } else {
                // Default to Moscow
                val moscow = SavedCity(
                    id = 524901,
                    name = "Москва",
                    latitude = 55.7522,
                    longitude = 37.6156,
                    country = "Россия",
                    countryCode = "RU",
                    admin1 = "Москва"
                )
                fetchWeatherForCity(moscow)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchCity(query)
        } else {
            _searchResults.value = emptyList()
        }
    }

    private fun searchCity(query: String) {
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    WeatherApiClient.geocodingService.searchCity(query)
                }
                _searchResults.value = response.results ?: emptyList()
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectSearchResult(result: GeocodingResult) {
        val city = SavedCity(
            id = result.id,
            name = result.name,
            latitude = result.latitude,
            longitude = result.longitude,
            country = result.country,
            countryCode = result.countryCode,
            admin1 = result.admin1,
            timestamp = System.currentTimeMillis() // Update timestamp to put it on top next launch
        )
        // Auto save to history or list
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                savedCityRepository.insertCity(city)
            }
        }
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        fetchWeatherForCity(city)
    }

    fun fetchWeatherForCity(city: SavedCity) {
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                // Fetch data from open-meteo
                val forecast = withContext(Dispatchers.IO) {
                    WeatherApiClient.forecastService.getForecast(city.latitude, city.longitude)
                }

                val cur = forecast.currentWeather
                if (cur == null) {
                    _uiState.value = WeatherUiState.Error("Не удалось получить текущую погоду для ${city.name}")
                    return@launch
                }

                val isSaved = withContext(Dispatchers.IO) {
                    savedCityRepository.isCitySaved(city.id)
                }

                _uiState.value = WeatherUiState.Success(
                    city = city,
                    currentWeather = cur,
                    hourly = forecast.hourly,
                    daily = forecast.daily,
                    aiSummary = "Генерация умной ИИ-сводки...",
                    isSaved = isSaved
                )

                generateAiSummary(city, cur)

            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Ошибка загрузки погоды: ${e.localizedMessage}")
            }
        }
    }

    private fun generateAiSummary(city: SavedCity, current: com.example.data.model.CurrentWeather) {
        _isAiGenerating.value = true
        viewModelScope.launch {
            try {
                val conditionString = mapWeatherCodeToDescription(current.weathercode, current.isDay == 1)
                val summary = withContext(Dispatchers.IO) {
                    GeminiClient.getAiWeatherSummary(
                        cityName = city.name,
                        temperature = current.temperature,
                        condition = conditionString,
                        humidity = 68.0, // Default or fallback
                        windSpeed = current.windspeed,
                        feelsLike = current.temperature - (current.windspeed * 0.1) // simple windchill approximation
                    )
                }

                val currentState = _uiState.value
                if (currentState is WeatherUiState.Success && currentState.city.id == city.id) {
                    _uiState.value = currentState.copy(aiSummary = summary)
                }
            } catch (e: Exception) {
                // silent failure for AI summary
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun toggleSavedCity(city: SavedCity, currentlySaved: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (currentlySaved) {
                    savedCityRepository.deleteCityById(city.id)
                } else {
                    savedCityRepository.insertCity(city.copy(timestamp = System.currentTimeMillis()))
                }
            }
            // Update uiState to reflect the change
            val currentState = _uiState.value
            if (currentState is WeatherUiState.Success && currentState.city.id == city.id) {
                _uiState.value = currentState.copy(isSaved = !currentlySaved)
            }
        }
    }

    fun deleteSavedCity(city: SavedCity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                savedCityRepository.deleteCity(city)
            }
            // If the deleted city is currently viewed, update its "isSaved" flag so star looks right
            val currentState = _uiState.value
            if (currentState is WeatherUiState.Success && currentState.city.id == city.id) {
                _uiState.value = currentState.copy(isSaved = false)
            }
        }
    }

    fun getWeatherVisualType(code: Int): WeatherVisualType {
        return when (code) {
            0 -> WeatherVisualType.SUNNY
            1, 2, 3 -> WeatherVisualType.CLOUDY
            45, 48 -> WeatherVisualType.FOGGY
            51, 53, 55, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherVisualType.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherVisualType.SNOWY
            95, 96, 99 -> WeatherVisualType.THUNDERSTORM
            else -> WeatherVisualType.CLOUDY
        }
    }

    fun mapWeatherCodeToDescription(code: Int, isDay: Boolean = true): String {
        return when (code) {
            0 -> if (isDay) "Ясно" else "Ясная ночь"
            1 -> if (isDay) "Преимущественно ясно" else "Преимущественно ясная ночь"
            2 -> if (isDay) "Переменная облачность" else "Облачно с прояснениями"
            3 -> "Пасмурно"
            45 -> "Туман"
            48 -> "Осаждающийся туман"
            51 -> "Легкая морось"
            53 -> "Умеренная морось"
            55 -> "Плотная морось"
            61 -> "Слабый дождь"
            63 -> "Умеренный дождь"
            65 -> "Сильный дождь"
            66 -> "Слабый ледяной дождь"
            67 -> "Сильный ледяной дождь"
            71 -> "Небольшой снегопад"
            73 -> "Умеренный снегопад"
            75 -> "Сильный снегопад"
            77 -> "Снежные зерна"
            80 -> "Слабый ливень"
            81 -> "Умеренный ливень"
            82 -> "Сильный ливень"
            85 -> "Слабый снежный ливень"
            86 -> "Сильный снежный ливень"
            95 -> "Гроза"
            96 -> "Гроза с градом"
            99 -> "Сильная гроза с градом"
            else -> "Переменчивая погода"
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val application: Application,
        private val repository: SavedCityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
