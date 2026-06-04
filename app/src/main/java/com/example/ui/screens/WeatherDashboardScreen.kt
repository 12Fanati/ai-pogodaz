package com.example.ui.screens

import com.example.ui.components.GlassText
import com.example.ui.components.GlassTextMuted
import com.example.ui.components.getGlassTextStyle
import com.example.ui.components.GoogleMapCard
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SavedCity
import com.example.data.model.DailyWeather
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyWeather
import com.example.ui.effects.WeatherBackground
import com.example.ui.effects.WeatherVisualType
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Modern Immersive Atmospheric UI Theme Configuration
val SpaceDark: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val GlowIndigo: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
val GlowSky: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
val TextLight: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground
val TextMuted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val GlassFill: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background == Color(0xFF080B1A)) Color(0xFFF8FAFC).copy(alpha = 0.04f) else Color(0xFF0F172A).copy(alpha = 0.06f)
val GlassBorder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val sandboxFx by viewModel.sandboxFx.collectAsState()

    // Active visual effect layering logic
    val currentEffect = when {
        sandboxFx != null -> sandboxFx!!
        uiState is WeatherUiState.Success -> {
            viewModel.getWeatherVisualType((uiState as WeatherUiState.Success).currentWeather.weathercode)
        }
        else -> WeatherVisualType.CLOUDY
    }

    val isDay = when {
        uiState is WeatherUiState.Success -> (uiState as WeatherUiState.Success).currentWeather.isDay == 1
        else -> true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDark)
    ) {
        WeatherBackground(
            weatherType = currentEffect,
            isDay = isDay,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle cosmic gradient glow overlaid on top for extreme visual depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlowIndigo, Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    0 -> RealTimeWeatherScreen(viewModel = viewModel, uiState = uiState)
                    1 -> VisualSandboxScreen(viewModel = viewModel, activeFx = currentEffect)
                    2 -> CitiesSearchScreen(viewModel = viewModel)
                    3 -> AiAssistantScreen(viewModel = viewModel)
                }
            }

            BottomNavigationBar(
                selectedTabIndex = currentTab,
                onTabSelected = { index ->
                    viewModel.setTab(index)
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))),
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.height(72.dp)
        ) {
            NavigationBarItem(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = "Weather"
                    )
                },
                label = { Text("Погода", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6366F1),
                    selectedTextColor = Color(0xFF6366F1),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.White.copy(alpha = 0.12f)
                )
            )

            NavigationBarItem(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.BlurOn,
                        contentDescription = "FX Sandbox"
                    )
                },
                label = { Text("FX Лаб", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6366F1),
                    selectedTextColor = Color(0xFF6366F1),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.White.copy(alpha = 0.12f)
                )
            )

            NavigationBarItem(
                selected = selectedTabIndex == 2,
                onClick = { onTabSelected(2) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                },
                label = { Text("Поиск", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6366F1),
                    selectedTextColor = Color(0xFF6366F1),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.White.copy(alpha = 0.12f)
                )
            )

            NavigationBarItem(
                selected = selectedTabIndex == 3,
                onClick = { onTabSelected(3) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Assistant"
                    )
                },
                label = { Text("ИИ Советник", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFC084FC),
                    selectedTextColor = Color(0xFFC084FC),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun RealTimeWeatherScreen(
    viewModel: WeatherViewModel,
    uiState: WeatherUiState
) {
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "RealTimeWeatherState"
    ) { state ->
        when (state) {
            is WeatherUiState.Idle,
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Получение метеоданных...",
                            color = TextLight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
            is WeatherUiState.Success -> {
                val current = state.currentWeather
                val hourly = state.hourly
                val daily = state.daily

                val currentHourIndex = hourly?.time?.indexOfFirst { it == current.time }?.coerceAtLeast(0) ?: 0
                val temperature = current.temperature
                val descriptionStr = viewModel.mapWeatherCodeToDescription(current.weathercode, current.isDay == 1)
                val windSpeed = current.windspeed
                val humidity = hourly?.relativeHumidity2m?.getOrNull(currentHourIndex) ?: 62.0
                val feelsLike = hourly?.apparentTemperature?.getOrNull(currentHourIndex) ?: (temperature - (windSpeed * 0.1))

                val maxTemp = daily?.temperature2mMax?.firstOrNull() ?: temperature
                val minTemp = daily?.temperature2mMin?.firstOrNull() ?: temperature

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "Location",
                                        tint = TextLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    GlassText(
                                        text = state.city.name,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleSavedCity(state.city, state.isSaved)
                                        },
                                        modifier = Modifier.size(24.dp).testTag("save_city_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (state.isSaved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (state.isSaved) Color(0xFFFFD54F) else TextLight.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                GlassTextMuted(
                                    text = state.city.country ?: "Координаты: ${state.city.latitude}, ${state.city.longitude}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                                GlassIconButton(
                                    icon = if (isDarkTheme) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier.testTag("theme_toggle")
                                )
                                GlassIconButton(
                                    icon = Icons.Rounded.Search,
                                    onClick = { viewModel.setTab(2) }
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier.padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .drawBehind {
                                        val radialRadius = this.size.width * 0.5f
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.15f), Color.Transparent),
                                                radius = radialRadius
                                            ),
                                            radius = radialRadius
                                        )
                                    }
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getWeatherBigIcon(current.weathercode, current.isDay == 1),
                                    contentDescription = "Weather Icon",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(bottom = 6.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    GlassText(
                                        text = "${temperature.toInt()}",
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        letterSpacing = (-4).sp,
                                        modifier = Modifier.testTag("current_temp_text")
                                    )
                                    GlassText(
                                        text = "°",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Light,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                GlassText(
                                    text = descriptionStr,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                GlassTextMuted(
                                    text = "Макс: ${maxTemp.toInt()}°  Мин: ${minTemp.toInt()}°",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    item {
                        AiQuickSummaryCard(
                            aiSummary = state.aiSummary,
                            isGenerating = isAiGenerating,
                            onRegenerate = {
                                viewModel.fetchWeatherForCity(state.city)
                            },
                            onNavigateToAi = {
                                viewModel.setTab(3)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            GlassParameterCard(
                                modifier = Modifier.weight(1f),
                                label = "Ветер",
                                value = "${windSpeed.toInt()} км/ч",
                                icon = Icons.Rounded.Air,
                                colorAccent = Color(0xFF6366F1)
                            )
                            GlassParameterCard(
                                modifier = Modifier.weight(1f),
                                label = "Влажность",
                                value = "${humidity.toInt()}%",
                                icon = Icons.Rounded.WaterDrop,
                                colorAccent = Color(0xFF0EA5E9)
                            )
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 20.dp)
                        ) {
                            GlassParameterCard(
                                modifier = Modifier.weight(1f),
                                label = "Ощущается",
                                value = "${feelsLike.toInt()}°C",
                                icon = Icons.Rounded.Thermostat,
                                colorAccent = Color(0xFFF43F5E)
                            )
                            GlassParameterCard(
                                modifier = Modifier.weight(1f),
                                label = "Координаты",
                                value = "${state.city.latitude.toString().take(5)}°N",
                                icon = Icons.Rounded.LocationSearching,
                                colorAccent = Color(0xFFF59E0B)
                            )
                        }
                    }

                    item {
                        GoogleMapCard(
                            latitude = state.city.latitude,
                            longitude = state.city.longitude,
                            cityName = state.city.name,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }

                    item {
                        GlassHourlyForecastCard(hourly = hourly, currentHourIndex = currentHourIndex, viewModel = viewModel)
                    }
                }
            }
            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = "Error",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Возникла ошибка загрузки данных",
                            color = TextLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = TextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GlassFill)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable { onClick() }
            .testTag("glass_icon_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun GlassParameterCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    colorAccent: Color
) {
    Box(
        modifier = modifier
            .background(GlassFill, RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = colorAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                GlassTextMuted(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
                GlassText(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun GlassHourlyForecastCard(
    hourly: HourlyWeather?,
    currentHourIndex: Int,
    viewModel: WeatherViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassFill, RoundedCornerShape(28.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassText(
                    text = "Почасовой Прогноз (24ч)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "СЕЙЧАС",
                    color = Color(0xFFC084FC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val times = hourly?.time
            val temps = hourly?.temperature2m

            if (times != null && temps != null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val itemsCount = minOf(times.size - currentHourIndex, temps.size - currentHourIndex, 24).coerceAtLeast(0)
                    items((0 until itemsCount).toList()) { offset ->
                        val index = currentHourIndex + offset
                        val timeStr = times[index]
                        val tempVal = temps[index]
                        
                        // Parse time ISO formatted "2026-05-21T18:00"
                        val parsedTime = try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                            val date = sdf.parse(timeStr)
                            val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            outSdf.format(date ?: Date())
                        } catch (e: Exception) {
                            timeStr.substringAfter("T")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = parsedTime,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Icon(
                                imageVector = getWeatherIconSmall(index % 8),
                                contentDescription = "Weather Hourly Icon",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${tempVal.roundToInt()}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Данные почасового прогноза недоступны",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun AiQuickSummaryCard(
    aiSummary: String,
    isGenerating: Boolean,
    onRegenerate: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.12f),
                        Color(0xFFC084FC).copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFFC084FC).copy(alpha = 0.18f)
                        )
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable { onNavigateToAi() }
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Panel Icon",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                    GlassText(
                        text = "Персональный ИИ Анализ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { onRegenerate() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Regenerate",
                        tint = TextLight.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isGenerating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFC084FC),
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp
                    )
                    GlassTextMuted(
                        text = "Связываемся со спутником ИИ...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            } else {
                GlassText(
                    text = aiSummary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VisualSandboxScreen(
    viewModel: WeatherViewModel,
    activeFx: WeatherVisualType
) {
    val options = listOf(
        Triple(WeatherVisualType.SUNNY, "Ясно", Icons.Rounded.WbSunny),
        Triple(WeatherVisualType.CLOUDY, "Облачно", Icons.Rounded.Cloud),
        Triple(WeatherVisualType.FOGGY, "Туман", Icons.Rounded.BlurOn),
        Triple(WeatherVisualType.RAINY, "Дождь", Icons.Rounded.WaterDrop),
        Triple(WeatherVisualType.SNOWY, "Снег", Icons.Rounded.AcUnit),
        Triple(WeatherVisualType.THUNDERSTORM, "Гроза", Icons.Rounded.Thunderstorm)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(44.dp))
            GlassText(
                text = "Лаборатория FX",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            GlassIconButton(
                icon = if (isDarkTheme) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.testTag("theme_toggle_fx")
            )
        }
        GlassTextMuted(
            text = "Переключайте и тестируйте динамически генерируемые погодные эффекты",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(GlassFill, RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = when (activeFx) {
                        WeatherVisualType.SUNNY -> Icons.Rounded.WbSunny
                        WeatherVisualType.CLOUDY -> Icons.Rounded.Cloud
                        WeatherVisualType.FOGGY -> Icons.Rounded.BlurOn
                        WeatherVisualType.RAINY -> Icons.Rounded.WaterDrop
                        WeatherVisualType.SNOWY -> Icons.Rounded.AcUnit
                        WeatherVisualType.THUNDERSTORM -> Icons.Rounded.Thunderstorm
                    },
                    contentDescription = "Active FX",
                    tint = TextLight,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassText(
                    text = when (activeFx) {
                        WeatherVisualType.SUNNY -> "Солнечные лучи и золотой свет"
                        WeatherVisualType.CLOUDY -> "Трехмерные плывущие облака"
                        WeatherVisualType.FOGGY -> "Светлый стелющийся туман"
                        WeatherVisualType.RAINY -> "Интерактивный сильный дождь"
                        WeatherVisualType.SNOWY -> "Реалистичные падающие снежинки"
                        WeatherVisualType.THUNDERSTORM -> "Буря с яркими ветвящимися молниями"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val chunkedOptions = options.chunked(2)
            chunkedOptions.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { (type, title, icon) ->
                        val isSelected = activeFx == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .background(
                                    if (isSelected) Color(0xFF6366F1).copy(alpha = 0.25f) else GlassFill,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF6366F1) else GlassBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.setSandboxFx(type)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) Color(0xFFFFEA00) else TextLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                GlassText(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Light
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = { viewModel.setSandboxFx(null) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Sync",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Синхронизировать с прогнозом",
                        color = Color(0xFF22C55E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CitiesSearchScreen(
    viewModel: WeatherViewModel
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassText(
                text = "Избранные города",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            GlassIconButton(
                icon = if (isDarkTheme) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.testTag("theme_toggle_search")
            )
        }

        // Search bar input
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_city_input_tab"),
            placeholder = { Text("Поиск по названию...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = TextLight
                )
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.45f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                focusedContainerColor = Color.White.copy(alpha = 0.12f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (query.isNotEmpty()) {
            GlassText(
                text = "Результаты поиска",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            if (results.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassTextMuted(
                        text = "Ничего не найдено.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(results, key = { it.id }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassFill, RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    focusManager.clearFocus()
                                    viewModel.selectSearchResult(result)
                                    viewModel.setTab(0)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                GlassText(
                                    text = result.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                GlassTextMuted(
                                    text = "${result.country ?: ""} ${result.admin1 ?: ""}",
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Выбрать",
                                color = Color(0xFF6366F1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp)
            ) {
                GlassText(
                    text = "Избранные города",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Room БД",
                    color = Color(0xFF0EA5E9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }

            if (savedCities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(GlassFill, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.StarBorder,
                            contentDescription = "Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassTextMuted(
                            text = "Список избранного пуст.\nДобавляйте города через главную страницу.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(savedCities, key = { city -> city.id }) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassFill, RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.fetchWeatherForCity(city)
                                    viewModel.setTab(0)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "City",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    GlassText(
                                        text = city.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    GlassTextMuted(
                                        text = city.country ?: "Координаты: ${city.latitude}, ${city.longitude}",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteSavedCity(city) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    viewModel: WeatherViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    var customQuestion by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val promptSuggestions = listOf(
        "Что одеть по текущей погоде?",
        "Каковы шансы промокнуть сегодня?",
        "Дай 5 советов на текущий день",
        "Предложи стильный аутфит под погоду"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Panel",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(28.dp)
                        )
                        GlassText(
                            text = "ИИ Консультант",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                    GlassIconButton(
                        icon = if (isDarkTheme) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_ai")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                GlassTextMuted(
                    text = "Использует умную языковую модель Google Gemini для персональных рекомендаций",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassFill, RoundedCornerShape(24.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC084FC).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Sparkle",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        val currentCityName = when (uiState) {
                            is WeatherUiState.Success -> (uiState as WeatherUiState.Success).city.name
                            else -> "Выбранный город"
                        }
                        GlassText(
                            text = "Анализ для: $currentCityName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (uiState is WeatherUiState.Success) {
                        val state = uiState as WeatherUiState.Success
                        if (isAiGenerating) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFFC084FC))
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassTextMuted(
                                    text = "Gemini подготавливает полезный совет...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        } else {
                            GlassText(
                                text = state.aiSummary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else {
                        GlassTextMuted(
                            text = "Чтобы побеседовать с ИИ-синоптиком, загрузите погодный профиль любого города.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                GlassText(
                    text = "Спросите ИИ о чем-то конкретном:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    promptSuggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .background(GlassFill, RoundedCornerShape(30.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(30.dp))
                                .clickable {
                                    if (uiState is WeatherUiState.Success) {
                                        customQuestion = suggestion
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassFill, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    placeholder = {
                        Text(
                            "Задайте свой вопрос...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (customQuestion.isNotBlank() && uiState is WeatherUiState.Success) {
                            focusManager.clearFocus()
                            val success = uiState as WeatherUiState.Success
                            viewModel.fetchWeatherForCity(
                                success.city.copy(
                                    name = "${success.city.name} (Запрос: $customQuestion)"
                                )
                            )
                            customQuestion = ""
                        }
                    },
                    modifier = Modifier.testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color(0xFFC084FC)
                    )
                }
            }
        }
    }
}

fun getWeatherBigIcon(code: Int, isDay: Boolean): ImageVector {
    return when (code) {
        0 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        1 -> if (isDay) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay
        2 -> if (isDay) Icons.Rounded.WbCloudy else Icons.Rounded.Cloud
        3 -> Icons.Rounded.Cloud
        45, 48 -> Icons.Rounded.FilterDrama
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Rounded.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
        95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.Cloud
    }
}

fun getWeatherIconSmall(fakeIndex: Int): ImageVector {
    return when (fakeIndex) {
        0 -> Icons.Rounded.WbSunny
        1 -> Icons.Rounded.Cloud
        2 -> Icons.Rounded.WaterDrop
        3 -> Icons.Rounded.Thunderstorm
        4 -> Icons.Rounded.AcUnit
        5 -> Icons.Rounded.WbCloudy
        6 -> Icons.Rounded.Grain
        else -> Icons.Rounded.Cloud
    }
}
