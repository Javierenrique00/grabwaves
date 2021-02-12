package com.mundocrativo.javier.solosonido.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InfoObj(
    val title : String,
    val channel : String,
    val thumbnailUrl : String,
    val width : Int,
    val height : Int,
    val duration : Int,
    val urlVideo : String,
    val related : List<Related>
)

@JsonClass(generateAdapter = true)
data class Related(
    val id: String,
    val title: String,
    val author : String,
    val duration : Int,
    val iUrl:String
)