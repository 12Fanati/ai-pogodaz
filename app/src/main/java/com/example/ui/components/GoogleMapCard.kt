package com.example.ui.components

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.screens.GlassBorder
import com.example.ui.screens.GlassFill

enum class MapType {
    OSM,
    YANDEX,
    GOOGLE
}

@Composable
fun GoogleMapCard(
    latitude: Double,
    longitude: Double,
    cityName: String,
    modifier: Modifier = Modifier
) {
    var selectedMapType by remember { mutableStateOf(MapType.OSM) }

    val leafletHtml = remember(latitude, longitude, cityName) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: #0d1117;
                }
                .leaflet-bar {
                    border: none !important;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.5) !important;
                }
                .leaflet-bar a {
                    background-color: #161b22 !important;
                    color: #c9d1d9 !important;
                    border: 1px solid #30363d !important;
                }
                .leaflet-bar a:hover {
                    background-color: #21262d !important;
                    color: #f0f6fc !important;
                }
                .leaflet-control-attribution {
                    display: none !important;
                }
                .leaflet-popup-content-wrapper {
                    background: #161b22 !important;
                    color: #f0f6fc !important;
                    border: 1px solid #30363d !important;
                    border-radius: 8px !important;
                }
                .leaflet-popup-tip {
                    background: #161b22 !important;
                    border: 1px solid #30363d !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script>
                var map = L.map('map', {
                    zoomControl: true,
                    attributionControl: false
                }).setView([$latitude, $longitude], 10);
                
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 20
                }).addTo(map);
                
                var marker = L.marker([$latitude, $longitude]).addTo(map);
                marker.bindPopup("<b style='font-family:sans-serif;'>$cityName</b>").openPopup();
                
                // Add resize check
                setTimeout(function() {
                    map.invalidateSize();
                }, 400);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val yandexUrl = remember(latitude, longitude) {
        "https://yandex.ru/map-widget/v1/?ll=$longitude,$latitude&z=10&l=map&pt=$longitude,$latitude,pm2rdm"
    }

    val googleHtml = remember(latitude, longitude) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    background-color: #0d1117;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <iframe 
                src="https://maps.google.com/maps?q=$latitude,$longitude&z=12&ie=UTF8&iwloc=&output=embed"
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        // Map Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = "Map Icon",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Интерактивная карта",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Координаты: $latitude° N, $longitude° E",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // Custom High-Performance Modern Toggle Switcher
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val mapOptions = listOf(
                MapType.OSM to "Тёмная OSM",
                MapType.YANDEX to "Яндекс",
                MapType.GOOGLE to "Google Web"
            )
            mapOptions.forEach { (type, label) ->
                val isSelected = selectedMapType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedMapType = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black.copy(alpha = 0.2f))
        ) {
            AndroidView(
                factory = { webContext ->
                    WebView(webContext).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return false
                                // Intercept custom URLs (intent://, yandextaxi://, yandexmaps://, maps://) to block ERR_UNKNOWN_URL_SCHEME
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    return true // block
                                }
                                return false // allow default load
                            }
                        }
                        webChromeClient = WebChromeClient()
                        
                        // Load initial map
                        when (selectedMapType) {
                            MapType.OSM -> {
                                tag = leafletHtml
                                loadDataWithBaseURL("https://localhost", leafletHtml, "text/html", "UTF-8", null)
                            }
                            MapType.YANDEX -> {
                                tag = yandexUrl
                                loadUrl(yandexUrl)
                            }
                            MapType.GOOGLE -> {
                                tag = googleHtml
                                loadDataWithBaseURL("https://maps.google.com", googleHtml, "text/html", "UTF-8", null)
                            }
                        }
                    }
                },
                update = { webView ->
                    // Correctly update during state changes to avoid unnecessary reloads
                    when (selectedMapType) {
                        MapType.OSM -> {
                            val lastLoaded = webView.tag as? String
                            if (lastLoaded != leafletHtml) {
                                webView.tag = leafletHtml
                                webView.loadDataWithBaseURL("https://localhost", leafletHtml, "text/html", "UTF-8", null)
                            }
                        }
                        MapType.YANDEX -> {
                            val lastLoaded = webView.tag as? String
                            if (lastLoaded != yandexUrl) {
                                webView.tag = yandexUrl
                                webView.loadUrl(yandexUrl)
                            }
                        }
                        MapType.GOOGLE -> {
                            val lastLoaded = webView.tag as? String
                            if (lastLoaded != googleHtml) {
                                webView.tag = googleHtml
                                webView.loadDataWithBaseURL("https://maps.google.com", googleHtml, "text/html", "UTF-8", null)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Geo-Telemetry Floating badge
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = "Explore",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(13.dp)
                    )
                    val displayProviderName = when (selectedMapType) {
                        MapType.OSM -> "OSM Лифлет"
                        MapType.YANDEX -> "Яндекс"
                        MapType.GOOGLE -> "Google Web"
                    }
                    Text(
                        text = "Регион: $cityName ($displayProviderName)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
