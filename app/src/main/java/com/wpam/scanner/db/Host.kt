package com.wpam.scanner.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Host (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val network: String,
    val ip: String
) {
    companion object {
        val columnsNames = arrayOf("id", "network", "ip")
    }
}