package com.mundocrativo.javier.solosonido.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class QueueObj(
    @PrimaryKey(autoGenerate = true) val id:Long,
    val queueName:String
)