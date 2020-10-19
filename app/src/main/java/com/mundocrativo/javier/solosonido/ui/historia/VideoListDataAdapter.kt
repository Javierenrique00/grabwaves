package com.mundocrativo.javier.solosonido.ui.historia

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.util.Log
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
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.soywiz.klock.ISO8601
import com.soywiz.klock.TimeSpan
import kotlinx.android.synthetic.main.video_list_recycler_item.view.*

class VideoListDataAdapter(val context: Context,val event:MutableLiveData<VideoListEvent> = MutableLiveData()) : ListAdapter<VideoObj, VideoListDataAdapter.VideoListViewHolder>(
    VideoListDiffCallback()
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
                    VideoListEvent.OnStartDrag(
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
            holder.deltaTime.text = Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(item.duration*1000.0)))
            //Log.v("msg","traeinfo: Duration:${item.duration}")

            holder.layout.setOnClickListener {
                event.value = VideoListEvent.OnItemClick(position, item)
            }

            //--- pone el color del fondo
            var backColor = R.color.ColorFondoApp
            if(item.esSelected) backColor = R.color.ColorSelectfondo
            holder.layout.setBackgroundColor(context.getColor(backColor))

            //--para preguntar por el info del video
            if(!item.esInfoReady){
                //Log.v("msg","Adapter need info $item.url")
                event.value = VideoListEvent.OnItemGetInfo(position, item)
            }else{
                holder.title.text = item.title

                holder.channel.text = when(item.kindMedia){
                    KIND_URL_VIDEO -> item.channel
                    KIND_URL_PLAYLIST -> "Playlist - ${item.total_items}"
                    else -> item.channel
                }
            }

            //--para preguntar si tiene cargado el thumbnail del video
            if(!item.esUrlReady){
                holder.thumbnail.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_baseline_ondemand_video_24,context.theme))
            }else{
                if(item.thumbnailImg!=null){
                    holder.thumbnail.setImageDrawable(item.thumbnailImg)
                }else{
                    holder.thumbnail.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_baseline_ondemand_video_24,context.theme))
                }
            }

            holder.servStateImg.visibility = View.INVISIBLE

            //--para cargar id de la base de datos en el campo idDbField que es un dummy
            holder.idDbField.text = item.id.toString()

        }
    }

    class VideoListViewHolder(root: View,val event: MutableLiveData<VideoListEvent>) : RecyclerView.ViewHolder(root){
        var viewUrl : TextView = root.urlTt
        var deltaTime : TextView = root.deltaTimeTb
        var layout : ConstraintLayout = root.videoLayout
        var title : TextView = root.titleTt
        var channel : TextView = root.channelTt
        var thumbnail : ImageView = root.videoThumbnail
        var idDbField : TextView = root.idDbField
        val servStateImg : ImageView = root.servState

        fun swipeRight(index:Int){
            event.value = VideoListEvent.OnSwipeRight(index)
        }
    }


}