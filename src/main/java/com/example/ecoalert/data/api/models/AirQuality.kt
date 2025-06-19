package com.example.ecoalert.data.api.models

data class AirQuality(
    val aqi: Int = 0,
    val pm25: Double = 0.0,
    val pm10: Double = 0.0,
    val co: Double = 0.0,
    val no2: Double = 0.0,
    val o3: Double = 0.0,
    val so2: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun getAqiLevel(): AqiLevel {
        return when (aqi) {
            1 -> AqiLevel.GOOD
            2 -> AqiLevel.FAIR
            3 -> AqiLevel.MODERATE
            4 -> AqiLevel.POOR
            5 -> AqiLevel.VERY_POOR
            else -> AqiLevel.UNKNOWN
        }
    }

    fun getHealthRecommendation(): String {
        return when (getAqiLevel()) {
            AqiLevel.GOOD -> "Jakość powietrza jest dobra. Możesz swobodnie przebywać na zewnątrz."
            AqiLevel.FAIR -> "Jakość powietrza jest zadowalająca dla większości osób."
            AqiLevel.MODERATE -> "Osoby wrażliwe mogą odczuwać niewielkie problemy zdrowotne."
            AqiLevel.POOR -> "Osoby wrażliwe mogą odczuwać problemy zdrowotne. Ogranicz aktywność na zewnątrz."
            AqiLevel.VERY_POOR -> "Zagrożenie dla zdrowia. Unikaj aktywności na zewnątrz."
            AqiLevel.UNKNOWN -> "Brak danych o jakości powietrza."
        }
    }
}

enum class AqiLevel(val displayName: String) {
    GOOD("Dobra"),
    FAIR("Zadowalająca"),
    MODERATE("Umiarkowana"),
    POOR("Zła"),
    VERY_POOR("Bardzo zła"),
    UNKNOWN("Nieznana")
}