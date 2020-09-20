package com.mundocrativo.javier.solosonido

import android.app.Application
import android.content.ComponentName
import androidx.room.Room
import com.mundocrativo.javier.solosonido.com.CacheStrDao
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.db.DataDatabase
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.service.MusicService
import com.mundocrativo.javier.solosonido.service.MusicServiceConnection
import com.mundocrativo.javier.solosonido.ui.config.ConfigViewModel
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import com.mundocrativo.javier.solosonido.ui.player.PlayerViewModel
import com.mundocrativo.javier.solosonido.ui.search.SearchViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    fun provideDatabase(application: Application): DataDatabase{
        return Room.databaseBuilder(application,DataDatabase::class.java,"data_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    fun provideVideoDao(database: DataDatabase):VideoDao {
        return database.videoDao
    }

    fun provideCacheStrDao(database: DataDatabase):CacheStrDao {
        return database.cacheStrDao
    }

    //--- instancia de la base de datos
    single { provideDatabase(androidApplication()) }

    //--- instancia VideoDao
    single { provideVideoDao(get()) }

    //--- instancia CacheStrDao
    single { provideCacheStrDao(get()) }


    fun provideDirectCache(cacheStrDao: CacheStrDao):DirectCache{
        return DirectCache(cacheStrDao)
    }

    single { provideDirectCache(get()) }

    //--- necesita el nombre del servicio del MediaBrowser, el cual se pone como parámetro
    fun provideMusicServiceConnection(application: Application):MusicServiceConnection{
        return MusicServiceConnection(application.baseContext, ComponentName(application.baseContext, MusicService::class.java)
        )
    }

    single { provideMusicServiceConnection(get()) }


    fun provideAppRepository(videoDao: VideoDao,directCache: DirectCache,musicServiceConnection: MusicServiceConnection):AppRepository {
        return AppRepository(videoDao,directCache,musicServiceConnection)
    }

    single { provideAppRepository(get(),get(),get()) }

}

val viewModule = module {

    viewModel { MainViewModel(get()) }

    viewModel { ConfigViewModel() }

    viewModel { SearchViewModel(get()) }

    viewModel { PlayerViewModel(get()) }

}