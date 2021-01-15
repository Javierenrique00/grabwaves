package com.mundocrativo.javier.solosonido.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchObj(
    val items :  List<VideoSearch>,
    val results : String,
)

@JsonClass(generateAdapter = true)
data class VideoSearch(
    val type : String,
    val isLive: Boolean,
    val isUpcoming: Boolean,
    val title : String?,
    val url : String?,
    val thumbnails : List<MiniaturasList>,
    val author : Author?,
    val description : String?,
    val views : Long?,
    val duration : String?,
    val uploaded_at : String?

)

@JsonClass(generateAdapter = true)
data class Author(
    val name : String?,
    val ref : String?,
    val verified : Boolean?
)

@JsonClass(generateAdapter = true)
data class MiniaturasList(
    val url : String?,
    val width : Int?,
    val height : Int?
)