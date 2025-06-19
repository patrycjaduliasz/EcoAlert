package com.example.ecoalert.data.api

import com.example.ecoalert.ui.theme.utils.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiService {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.OPEN_WEATHER_MAP_API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val openWeatherMapApi: OpenWeatherMapApi = retrofit.create(OpenWeatherMapApi::class.java)
}