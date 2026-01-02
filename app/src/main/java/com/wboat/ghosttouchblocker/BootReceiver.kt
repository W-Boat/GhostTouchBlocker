package com.wboat.ghosttouchblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.DataOutputStream

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("auto_start", false)) {
                Thread {
                    try {
                        val process = Runtime.getRuntime().exec("su")
                        val os = DataOutputStream(process.outputStream)
                        os.writeBytes("am start -n ${context.packageName}/.MainActivity\n")
                        os.writeBytes("exit\n")
                        os.flush()
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }
}
