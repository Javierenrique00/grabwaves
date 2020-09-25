package com.mundocrativo.javier.solosonido.ui.search

import com.mundocrativo.javier.solosonido.model.VideoObj

sealed class SearchListEvent {
    data class OnItemClick(val position:Int, val item: VideoObj) : SearchListEvent()
    data class OnItemGetThumbnail(val position:Int, val item: VideoObj) : SearchListEvent()
    data class OnItemLongClick(val position: Int,val item: VideoObj) : SearchListEvent()
}