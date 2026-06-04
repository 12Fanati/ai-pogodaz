package com.example.data.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getAiWeatherSummary(
        cityName: String,
        temperature: Double,
        condition: String,
        humidity: Double,
        windSpeed: Double,
        feelsLike: Double
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Для работы ИИ-аналитики укажите реальный GEMINI_API_KEY в панели Secrets.\n\nТекущая сводка для г. $cityName: температура составляет $temperature°C, ощущается как $feelsLike°C. На улице $condition. Ветер дует со скоростью $windSpeed км/ч, относительная влажность воздуха — $humidity%."
        }

        val prompt = if (cityName.contains(" (Запрос: ") && cityName.endsWith(")")) {
            val cleanCity = cityName.substringBefore(" (Запрос: ")
            val question = cityName.substringAfter(" (Запрос: ").removeSuffix(")")
            "Пользователь спрашивает по поводу погоды: \"$question\"\n\n" +
            "Ответь на этот вопрос кратко, тепло и дружелюбно (2-3 предложения), используя следующие погодные данные для г. $cleanCity:\n" +
            "- Температура: $temperature°C (ощущается как $feelsLike°C)\n" +
            "- Погодные условия: $condition\n" +
            "- Влажность: $humidity%\n" +
            "- Скорость ветра: $windSpeed км/ч"
        } else {
            "Предоставь краткий (2-3 предложения), интересный и дружелюбный прогноз погоды для города $cityName на основе следующих данных: " +
            "Температура: $temperature°C, ощущается как $feelsLike°C, Описание: $condition, Влажность: $humidity%, Скорость ветра: $windSpeed км/ч. " +
            "Сделай его живым, позитивным и дай полезный/забавный совет для жителей на русском языке."
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "Ты — дружелюбный синоптик и помощник по погоде. Твои ответы теплы, остроумны и заботливы."))
            )
        )

        var lastException: Exception? = null
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        for (model in modelsToTry) {
            try {
                val response = service.generateContent(model, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    return text
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        return "Возникла ошибка при работе с ИИ: ${lastException?.localizedMessage ?: "доступные модели временно недоступны"}. \n\nТекущая погода: $temperature°C, $condition."
    }
}
