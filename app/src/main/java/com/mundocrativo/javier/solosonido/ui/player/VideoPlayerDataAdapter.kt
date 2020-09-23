package com.mundocrativo.javier.solosonido.ui.player

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.soywiz.klock.ISO8601
import com.soywiz.klock.TimeSpan
import kotlinx.android.synthetic.main.video_list_recycler_item.view.*

class VideoPlayerDataAdapter(val context: Context, val event:MutableLiveData<VideoPlayerListEvent> = MutableLiveData()) : ListAdapter<VideoObj, VideoPlayerDataAdapter.VideoListViewHolder>(
    VideoPlayerDiffCallback()
){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder =
            VideoListViewHolder(
                inflater.inflate(R.layout.video_list_recycler_item, parent, false),
                event
            )

        //--- detecta el touch sobre el icono para iniciar el scroll
        viewHolder.itemView.videoThumbnail.setOnTouchListener { view, motionEvent ->
            if(motionEvent.actionMasked == MotionEvent.ACTION_DOWN){
                event.value =
                    VideoPlayerListEvent.OnStartDrag(
                        viewHolder
                    )
            }
            return@setOnTouchListener true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: VideoListViewHolder, position: Int) {
        getItem(position).let { item ->
            holder.viewUrl.text = item.url
            //holder.deltaTime.text = Util.calcDeltaTiempo(item.timestamp/1000,DateTime.nowUnixLong()/1000)
            holder.deltaTime.text = Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(item.duration*1000.0)))

            holder.layout.setOnClickListener {
                event.value =
                    VideoPlayerListEvent.OnItemClick(
                        position,
                        item
                    )
            }

            //--para detectar el click
            holder.layout.setOnClickListener {
                event.value = VideoPlayerListEvent.OnItemClick(position,item)
            }

            //--- pone el color del fondo
            var backColor = R.color.ColorFondoApp
            if(item.esSelected) backColor = R.color.ColorSelectfondo
            holder.layout.setBackgroundColor(context.getColor(backColor))

            //--- Cambia el fondo del que está tocando
            if(item.esPlaying) backColor = R.color.ColorFondoResaltado
            holder.layout.setBackgroundColor(context.getColor(backColor))


            //--para preguntar por el info del video
            if(!item.esInfoReady){
                //Log.v("msg","info not set position=$position")
                event.value =
                    VideoPlayerListEvent.OnItemGetInfo(
                        position,
                        item
                    )
            }else{
                holder.title.text = item.title
                holder.channel.text = item.channel
                //holder.thumbnail.load(item.thumbnailUrl)
            }

            //--para preguntar si tiene cargado el thumbnail del video
            if(!item.esUrlReady){
                //holder.thumbnail.setImageDrawable(context.resources.getDrawable(R.drawable.ic_baseline_ondemand_video_24))
                holder.thumbnail.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_baseline_ondemand_video_24,context.theme))
            }else{
                holder.thumbnail.setImageDrawable(item.thumbnailImg)
            }

            //--para cargar la posición de la lista para ser borrada
            holder.index.text = position.toString()

        }
    }

    fun moveItem(from:Int,to:Int){
        event.value = VideoPlayerListEvent.OnMoveItem(from,to)
    }

    class VideoListViewHolder(root: View,val event: MutableLiveData<VideoPlayerListEvent>) : RecyclerView.ViewHolder(root){
        var viewUrl : TextView = root.urlTt
        var deltaTime : TextView = root.deltaTimeTb
        var layout : ConstraintLayout = root.videoLayout
        var title : TextView = root.titleTt
        var channel : TextView = root.channelTt
        var thumbnail : ImageView = root.videoThumbnail
        var index : TextView = root.idDbField

        fun swipeRight(index:Int){
            event.value = VideoPlayerListEvent.OnSwipeRight(index)
        }
    }


}