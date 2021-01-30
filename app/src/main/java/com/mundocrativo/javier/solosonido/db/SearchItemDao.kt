package com.mundocrativo.javier.solosonido.db

import androidx.room.*
import com.mundocrativo.javier.solosonido.model.SearchItem

@Dao
interface SearchItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item:SearchItem):Long

    @Query("SELECT * FROM searchitem WHERE busqueda LIKE '%' || :inputStr || '%' LIMIT 6")
    fun buscar(inputStr:String):List<SearchItem>

    @Query("SELECT * FROM searchitem WHERE busqueda=:inputStr")
    fun buscarExact(inputStr:String):List<SearchItem>

    @Query("SELECT * FROM searchitem ORDER BY id ASC")
    fun listaTodos():List<SearchItem>

    @Delete
    fun delete(item:SearchItem)

}