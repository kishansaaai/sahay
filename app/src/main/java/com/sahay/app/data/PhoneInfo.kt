package com.sahay.app.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import com.sahay.engine.JsonOut

/** Public device facts only: no serial, account, advertising ID or learner data. Read locally without extra permissions. */
data class PhoneInfo(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdk: Int,
    val platform: String,
    val cpuAbis: List<String>,
    val kernelManagedRamMiB: Long?,
    val onDeviceSpeechServiceAvailable: Boolean,
) {
    fun toJson(): String = JsonOut.write(mapOf(
        "schema" to "sahay.phone-info/1", "manufacturer" to manufacturer, "model" to model,
        "android_release" to androidRelease, "sdk" to sdk, "platform" to platform, "cpu_abis" to cpuAbis,
        "kernel_managed_ram_mib" to kernelManagedRamMiB,
        "on_device_speech_service_available" to onDeviceSpeechServiceAvailable,
    ))

    companion object {
        fun read(context: Context): PhoneInfo {
            val memory = ActivityManager.MemoryInfo()
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory)
            val soc = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE
            return PhoneInfo(
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                soc.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE,
                Build.SUPPORTED_ABIS.toList(),
                memory.totalMem.takeIf { it > 0 }?.div(1024 * 1024),
                Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context),
            )
        }
    }
}
