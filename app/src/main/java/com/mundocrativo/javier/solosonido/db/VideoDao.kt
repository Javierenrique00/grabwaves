package com.mundocrativo.javier.solosonido.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mundocrativo.javier.solosonido.model.VideoObj

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item:VideoObj)

    @Query("SELECT * FROM VideoObj order by timestamp desc")
    fun traeVideos():List<VideoObj>

}