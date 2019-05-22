package com.wpam.scanner.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Wifi (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val ssid: String,
    val bssid: String,
    val security: String
) {
    companion object {
        val columnsNames = arrayOf("id", "ssid", "bssid", "security")
    }
}