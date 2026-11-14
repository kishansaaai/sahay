package com.sahay.app.llm

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.sahay.engine.GovMode
import com.sahay.engine.GovSignals
import com.sahay.engine.Governor

/**
 * Reads thermal status, battery level, charging state and free memory every [periodMs] and feeds the [Governor] (issue A13).
 * Every mode change is written to logcat (tag SahayGovernor) and kept in [Governor.log] for the debug panel; [onMode] drives the status chip.
 */
class DeviceMonitor(private val context: Context, private val governor: Governor, private val periodMs: Long = 5_000) {
    private val handler = Handler(Looper.getMainLooper())
    private var seen = 0
    var onMode: ((GovMode, String) -> Unit)? = null
    @Volatile var last: GovSignals? = null; private set

    private val tick = object : Runnable {
        override fun run() { sample(); handler.postDelayed(this, periodMs) }
    }

    fun start() { handler.removeCallbacks(tick); handler.post(tick) }
    fun stop() = handler.removeCallbacks(tick)

    fun sample() {
        val s = read()
        last = s
        governor.update(s, SystemClock.elapsedRealtime())
        while (seen < governor.log.size) {
            val e = governor.log[seen++]
            Log.i("SahayGovernor", "${e.from} -> ${e.to}: ${e.reason} (thermal=${s.thermal} battery=${s.batteryPct}% charging=${s.charging} freeMem=${s.freeMemMb}MB)")
            onMode?.invoke(e.to, e.reason)
        }
    }

    fun read(): GovSignals {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 29) pm.currentThermalStatus else 0
        val b = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val mi = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return GovSignals(thermal, if (level < 0) 100 else level * 100 / scale, charging, (mi.availMem / (1024 * 1024)).toInt())
    }
}
