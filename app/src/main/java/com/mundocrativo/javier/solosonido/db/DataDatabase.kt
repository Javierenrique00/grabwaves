package com.mundocrativo.javier.solosonido.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.coroutines.CoroutineScope

//----  https://android.jlelse.eu/painless-android-testing-with-room-koin-bb949eefcbee
//--- para usar Koin con la base de datos

@Database(entities = [VideoObj::class],version = 1)
abstract class DataDatabase : RoomDatabase() {

    abstract val videoDao : VideoDao

//    companion object{
//        @Volatile
//        private var INSTANCE: DataDatabase? = null
//
//        fun getDatabase(context: Context): DataDatabase {
//            val tempInstance =
//                INSTANCE
//            if (tempInstance != null) {
//                return tempInstance
//            }
//            synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    DataDatabase::class.java,
//                    "Data_Database"
//                ).fallbackToDestructiveMigration().build()
//                INSTANCE = instance
//                return instance
//            }
//        }
//    }

}