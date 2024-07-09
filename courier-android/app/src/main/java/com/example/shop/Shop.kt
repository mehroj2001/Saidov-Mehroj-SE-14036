package com.example.shop

import java.io.Serializable

data class Shop(
    val contact_person: String,
    val id: Int,
    val isEnabled: Boolean,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val phone: String
) : Serializable