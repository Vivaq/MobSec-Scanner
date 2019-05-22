package com.wpam.scanner.scan

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.widget.DrawerLayout
import com.wpam.scanner.db.AppDatabase
import com.wpam.scanner.db.Bluetooth
import kotlinx.android.synthetic.main.activity_main.*

class BluetoothScanner (
    private val context: Context,
    private val db: AppDatabase
) : Scanner {

    private var success = false
    override val type = ScanType.BLUETOOTH
    override fun doScan(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        }
        while (!bluetoothAdapter.isEnabled) { }

        context.registerReceiver(broadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        context.registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        bluetoothAdapter.startDiscovery()

        while (!success) {
            Thread.sleep(1000)
        }
        return true
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action as String
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    var type = ""
                    when(device.type) {
                        BluetoothDevice.DEVICE_TYPE_CLASSIC -> {type = "classic"}
                        BluetoothDevice.DEVICE_TYPE_LE -> {type = "low energy"}
                        BluetoothDevice.DEVICE_TYPE_DUAL -> {type = "dual"}
                    }
                    Thread {
                        db.bluetoothDao().insertAll(Bluetooth(0, device.address, device.name, type))
                    }.start()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    (context as Activity).drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    success = true
                    destroy()
                }
            }
        }
    }

    private fun destroy() {
        context.unregisterReceiver(broadcastReceiver)
    }
}