package com.mundocrativo.javier.solosonido.model

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class VideoObj(
    @PrimaryKey(autoGenerate = true) var id:Long,
    var url:String,
    var title:String,
    var channel:String,
    var thumbnailUrl:String,
    var width:Int,
    var height:Int,
    var duration:Int,
    var timestamp:Long,
    var kindMedia:Int, //---para las listas
    var total_items:Int,  //---cantidad de videos en la lista
    @Ignore var esInfoReady:Boolean,
    @Ignore var esUrlReady:Boolean,
    @Ignore var esSelected:Boolean,
    @Ignore var itemPosition:Int,
    @Ignore var thumbnailImg:Drawable?,
    @Ignore var esPlaying:Boolean,
    @Ignore var durationStr:String

){
    constructor() : this(0,"","","","",0,0,0,0L,0,0,false,false,false,0,null,false,"")

    constructor(url:String) : this(0,url,"","","",0,0,0,0L,0,0,false,false,false,0,null,false,"")
}