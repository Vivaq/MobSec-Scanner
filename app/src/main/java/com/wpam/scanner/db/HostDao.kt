package com.wpam.scanner.db

import android.arch.persistence.room.*

@Dao
interface HostDao {
    @Query("SELECT * FROM host")
    fun getAll(): List<Host>

    @Query("SELECT ip FROM host")
    fun getAllIps(): List<String>

    @Insert
    fun insertAll(vararg hosts: Host)

    @Query("DELETE from host WHERE network = :network")
    fun deleteByNet(network: String)

    @Query("DELETE FROM host")
    fun clear()
}
