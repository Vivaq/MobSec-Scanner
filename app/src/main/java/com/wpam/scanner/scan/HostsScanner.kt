package com.wpam.scanner.scan

import android.content.Context
import com.wpam.scanner.db.AppDatabase
import com.wpam.scanner.db.Host
import com.wpam.scanner.utils.NetUtils
import kotlin.math.min

class HostsScanner(
    private val context: Context,
    private val ipAddrWithMask: String,
    private val db: AppDatabase,
    _threads: Int?
) : Scanner {

    override var threads: Int = if (_threads is Int) _threads else super.threads

    override val type = ScanType.HOSTS
    private val startIp: Long
    private val endIp: Long

    init {
        val (ip1, ip2) = NetUtils.convertCIDRtoIpRange(ipAddrWithMask)
        startIp = ip1
        endIp = ip2
    }

    override fun doScan(): Boolean {
        val threadPool = ArrayList<Thread>()
        for (ip in startIp..endIp) {
            val thread = makeIpScanThread(ip.toInt())
            threadPool.add(thread)
        }
        for (i in 0 until min(threads, threadPool.size)) {
            threadPool[i].start()
        }
        while (true) {
            val toRemove = ArrayList<Int>()
            val availableThreads = min(threads, threadPool.size)
            var currIndex = availableThreads
            for (i in 0 until availableThreads) {
                if(!threadPool[i].isAlive) {
                    toRemove.add(i)
                    if (currIndex < threadPool.size) {
                        threadPool[currIndex++].start()
                    }
                }
            }
            toRemove.reverse()
            for (i in toRemove) {
                threadPool.removeAt(i)
            }
            if (threadPool.isEmpty()) break
        }
        return true
    }

    private fun makeIpScanThread(currIntIp: Int): Thread {
        return Thread {
            val bytes = Integer.toHexString(currIntIp).padStart(8, '0').windowed(2, 2)
            val octetList = ArrayList<String> ()
            for (octet in bytes) {
                octetList.add(octet.toLong(16).toString())
            }
            val currIp = octetList.joinToString(".")

            val isHostUp = pingHost(currIp)
            if (isHostUp) {
                db.hostDao().insertAll(Host(0, ipAddrWithMask, currIp))
            }
        }
    }

    private fun pingHost(ip: String): Boolean {
        val process = Runtime.getRuntime().exec("ping -c3 -q $ip")
        val output = StringBuilder()
        java.util.Scanner(process.inputStream).use {
            while (it.hasNextLine())
                output.append(it.nextLine() + "\n")
        }
        return "100% packet loss" !in output
    }
}