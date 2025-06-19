package com.example.ecoalert.data.api.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val alertEnabled: Boolean = false,
    val createdAt: Long = 0L
) : Parcelable