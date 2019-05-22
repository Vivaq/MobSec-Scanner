package com.wpam.scanner.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [Host::class, Port::class, Bluetooth::class, Wifi::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun portDao(): PortDao
    abstract fun bluetoothDao(): BluetoothDao
    abstract fun wifiDao(): WifiDao
}
