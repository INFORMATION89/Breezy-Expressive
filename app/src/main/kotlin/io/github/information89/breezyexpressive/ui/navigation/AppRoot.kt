package io.github.information89.breezyexpressive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.information89.breezyexpressive.data.store.Appearance
import io.github.information89.breezyexpressive.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object MainKey : NavKey

@Serializable
data object SettingsKey : NavKey

/**
 * Top-level navigation. The three swipeable pages live inside [MainPager];
 * Navigation 3 only handles screens that open on top of them (Settings, later location details).
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
                MainPager(onOpenSettings = { backStack.add(SettingsKey) })
            }
            entry<SettingsKey> {
                SettingsScreen(
                    appearance = appearance,
                    onAppearanceChange = onAppearanceChange,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
