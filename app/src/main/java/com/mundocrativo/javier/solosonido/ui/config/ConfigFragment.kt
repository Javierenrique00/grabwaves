package com.mundocrativo.javier.solosonido.ui.config

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import kotlinx.android.synthetic.main.config_fragment.*
import kotlinx.android.synthetic.main.config_fragment.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConfigFragment : Fragment() {

    companion object {
        fun newInstance() =
            ConfigFragment()
    }

    private val viewModel by sharedViewModel<ConfigViewModel>()
    private lateinit var pref : AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =  inflater.inflate(R.layout.config_fragment, container, false)

        view.checkServerBt.setOnClickListener {
            //revizaServer(serverTb.text.toString(),"https://www.youtube.com/watch?v=kA9voL0edJU",false,false)
            val urlToPlay = Util.createUrlConnectionStringPlay(serverTb.text.toString(),"https://www.youtube.com/watch?v=kA9voL0edJU",false)
            launchNavigator(urlToPlay)
        }

        view.serverTb.addTextChangedListener(tw)

        view.qualitySw.setOnCheckedChangeListener { compoundButton, b ->
            //Log.v("msg","Cambio el estado del switch a:$b")
            pref.hQ = b
            showCalidadSw(b)
        }

        //--setup the action bar !  Para el menu
        (activity as AppCompatActivity).setSupportActionBar(view.app_bar_back)
        view.app_bar_back.setNavigationOnClickListener {
            activity!!.finish()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(ConfigViewModel::class.java)

        pref = AppPreferences(context!!)

    }

    override fun onResume() {
        super.onResume()
        //-- trae las preferencias hacia el display
        serverTb.setText(pref.server)
        checkFormulary()
        qualitySw.isChecked = pref.hQ

    }

    fun showCalidadSw(estado:Boolean){
        qualitySw.text = if(estado) getString(R.string.swTextHq) else getString(R.string.swTextLq)
    }

    fun checkFormulary():Boolean{
        val isOk = checkAddress()
        checkServerBt.isEnabled = isOk
        return isOk
    }

    fun checkAddress():Boolean{
        val text = serverTb.text.toString()
        return !text.isEmpty()
    }

    val tw = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            pref.server = serverTb.text.toString()
            checkFormulary()
            Log.v("msg","textBox=${pref.server}")
        }

    }

    fun launchNavigator(ruta:String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(ruta)
        startActivity(intent)
    }

}