package com.wpam.scanner.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface WifiDao {
    @Query("SELECT * FROM wifi")
    fun getAll(): List<Wifi>

    @Query("SELECT ssid FROM wifi")
    fun getAllSSID(): Array<String>

    @Query("SELECT * FROM wifi WHERE ssid=:ssid")
    fun getWifi(ssid: String): Wifi

    @Insert
    fun insertAll(vararg wifi: Wifi)

    @Query("DELETE FROM wifi")
    fun clear()
}
