package com.wpam.scanner.utils

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

class NetUtils {
    companion object {
        fun convertCIDRtoIpRange(ipAddrWithMask: String): Pair<Long, Long> {
            val (ipAddr, strSubnet) = ipAddrWithMask.split("/")
            val subnetLength = Integer.parseInt(strSubnet)

            var hexIp = ""
            for (octet in ipAddr.split(".")) {
                hexIp += Integer.toHexString(Integer.parseInt(octet))
            }

            val mask = 0xffffffff shl (32 - subnetLength)
            val broadcast = 0xffffffff shr subnetLength

            val netAddr = (hexIp.toLong(16) and mask)
            val startIp = netAddr + 1
            val endIp = netAddr + (broadcast - 1)

            return Pair(startIp, endIp)
        }

        fun getNetworksAddresses(): ArrayList<String> {
            val allIps = ArrayList<String>()
            try {
                val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (enumNetworkInterfaces.hasMoreElements()) {
                    val networkInterface = enumNetworkInterfaces.nextElement()

                    val ifList = networkInterface.interfaceAddresses
                    for (ifAddress in ifList) {
                        val ipAddress = ifAddress.address
                        if (!ipAddress.isLoopbackAddress and isIpv4(ipAddress.hostAddress)) {
                            allIps.add(ifAddress.toString().split(" ")[0].substring(1))
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return allIps
        }

        fun isIpv4(ipAddress: String): Boolean {
            var counter = 0
            for (ipOctet in ipAddress.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                try {
                    if (Integer.valueOf(ipOctet) in 0..255) {
                        if (++counter == 4) {
                            return true
                        }
                    }
                } catch (e: NumberFormatException) {
                    break
                }
            }
            return false
        }
    }
}