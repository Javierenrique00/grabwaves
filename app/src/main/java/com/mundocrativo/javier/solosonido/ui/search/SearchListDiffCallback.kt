package com.mundocrativo.javier.solosonido.ui.search

import androidx.recyclerview.widget.DiffUtil
import com.mundocrativo.javier.solosonido.model.VideoObj

class SearchListDiffCallback : DiffUtil.ItemCallback<VideoObj>() {

    override fun areContentsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return ((oldItem.url.contentEquals(newItem.url)) and
                (oldItem.esUrlReady==newItem.esUrlReady))
    }

    override fun areItemsTheSame(oldItem: VideoObj, newItem: VideoObj): Boolean {
        return (oldItem.id == newItem.id)
    }

}