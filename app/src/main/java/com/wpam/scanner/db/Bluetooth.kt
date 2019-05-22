package com.wpam.scanner.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Bluetooth (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val address: String,
    val name: String,
    val type: String
) {
    companion object {
        val columnsNames = arrayOf("id", "address", "name", "type")
    }
}