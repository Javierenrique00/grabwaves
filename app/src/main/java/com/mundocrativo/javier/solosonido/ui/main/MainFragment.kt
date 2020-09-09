package com.mundocrativo.javier.solosonido.ui.main

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mundocrativo.javier.solosonido.BuildConfig
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel by sharedViewModel<MainViewModel>()
    private lateinit var pref : AppPreferences
    private lateinit var videoListDataAdapter: VideoListDataAdapter



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

        view.checkServerBt.setOnClickListener {
            revizaServer(serverTb.text.toString(),"https://www.youtube.com/watch?v=kA9voL0edJU",false,false)
        }

        view.pasteBt.setOnClickListener {
            loadPasteText()
        }

        view.serverTb.addTextChangedListener(tw)

        view.qualitySw.setOnCheckedChangeListener { compoundButton, b ->
            //Log.v("msg","Cambio el estado del switch a:$b")
            pref.hQ = b
            showCalidadSw(b)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        pref = AppPreferences(context!!)
        versionTt.text = "V:${BuildConfig.VERSION_NAME}"

        //--- videoRecyclerView
        setupVideoListRecyclerAdapter()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.submitList(it)
            videoListDataAdapter.notifyDataSetChanged()
        })

        //--- Cuando cambia un item de la lista SELECTION
        viewModel.videoItemChanged.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.notifyItemChanged(it.first,it.second)
        })

    }

    override fun onResume() {
        super.onResume()
        //-- trae las preferencias hacia el display
        serverTb.setText(pref.server)
        checkFormulary()
        qualitySw.isChecked = pref.hQ

        //--- debe cargar los videos que están en la base de datos
        viewModel.loadVideosFromDb()

        val enlace = viewModel.enlaceExternal
        if(enlace==null){
            Log.v("msg","----> No hay link")
        } else{
            Log.v("msg","----> opening link: $enlace")
            revizaServer(serverTb.text.toString(),enlace,pref.hQ,false)
            viewModel.enlaceExternal = null
        }
    }

    fun revizaServer(server:String,videoLetras:String,hQ:Boolean,addDb:Boolean) {
        val videoBase64 = Util.convStringToBase64(videoLetras)
        val quality = if(hQ) "hq" else "lq"
        val ruta = server + "/?link="+videoBase64+"&q=$quality"
        Log.v("msg","Contactando streaming:$ruta")
        if(addDb) viewModel.insertNewVideo(videoLetras)
        launchNavigator(ruta)
        //playMedia(ruta)
    }

    fun launchNavigator(ruta:String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(ruta)
        startActivity(intent)
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


    val tw = object : TextWatcher{
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

    //---https://stackoverflow.com/questions/19177231/android-copy-paste-from-clipboard-manager
    fun loadPasteText(){
        var enlaceLetras = ""
        var clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if(!(clipboard.hasPrimaryClip())){

        } else if(!(clipboard.getPrimaryClipDescription()!!.hasMimeType(MIMETYPE_TEXT_PLAIN))){

        } else {
            val item = clipboard.getPrimaryClip()!!.getItemAt(0)
            enlaceLetras = item.text.toString()
            enlaceTb.setText(enlaceLetras)
        }
        if(!enlaceLetras.isEmpty()) revizaServer(serverTb.text.toString(),enlaceLetras,pref.hQ,true)
    }

    private fun setupVideoListRecyclerAdapter(){
        videoListDataAdapter = VideoListDataAdapter(context!!)
        videoRv.layoutManager = LinearLayoutManager(context)
        videoRv.adapter = videoListDataAdapter
        videoListDataAdapter.event.observe(viewLifecycleOwner, Observer {
            when(it){
                is VideoListEvent.OnItemClick ->{
                    val itemSelected = it.item
                    itemSelected.esSelected = true
                    viewModel.videoItemChanged.postValue(Pair(it.position,itemSelected))
                    revizaServer(serverTb.text.toString(),it.item.url,pref.hQ,false)
                }
            }
        })
    }



    fun playMedia(ruta:String){
        val mediaPlayer: MediaPlayer? = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(ruta)
            prepare() // might take long! (for buffering, etc)
            start()
        }
    }


}