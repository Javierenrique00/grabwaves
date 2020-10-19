package com.mundocrativo.javier.solosonido.ui.search

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.android.synthetic.main.video_list_recycler_item.view.*

class VideoSearchDataAdapter(val context:Context,val event:MutableLiveData<SearchListEvent> = MutableLiveData()) : ListAdapter<VideoObj, VideoSearchDataAdapter.SearchItemViewHolder>(SearchListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = SearchItemViewHolder(inflater.inflate(R.layout.video_list_recycler_item,parent,false))
        return viewHolder
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        getItem(position).let {item ->
            holder.title.text = item.title
            holder.viewUrl.text = item.url
            holder.channel.text = item.channel
            holder.deltaTime.text = item.durationStr

            //--para preguntar si tiene cargado el thumbnail del video
            if(!item.esUrlReady){
                //holder.thumbnail.setImageDrawable(context.resources.getDrawable(R.drawable.ic_baseline_ondemand_video_24))
                holder.thumbnail.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_baseline_ondemand_video_24,context.theme))
                event.value = SearchListEvent.OnItemGetThumbnail(position,item)
            }else{
                holder.thumbnail.setImageDrawable(item.thumbnailImg)
            }

            holder.layout.setOnClickListener {
                event.value = SearchListEvent.OnItemClick(position,item)
            }

            holder.layout.setOnLongClickListener {
                event.value = SearchListEvent.OnItemLongClick(position,item)
                true
            }

            //--- pone el color del fondo
            var backColor = R.color.ColorFondoApp
            if(item.esSelected) backColor = R.color.ColorSelectfondo
            holder.layout.setBackgroundColor(context.getColor(backColor))

            holder.servStateImg.visibility = View.GONE

        }

    }



    class SearchItemViewHolder(root: View) : RecyclerView.ViewHolder(root){
        var viewUrl : TextView = root.urlTt
        var deltaTime : TextView = root.deltaTimeTb
        var layout : ConstraintLayout = root.videoLayout
        var title : TextView = root.titleTt
        var channel : TextView = root.channelTt
        var thumbnail : ImageView = root.videoThumbnail
        var idDbField : TextView = root.idDbField
        val servStateImg : ImageView = root.servState
    }

}