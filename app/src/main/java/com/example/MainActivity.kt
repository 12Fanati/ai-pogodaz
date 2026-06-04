package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.db.SavedCityRepository
import com.example.ui.screens.WeatherDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room DB and Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val dao = database.savedCityDao()
    val repository = SavedCityRepository(dao)

    // Build ViewModel using standard Factory
    val viewModel: WeatherViewModel by viewModels {
      WeatherViewModel.Factory(application, repository)
    }

    setContent {
      val isDarkTheme by viewModel.isDarkTheme.collectAsState()
      MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          WeatherDashboardScreen(viewModel = viewModel)
        }
      }
    }
  }
}

