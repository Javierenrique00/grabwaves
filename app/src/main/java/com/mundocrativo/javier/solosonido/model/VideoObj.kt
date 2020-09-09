package com.mundocrativo.javier.solosonido.model

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
    @Ignore var esInfoReady:Boolean,
    @Ignore var esUrlReady:Boolean,
    @Ignore var esSelected:Boolean
){
    constructor() : this(0,"","","","",0,0,0,0L,false,false,false)
}