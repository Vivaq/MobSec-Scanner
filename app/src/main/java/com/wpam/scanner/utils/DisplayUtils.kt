package com.wpam.scanner.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

class DisplayUtils {
    companion object {
        fun prettyInformation(context: Context, msgText: String) {
            this.let {
                val builder = AlertDialog.Builder(context)
                builder.setCancelable(false)

                builder.setTitle(msgText)
                builder.setPositiveButton("OK") { _, _ -> }

                val alertDialog = builder.create()
                alertDialog.show()
            }
        }

        fun toast(context: Context, data: String) {
            Toast.makeText(
                context, data,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}