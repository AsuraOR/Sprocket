package com.example.sprocket.data.model

data class ServiceRecord(
    val id: String,
    val partId: String,
    val year: Int,
    val month: Int,
    val odometerKm: Int,
    val cost: Long,
    val performer: String, // "DIY" or "Dealer"
    val note: String
)
