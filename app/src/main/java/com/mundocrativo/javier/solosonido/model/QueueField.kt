package com.mundocrativo.javier.solosonido.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QueueField(
    @PrimaryKey(autoGenerate = true) val id:Long,
    var queueId:Long,
    val itemId:String, //--este es el url de youtube
    var order:Int,  //-- este es el orden en que aparece desde 0 ascendente
    var lastPlayed:Long  //---guarda cuando fue la ultima vez que se toco
)