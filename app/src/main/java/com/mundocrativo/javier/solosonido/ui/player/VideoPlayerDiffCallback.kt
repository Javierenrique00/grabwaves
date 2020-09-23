package com.mundocrativo.javier.solosonido.ui.player

import androidx.recyclerview.widget.DiffUtil
import com.mundocrativo.javier.solosonido.model.VideoObj

class VideoPlayerDiffCallback : DiffUtil.ItemCallback<VideoObj>() {

    override fun areContentsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return ((oldItem.title.contentEquals(newItem.title)) and
                (oldItem.esInfoReady==newItem.esInfoReady) and
                (oldItem.esUrlReady==newItem.esUrlReady) and
                (oldItem.esSelected==newItem.esSelected) and
                (oldItem.esPlaying==newItem.esPlaying))
    }

    override fun areItemsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return ( oldItem.url.contentEquals(newItem.url))
    }

}