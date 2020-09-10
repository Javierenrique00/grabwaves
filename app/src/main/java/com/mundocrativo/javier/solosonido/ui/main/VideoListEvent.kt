package com.mundocrativo.javier.solosonido.ui.main

import androidx.recyclerview.widget.RecyclerView
import com.mundocrativo.javier.solosonido.model.VideoObj

sealed class VideoListEvent {
    data class OnItemClick(val position:Int, val item: VideoObj) : VideoListEvent()
    data class OnItemGetInfo(val position:Int, val item: VideoObj) : VideoListEvent()
    data class OnStartDrag(val viewHolder:RecyclerView.ViewHolder) : VideoListEvent()
    data class OnSwipeRight(val id:Long,val url:String) : VideoListEvent()

}