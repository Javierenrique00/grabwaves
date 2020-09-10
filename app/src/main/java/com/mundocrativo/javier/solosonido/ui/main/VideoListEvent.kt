package com.mundocrativo.javier.solosonido.ui.main

import com.mundocrativo.javier.solosonido.model.VideoObj

sealed class VideoListEvent {
    data class OnItemClick(val position:Int, val item: VideoObj) : VideoListEvent()
    data class OnItemGetInfo(val position:Int, val item: VideoObj) : VideoListEvent()

}