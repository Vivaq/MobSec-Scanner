package com.wpam.scanner.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Port (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val port: Int,
    val ip: String
) {
    companion object {
        val columnsNames = arrayOf("id", "port", "ip")
    }
}