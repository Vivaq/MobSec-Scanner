package com.wpam.scanner.scan

interface Scanner {
    val type: ScanType
    val threads get() = 100
    fun doScan(): Boolean
}