package com.mundocrativo.javier.solosonido.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ListaAudioMetadata(
    val list:List<AudioMetadata>
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class AudioMetadata(
    var mediaId : String,
    val title : String,
    val artist : String,
    var url : String,
    val thumbnailUrl : String,
    val duration: Int,
    val extraUrlVideo:String
) : Parcelable
{
    constructor() : this("","","","","",0,"")
}
