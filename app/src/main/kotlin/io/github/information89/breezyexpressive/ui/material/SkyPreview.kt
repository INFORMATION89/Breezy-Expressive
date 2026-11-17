package io.github.information89.breezyexpressive.ui.material

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.mutableStateOf
import io.github.information89.breezyexpressive.insight.Sky

/**
 * Debug builds only: preview the background for any sky and time of day, from the Debug section
 * in Settings or with adb:
 * adb shell am start -n <pkg>/.MainActivity --es preview_sky SUNNY --ez preview_day true
 * Each override is independent; null = follow the live weather.
 */
object SkyPreview {
    val sky = mutableStateOf<Sky?>(null)
    val isDay = mutableStateOf<Boolean?>(null)
    /** Feeds made-up rain (starting in ~50 min, peaking heavy) into the precipitation timeline. */
    val rain = mutableStateOf(false)

    fun isAvailable(context: Context) = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun readFrom(context: Context, intent: Intent?) {
        if (intent == null || !isAvailable(context)) return
        intent.getStringExtra("preview_sky")?.let { name -> Sky.entries.firstOrNull { it.name == name }?.let { sky.value = it } }
        if (intent.hasExtra("preview_day")) isDay.value = intent.getBooleanExtra("preview_day", true)
    }
}
