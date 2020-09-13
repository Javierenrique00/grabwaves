package com.mundocrativo.javier.solosonido

import android.app.Application
import androidx.room.Room
import com.mundocrativo.javier.solosonido.com.CacheStrDao
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.db.DataDatabase
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.ui.config.ConfigViewModel
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import com.mundocrativo.javier.solosonido.ui.search.SearchViewModel
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



    fun provideAppRepository(videoDao: VideoDao,directCache: DirectCache):AppRepository {
        return AppRepository(videoDao,directCache)
    }

    single { provideAppRepository(get(),get()) }
}

val viewModule = module {

    viewModel { MainViewModel(get()) }

    viewModel { ConfigViewModel() }

    viewModel { SearchViewModel(get()) }

}