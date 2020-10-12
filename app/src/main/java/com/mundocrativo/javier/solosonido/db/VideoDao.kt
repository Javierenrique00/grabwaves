package com.mundocrativo.javier.solosonido.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mundocrativo.javier.solosonido.model.VideoObj

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item:VideoObj):Long

    @Query("SELECT * FROM VideoObj order by timestamp desc")
    fun traeVideos():List<VideoObj>

    @Query("DELETE FROM VideoObj WHERE id=:key")
    fun delete(key:Long)

}