package io.github.information89.breezyexpressive

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.information89.breezyexpressive.data.store.Appearance
import io.github.information89.breezyexpressive.ui.navigation.AppRoot
import io.github.information89.breezyexpressive.ui.theme.BreezyTheme
import io.github.information89.breezyexpressive.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appearanceStore = appContainer.appearanceStore
        setContent {
            val appearance by appearanceStore.appearance.collectAsStateWithLifecycle(initialValue = Appearance())
            val scope = rememberCoroutineScope()
            val dark = appearance.isDarkTheme()

            // Keep system bar icons readable when the in-app theme differs from the system theme.
            LaunchedEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            BreezyTheme(appearance = appearance) {
                AppRoot(
                    appearance = appearance,
                    onAppearanceChange = { transform -> scope.launch { appearanceStore.update(transform) } },
                )
            }
        }
    }
}
