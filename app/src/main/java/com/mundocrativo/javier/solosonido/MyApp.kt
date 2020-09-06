package com.mundocrativo.javier.solosonido

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        //--inicializacion de Koin
        startKoin {
            androidContext(this@MyApp)
            modules(listOf(
                appModule,
                viewModule
            ))
        }
    }

}