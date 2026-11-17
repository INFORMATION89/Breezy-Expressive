package io.github.information89.breezyexpressive

import android.app.Application
import android.content.Context
import io.github.information89.breezyexpressive.data.repository.WeatherRepository
import io.github.information89.breezyexpressive.data.source.Network
import io.github.information89.breezyexpressive.data.source.openmeteo.OpenMeteoSource
import io.github.information89.breezyexpressive.data.store.AppearanceStore
import io.github.information89.breezyexpressive.data.store.WeatherCache
import java.io.File

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container: the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val appearanceStore = AppearanceStore(context.applicationContext)

    private val httpClient by lazy { Network.client() }

    val weatherRepository by lazy {
        WeatherRepository(
            sources = listOf(OpenMeteoSource(httpClient, Network.json)),
            cache = WeatherCache(File(context.applicationContext.filesDir, "weather"), Network.json),
        )
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as App).container
