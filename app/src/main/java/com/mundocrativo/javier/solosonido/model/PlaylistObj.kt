package com.mundocrativo.javier.solosonido.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaylistObj(
    val id : String?,
    val url : String?,
    val title : String?,
    val total_items : Int?,
    val items : List<PlaylistItems>,
    val error : Boolean
)
@JsonClass(generateAdapter = true)
data class PlaylistItems(
    val url : String?,
    val title : String?,
    val thumbnail : String?,
    val duration : Int,
    val author : String?
)