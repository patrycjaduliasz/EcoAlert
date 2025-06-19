package com.example.ecoalert.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapApi {
    @GET("air_pollution")
    suspend fun getCurrentAirPollution(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): Response<AirPollutionResponse>
}

data class AirPollutionResponse(
    val coord: Coord,
    val list: List<AirPollutionData>
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class AirPollutionData(
    val dt: Long,
    val main: AirQualityMain,
    val components: AirQualityComponents
)

data class AirQualityMain(
    val aqi: Int
)

data class AirQualityComponents(
    val co: Double,
    val no: Double,
    val no2: Double,
    val o3: Double,
    val so2: Double,
    val pm2_5: Double,
    val pm10: Double,
    val nh3: Double
)