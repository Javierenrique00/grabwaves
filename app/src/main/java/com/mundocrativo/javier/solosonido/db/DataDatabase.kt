package com.mundocrativo.javier.solosonido.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mundocrativo.javier.solosonido.com.CacheStrDao
import com.mundocrativo.javier.solosonido.model.CacheStr
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.coroutines.CoroutineScope

//----  https://android.jlelse.eu/painless-android-testing-with-room-koin-bb949eefcbee
//--- para usar Koin con la base de datos

@Database(entities = [VideoObj::class,CacheStr::class],version = 5)
abstract class DataDatabase : RoomDatabase() {

    abstract val videoDao : VideoDao
    abstract val cacheStrDao : CacheStrDao


}