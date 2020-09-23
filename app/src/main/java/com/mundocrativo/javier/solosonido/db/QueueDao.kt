package com.mundocrativo.javier.solosonido.db

import androidx.room.*
import com.mundocrativo.javier.solosonido.model.QueueObj

@Dao
interface QueueDao {

    @Insert(onConflict =OnConflictStrategy.REPLACE)
    fun insert(item:QueueObj):Long

    @Query("SELECT * FROM QueueObj order by queueName")
    fun traeQueues():List<QueueObj>

    @Delete
    fun delete(item: QueueObj)

    @Query("SELECT * FROM QueueObj WHERE queueName=:name")
    fun searchByName(name:String):QueueObj?

}