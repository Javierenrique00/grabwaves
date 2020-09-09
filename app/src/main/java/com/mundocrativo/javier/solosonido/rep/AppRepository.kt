package com.mundocrativo.javier.solosonido.rep

import com.mundocrativo.javier.solosonido.db.DataDatabase
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.VideoObj


class AppRepository(val videoDao: VideoDao) {

    fun listVideos():List<VideoObj>{
        return videoDao.traeVideos()
    }

    fun insertVideo(item:VideoObj){
        videoDao.insert(item)
    }


}