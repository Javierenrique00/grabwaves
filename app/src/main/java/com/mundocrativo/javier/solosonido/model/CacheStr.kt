package com.mundocrativo.javier.solosonido.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CacheStr(
    @PrimaryKey(autoGenerate = false) var id:Long,
    var cacheData:String?,
    var timestamp:Long
    )

@Entity
data class CacheMetaStr(
    @PrimaryKey(autoGenerate = false) var id:Long,
    var cacheData:String?,
    var timestamp:Long
)