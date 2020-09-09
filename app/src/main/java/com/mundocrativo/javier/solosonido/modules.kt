package com.mundocrativo.javier.solosonido

import android.app.Application
import androidx.room.Room
import com.mundocrativo.javier.solosonido.db.DataDatabase
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.math.sin

val appModule = module {

    fun provideDatabase(application: Application): DataDatabase{
        return Room.databaseBuilder(application,DataDatabase::class.java,"data_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    fun provideVideoDao(database: DataDatabase):VideoDao {
        return database.videoDao
    }

    //--- instancia de la base de datos
    single { provideDatabase(androidApplication()) }

    //--- instancia VideoDao
    single { provideVideoDao(get()) }


    fun provideAppRepository(videoDao: VideoDao):AppRepository {
        return AppRepository(videoDao)
    }

    single { provideAppRepository(get()) }
}

val viewModule = module {

    viewModel { MainViewModel(get()) }

}