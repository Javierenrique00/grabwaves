package com.mundocrativo.javier.solosonido.ui.historia

import androidx.recyclerview.widget.DiffUtil
import com.mundocrativo.javier.solosonido.model.VideoObj

class VideoListDiffCallback : DiffUtil.ItemCallback<VideoObj>() {

    override fun areContentsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return ((oldItem.title.contentEquals(newItem.title)) and
                (oldItem.esInfoReady==newItem.esInfoReady) and
                (oldItem.esUrlReady==newItem.esUrlReady) and
                (oldItem.esSelected==newItem.esSelected))
    }

    override fun areItemsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return ((oldItem.id == newItem.id)  and (oldItem.url.contentEquals(newItem.url)))
    }

}