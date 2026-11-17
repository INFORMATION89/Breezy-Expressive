package io.github.information89.breezyexpressive

import android.app.Application
import android.content.Context
import io.github.information89.breezyexpressive.data.store.AppearanceStore

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
}

val Context.appContainer: AppContainer
    get() = (applicationContext as App).container
