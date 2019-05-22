package com.wpam.scanner.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface PortDao {
    @Query("SELECT * FROM port")
    fun getAll(): List<Port>

    @Insert
    fun insertAll(vararg port: Port)

    @Query("DELETE from port WHERE ip = :ip")
    fun deleteByIp(ip: String)

    @Query("DELETE FROM port")
    fun clear()
}
