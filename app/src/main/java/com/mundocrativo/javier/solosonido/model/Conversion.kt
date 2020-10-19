package com.mundocrativo.javier.solosonido.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Conversion(
    val file : String,
    val msconverted: Long
)

@JsonClass(generateAdapter = true)
data class Converted(
    val complete: List<String>,
    val conversion: List<Conversion>
)