package com.mundocrativo.javier.solosonido.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import kotlinx.android.synthetic.main.video_list_recycler_item.view.*

class VideoListDataAdapter(val context: Context,val event:MutableLiveData<VideoListEvent> = MutableLiveData()) : ListAdapter<VideoObj, VideoListDataAdapter.VideoListViewHolder>(VideoListDiffCallback()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListViewHolder(inflater.inflate(R.layout.video_list_recycler_item,parent,false))
    }

    override fun onBindViewHolder(holder: VideoListViewHolder, position: Int) {
        getItem(position).let { item ->
            holder.viewUrl.text = item.url
            holder.deltaTime.text = Util.calcDeltaTiempo(item.timestamp/1000,DateTime.nowUnixLong()/1000)

            holder.layout.setOnClickListener {
                event.value = VideoListEvent.OnItemClick(position,item)
            }

            //--- pone el color del fondo
            var backColor = R.color.ColorFondoApp
            if(item.esSelected) backColor = R.color.ColorResaltado
            holder.layout.setBackgroundColor(context.getColor(backColor))

            //--para preguntar por el info del video
            if(!item.esInfoReady){
                event.value = VideoListEvent.OnItemGetInfo(position,item)
            }else{
                holder.title.text = item.title
                holder.channel.text = item.channel
                //holder.thumbnail.load(item.thumbnailUrl)
            }

            //--para preguntar si tiene cargado el thumbnail del video
            if(!item.esUrlReady){
                holder.thumbnail.setImageDrawable(context.resources.getDrawable(R.drawable.ic_baseline_ondemand_video_24))
            }else{
                holder.thumbnail.setImageDrawable(item.thumbnailImg)
            }




        }
    }


    class VideoListViewHolder(root: View) : RecyclerView.ViewHolder(root){
        var viewUrl : TextView = root.urlTt
        var deltaTime : TextView = root.deltaTimeTb
        var layout : ConstraintLayout = root.videoLayout
        var title : TextView = root.titleTt
        var channel : TextView = root.channelTt
        var thumbnail : ImageView = root.videoThumbnail
    }


}