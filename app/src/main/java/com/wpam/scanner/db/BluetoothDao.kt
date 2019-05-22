package com.wpam.scanner.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface BluetoothDao {
    @Query("SELECT * FROM bluetooth")
    fun getAll(): List<Bluetooth>

    @Query("SELECT address FROM bluetooth")
    fun getAllAdresses(): Array<String>

    @Query("SELECT * FROM bluetooth WHERE address=:address")
    fun getBt(address: String): Bluetooth

    @Insert
    fun insertAll(vararg bluetooth: Bluetooth)

    @Query("DELETE FROM bluetooth")
    fun clear()
}
