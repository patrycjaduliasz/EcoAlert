package com.example.ecoalert.data.repository

import com.example.ecoalert.data.api.ApiService
import com.example.ecoalert.data.api.models.AirQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AirQualityRepository {
    private val api = ApiService.openWeatherMapApi

    suspend fun getCurrentAirQuality(latitude: Double, longitude: Double): Result<AirQuality> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCurrentAirPollution(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = ""
                )
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val pollution = data.list.first()
                        Result.success(
                            AirQuality(
                                aqi = pollution.main.aqi,
                                pm25 = pollution.components.pm2_5,
                                pm10 = pollution.components.pm10,
                                co = pollution.components.co,
                                no2 = pollution.components.no2,
                                o3 = pollution.components.o3,
                                so2 = pollution.components.so2,
                                timestamp = pollution.dt * 1000,
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                    } ?: Result.failure(Exception("Brak danych"))
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}