package com.example.blockfraudcalls.model

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistNumber (
    val id: String,
    val number: String
)
