package com.wpam.scanner.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.wpam.scanner.db.AppDatabase
import com.wpam.scanner.db.Wifi
import com.wpam.scanner.utils.DisplayUtils
import android.location.LocationManager
import org.jetbrains.anko.runOnUiThread


class WifiScanner(
    private val context: Context,
    private val db: AppDatabase
) : Scanner {
    override val type = ScanType.WIFI

    override fun doScan(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!(networkEnabled and gpsEnabled and wifiManager.isWifiEnabled)) {
            context.runOnUiThread {
                DisplayUtils.toast(context, "Wifi and GPS must be enabled.")
            }
            return false
        }

        var success = false
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                for (result in wifiManager.scanResults) {
                    Thread {
                        db.wifiDao().insertAll(Wifi(0, result.SSID, result.BSSID, result.capabilities))
                    }.start()
                }
                success = true
            }
        }
        context.registerReceiver(broadcastReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        wifiManager.startScan()
        while (!success) {
            Thread.sleep(1000)
        }
        context.unregisterReceiver(broadcastReceiver)
        return true
    }
}