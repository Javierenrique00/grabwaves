package com.mundocrativo.javier.solosonido

import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

}

val viewModule = module {

    viewModel { MainViewModel() }

    //single { }

}