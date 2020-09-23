package com.mundocrativo.javier.solosonido.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mundocrativo.javier.solosonido.model.QueueField
import com.mundocrativo.javier.solosonido.model.VideoObj

@Dao
interface QueueFieldDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: QueueField)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(itemList: List<QueueField>)

    @Query("SELECT * FROM QueueField where queueId=:index order by `order` ASC")
    fun traeQueueItems(index:Long):List<QueueField>

    @Query("DELETE FROM QueueField WHERE queueId=:queueIdToDelete")
    fun deleteFromQueue(queueIdToDelete:Long)


}