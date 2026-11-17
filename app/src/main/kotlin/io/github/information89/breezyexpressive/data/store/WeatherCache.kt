package io.github.information89.breezyexpressive.data.store

import io.github.information89.breezyexpressive.data.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Last known weather per location, as one small JSON file each.
 * Shown instantly on launch and when offline. A corrupt or outdated file is simply ignored.
 */
class WeatherCache(private val dir: File, private val json: Json) {

    suspend fun read(locationId: String): Weather? = withContext(Dispatchers.IO) {
        val file = fileFor(locationId)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<Weather>(file.readText()) }.getOrNull()
    }

    suspend fun write(locationId: String, weather: Weather) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val target = fileFor(locationId)
        // Write to a temp file first so a crash never leaves a half-written cache.
        val temp = File(dir, "${target.name}.tmp")
        temp.writeText(json.encodeToString(Weather.serializer(), weather))
        if (!temp.renameTo(target)) {
            target.delete()
            temp.renameTo(target)
        }
    }

    suspend fun delete(locationId: String) = withContext(Dispatchers.IO) {
        fileFor(locationId).delete()
    }

    private fun fileFor(locationId: String): File {
        val safe = locationId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(dir, "$safe.json")
    }
}
