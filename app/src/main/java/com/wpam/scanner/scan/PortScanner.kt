package com.wpam.scanner.scan

import android.annotation.SuppressLint
import android.content.Context
import com.wpam.scanner.db.AppDatabase
import com.wpam.scanner.db.Port
import java.io.IOException
import java.net.Socket
import kotlin.math.min

@SuppressLint("SetTextI18n")
class PortScanner(
    private val context: Context,
    private val ip: String,
    private val portRange: String,
    private val db: AppDatabase
) : Scanner {
    override val threads: Int = super.threads

    override val type = ScanType.PORTS

    override fun doScan(): Boolean {

        val portRangeArr = portRange.split("-")

        val startPort = Integer.parseInt(portRangeArr[0])
        val endPort = Integer.parseInt(portRangeArr[1])

        val threadPool = ArrayList<Thread>()
        for (port in startPort..endPort) {
            val thread = makePortScanThread(port)
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

    private fun makePortScanThread(port: Int): Thread {
        return Thread {
            try {
                val socket = Socket(ip, port)
                if (socket.isConnected) {
                    socket.close()
                    db.portDao().insertAll(Port(0, port, ip))
                }
            } catch (e: IOException) { }
        }
    }
}