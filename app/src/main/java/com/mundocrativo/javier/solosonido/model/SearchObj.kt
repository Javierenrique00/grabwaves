package com.mundocrativo.javier.solosonido.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchObj(
    val query : String,
    val currentRef : String,
    val items :  List<VideoSearch>,
    val results : String,
    val nextpageRef : String

)

@JsonClass(generateAdapter = true)
data class VideoSearch(
    val type : String,
    val live: Boolean,
    val title : String?,
    val link : String?,
    val thumbnail : String,
    val author : Author?,
    val description : String?,
    val views : Int?,
    val duration : String?,
    val uploaded_at : String?

)

@JsonClass(generateAdapter = true)
data class Author(
    val name : String?,
    val ref : String?,
    val verified : Boolean?
)