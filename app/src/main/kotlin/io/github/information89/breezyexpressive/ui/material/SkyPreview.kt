package io.github.information89.breezyexpressive.ui.material

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import io.github.information89.breezyexpressive.insight.Sky

/**
 * Debug builds only: preview the background for any sky, e.g.
 * adb shell am start -n <pkg>/.MainActivity --es preview_sky SUNNY --ez preview_day true
 */
object SkyPreview {
    val override = mutableStateOf<Pair<Sky, Boolean>?>(null)

    fun readFrom(context: Context, intent: Intent?) {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val name = intent?.getStringExtra("preview_sky") ?: return
        if (!debuggable) return
        val sky = Sky.entries.firstOrNull { it.name == name } ?: return
        override.value = sky to intent.getBooleanExtra("preview_day", true)
    }
}
