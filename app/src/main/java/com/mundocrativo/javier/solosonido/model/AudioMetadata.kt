package com.mundocrativo.javier.solosonido.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ListaAudioMetadata(
    val list:List<AudioMetadata>
) : Parcelable


@Parcelize
data class AudioMetadata(
    val mediaId : String,
    val title : String,
    val artist : String,
    val url : String,
    val thumbnailUrl : String,
    val thumbnailImg : Bitmap?
) : Parcelable
