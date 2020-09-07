package com.mundocrativo.javier.solosonido

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.mundocrativo.javier.solosonido.ui.main.MainFragment
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        //--- trae el dato del enlace
        val extras = intent.extras
        val enlace = extras?.getString(Intent.EXTRA_TEXT)
        enlace?.let {
            Log.v("msg","Enlace = $it")

            viewModel.enlaceExternal = it
        }


    }

}