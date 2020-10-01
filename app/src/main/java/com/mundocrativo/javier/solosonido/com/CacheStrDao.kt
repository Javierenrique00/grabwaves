package com.mundocrativo.javier.solosonido.com

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mundocrativo.javier.solosonido.model.CacheMetaStr
import com.mundocrativo.javier.solosonido.model.CacheStr

@Dao
interface CacheStrDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item:CacheStr)

    @Query("SELECT * FROM CacheStr where id=:key")
    fun traeCache(key:Long):CacheStr?

    @Query("SELECT id,timestamp FROM CacheStr order by timestamp")
    fun listCache():List<CacheStr>

    @Query("DELETE FROM CacheStr where id=:key")
    fun deleteCacheId(key:Long)
}

@Dao
interface CacheMetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item:CacheMetaStr)

    @Query("SELECT * FROM CacheMetaStr where id=:key")
    fun traeCache(key:Long):CacheMetaStr?

    @Query("SELECT id,timestamp FROM CacheMetaStr order by timestamp")
    fun listCache():List<CacheMetaStr>

    @Query("DELETE FROM CacheMetaStr where id=:key")
    fun deleteCacheId(key:Long)
}