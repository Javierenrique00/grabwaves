package com.mundocrativo.javier.solosonido

import android.app.Application
import android.content.ComponentName
import androidx.room.Room
import com.mundocrativo.javier.solosonido.base.GeneralCache
import com.mundocrativo.javier.solosonido.com.CacheMetaDao
import com.mundocrativo.javier.solosonido.com.CacheStrDao
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.com.MetadataCache
import com.mundocrativo.javier.solosonido.db.DataDatabase
import com.mundocrativo.javier.solosonido.db.QueueDao
import com.mundocrativo.javier.solosonido.db.QueueFieldDao
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.CacheMetaStr
import com.mundocrativo.javier.solosonido.model.CacheStr
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

    fun provideQueueDao(database: DataDatabase):QueueDao {
        return database.queueDao
    }

    fun provideQueueFieldDao(database: DataDatabase):QueueFieldDao {
        return database.queueFieldDao
    }

    fun provideCacheMetaDao(database: DataDatabase):CacheMetaDao {
        return database.cacheMetaDao
    }

    //--- instancia de la base de datos
    single { provideDatabase(androidApplication()) }

    //--- instancia VideoDao
    single { provideVideoDao(get()) }

    //--- instancia CacheStrDao
    single { provideCacheStrDao(get()) }

    //--- instancia QueueDao
    single { provideQueueDao(get()) }

    //--- instancia QueueFieldDao
    single { provideQueueFieldDao(get()) }

    //--- instancia CacheMetaDao
    single { provideCacheMetaDao(get()) }


    fun provideDirectCache(cacheStrDao: CacheStrDao):DirectCache{
        return DirectCache(cacheStrDao)
    }

    single { provideDirectCache(get()) }


    fun provideInterfaceCacheMetaDao(dao: CacheMetaDao):GeneralCache.DaoInterface{
        return object : GeneralCache.DaoInterface{
            override fun getData(key: Long): CacheMetaStr? {
                return dao.traeCache(key)
            }

            override fun putData(cache: CacheMetaStr) {
                return dao.insert(cache)
            }
        }
    }

    single { provideInterfaceCacheMetaDao(get()) }

    fun provideMetadataCache(dao:GeneralCache.DaoInterface):MetadataCache{
        return MetadataCache(dao)
    }
    single { provideMetadataCache(get()) }

    //--- necesita el nombre del servicio del MediaBrowser, el cual se pone como parámetro
    fun provideMusicServiceConnection(application: Application):MusicServiceConnection{
        return MusicServiceConnection(application.baseContext, ComponentName(application.baseContext, MusicService::class.java)
        )
    }

    single { provideMusicServiceConnection(get()) }


    fun provideAppRepository(videoDao: VideoDao,directCache: DirectCache,musicServiceConnection: MusicServiceConnection,queueDao: QueueDao,queueFieldDao: QueueFieldDao,metaCache:MetadataCache):AppRepository {
        return AppRepository(videoDao,directCache,musicServiceConnection,queueDao,queueFieldDao,metaCache)
    }

    single { provideAppRepository(get(),get(),get(),get(),get(),get()) }

}

val viewModule = module {

    viewModel { MainViewModel(get()) }

    viewModel { ConfigViewModel() }

    viewModel { SearchViewModel(get()) }

    viewModel { PlayerViewModel(get()) }

}