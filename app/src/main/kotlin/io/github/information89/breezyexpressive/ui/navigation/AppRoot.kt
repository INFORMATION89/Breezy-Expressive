package io.github.information89.breezyexpressive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.information89.breezyexpressive.data.store.Appearance
import kotlinx.serialization.Serializable

@Serializable
data object MainKey : NavKey

/**
 * Top-level navigation. The swipeable pages (Settings included) live inside [MainPager];
 * Navigation 3 only handles screens that open on top of them (later: location details).
 */
@Composable
fun AppRoot(
    appearance: Appearance,
    onAppearanceChange: ((Appearance) -> Appearance) -> Unit,
) {
    val backStack = rememberNavBackStack(MainKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MainKey> {
                MainPager(appearance = appearance, onAppearanceChange = onAppearanceChange)
            }
        },
    )
}
