package com.mundocrativo.javier.solosonido.ui.player

import androidx.recyclerview.widget.RecyclerView
import com.mundocrativo.javier.solosonido.model.VideoObj

sealed class VideoPlayerListEvent {
    data class OnItemClick(val position:Int, val item: VideoObj) : VideoPlayerListEvent()
    data class OnItemGetInfo(val position:Int, val item: VideoObj) : VideoPlayerListEvent()
    data class OnStartDrag(val viewHolder:RecyclerView.ViewHolder) : VideoPlayerListEvent()
    data class OnSwipeRight(val position:Int) : VideoPlayerListEvent()
    data class OnMoveItem(val from:Int, val to:Int) : VideoPlayerListEvent()

}