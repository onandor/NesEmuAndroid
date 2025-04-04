package com.onandor.nesemu.data.network.sgdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueryResult<T>(
    val success: Boolean,
    val page: Int? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val data: T
)

@Serializable
data class Game(
    val id: Long,
    val name: String,
    @SerialName("release_date")
    val releaseDate: Long? = null
)

@Serializable
data class Grid(
    val id: Long,
    val thumb: String
)