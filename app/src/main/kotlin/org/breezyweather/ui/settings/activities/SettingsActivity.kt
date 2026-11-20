/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.settings.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceFeature
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.bus.EventBus
import org.breezyweather.common.extensions.hasPermission
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.common.utils.helpers.PermissionHelper
import org.breezyweather.domain.settings.SettingsChangedMessage
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.sources.RefreshHelper
import org.breezyweather.sources.SourceManager
import org.breezyweather.ui.expressive.components.ScreenPush
import org.breezyweather.ui.settings.compose.AppearanceSettingsScreen
import org.breezyweather.ui.settings.compose.BackgroundSettingsScreen
import org.breezyweather.ui.settings.compose.DebugSettingsScreen
import org.breezyweather.ui.settings.compose.LocationSettingsScreen
import org.breezyweather.ui.settings.compose.MainScreenSettingsScreen
import org.breezyweather.ui.settings.compose.ModulesSettingsScreen
import org.breezyweather.ui.settings.compose.NotificationsSettingsScreen
import org.breezyweather.ui.settings.compose.RootSettingsView
import org.breezyweather.ui.settings.compose.SettingsScreenRouter
import org.breezyweather.ui.settings.compose.UnitSettingsScreen
import org.breezyweather.ui.settings.compose.WeatherSourcesSettingsScreen
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import javax.inject.Inject

private const val PERMISSION_CODE_POST_NOTIFICATION = 0

@AndroidEntryPoint
class SettingsActivity : BreezyActivity() {

    @Inject lateinit var sourceManager: SourceManager

    @Inject lateinit var refreshHelper: RefreshHelper

    companion object {
        const val KEY_SETTINGS_ACTIVITY_START_DESTINATION =
            "SETTINGS_ACTIVITY_START_DESTINATION"
    }

    private val updateIntervalState = mutableStateOf(
        SettingsManager.getInstance(this).updateInterval
    )
    private val darkModeState = mutableStateOf(
        SettingsManager.getInstance(this).darkMode
    )
    private val cardDisplayState = mutableStateOf(
        SettingsManager.getInstance(this).cardDisplayList
    )
    private val dailyTrendDisplayState = mutableStateOf(
        SettingsManager.getInstance(this).dailyTrendDisplayList
    )
    private val hourlyTrendDisplayState = mutableStateOf(
        SettingsManager.getInstance(this).hourlyTrendDisplayList
    )
    private val notificationEnabledState = mutableStateOf(
        SettingsManager.getInstance(this).isWidgetNotificationEnabled
    )
    private val todayForecastEnabledState = mutableStateOf(
        SettingsManager.getInstance(this).isTodayForecastEnabled
    )
    private val tomorrowForecastEnabledState = mutableStateOf(
        SettingsManager.getInstance(this).isTomorrowForecastEnabled
    )

    private var requestPostNotificationPermissionSucceedCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BreezyWeatherTheme {
                ContentView()
            }
        }

        EventBus.instance.with(SettingsChangedMessage::class.java).observeAutoRemove(this) {
            val darkMode = SettingsManager.getInstance(this).darkMode
            if (darkModeState.value != darkMode) {
                darkModeState.value = darkMode
            }

            val updateInterval = SettingsManager.getInstance(this).updateInterval
            if (updateIntervalState.value != updateInterval) {
                updateIntervalState.value = updateInterval
            }

            val cardDisplayList = SettingsManager.getInstance(this).cardDisplayList
            if (cardDisplayState.value != cardDisplayList) {
                cardDisplayState.value = cardDisplayList
            }

            val dailyTrendDisplayList = SettingsManager.getInstance(this).dailyTrendDisplayList
            if (dailyTrendDisplayState.value != dailyTrendDisplayList) {
                dailyTrendDisplayState.value = dailyTrendDisplayList
            }

            val hourlyTrendDisplayList = SettingsManager.getInstance(this).hourlyTrendDisplayList
            if (hourlyTrendDisplayState.value != hourlyTrendDisplayList) {
                hourlyTrendDisplayState.value = hourlyTrendDisplayList
            }

            val notificationEnabled = SettingsManager.getInstance(this).isWidgetNotificationEnabled
            if (notificationEnabledState.value != notificationEnabled) {
                notificationEnabledState.value = notificationEnabled
            }

            val todayForecastEnabled = SettingsManager.getInstance(this).isTodayForecastEnabled
            if (todayForecastEnabledState.value != todayForecastEnabled) {
                todayForecastEnabledState.value = todayForecastEnabled
            }

            val tomorrowForecastEnabled = SettingsManager.getInstance(this).isTomorrowForecastEnabled
            if (tomorrowForecastEnabledState.value != tomorrowForecastEnabled) {
                tomorrowForecastEnabledState.value = tomorrowForecastEnabled
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE_POST_NOTIFICATION) {
            if (grantResults.count { it == PackageManager.PERMISSION_GRANTED } == grantResults.size) {
                // all granted.
                requestPostNotificationPermissionSucceedCallback?.let { it() }
                requestPostNotificationPermissionSucceedCallback = null
            }
            return
        }
    }

    @Composable
    private fun ContentView() {
        val scope = rememberCoroutineScope()
        val onBack = { onBackPressedDispatcher.onBackPressed() }
        val startDestination = intent.getStringExtra(KEY_SETTINGS_ACTIVITY_START_DESTINATION)
            ?: SettingsScreenRouter.Root.route
        // Solstice: Navigation 3 (N1, 2026-11-20). The back stack is ours: a list of the same route
        // strings upstream used, kept across configuration changes and process death.
        val backStack = rememberSaveable(saver = routeStackSaver) { mutableStateListOf(startDestination) }

        val notificationPermissionState = rememberMultiplePermissionsState(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // permission not needed
                emptyList()
            }
        )
        val hasNotificationPermission = notificationPermissionState.permissions.isEmpty() ||
            notificationPermissionState.permissions[0].status == PermissionStatus.Granted

        // Disable notification settings when notification permission is not granted. This prevents jobs from trying to
        // still post a notification.
        LaunchedEffect(hasNotificationPermission) {
            if (notificationPermissionState.permissions.isNotEmpty()) {
                if (notificationPermissionState.permissions[0].status != PermissionStatus.Granted) {
                    SettingsManager.getInstance(this@SettingsActivity).apply {
                        if (isTodayForecastEnabled) isTodayForecastEnabled = false
                        if (isTomorrowForecastEnabled) isTomorrowForecastEnabled = false
                    }
                } else {
                    // Ensure the notification widget is shown again when the notification permission is granted.
                    scope.launch { refreshHelper.updateNotificationIfNecessary(this@SettingsActivity) }
                }
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else finish() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            // Solstice: iOS-style push instead of the default cross-fade, also for predictive back.
            transitionSpec = { ScreenPush.push() },
            popTransitionSpec = { ScreenPush.pop() },
            predictivePopTransitionSpec = { ScreenPush.pop() }
        ) { route ->
            NavEntry(route) {
                SettingsPage(route, onNavigate = { backStack.add(it) }, onBack, hasNotificationPermission)
            }
        }
    }

    @Composable
    private fun SettingsPage(
        route: String,
        onNavigate: (String) -> Unit,
        onBack: () -> Unit,
        hasNotificationPermission: Boolean,
    ) {
        val scope = rememberCoroutineScope()
        // Keeps upstream's calls (navController.navigate(route)) as they were.
        val navController = object {
            fun navigate(next: String) = onNavigate(next)
        }
        when (route) {
            SettingsScreenRouter.Root.route -> {
                RootSettingsView(
                    onNavigateTo = { navController.navigate(it) },
                    onNavigateBack = { onBack() }
                )
            }
            SettingsScreenRouter.BackgroundUpdates.route -> {
                BackgroundSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    updateInterval = remember { updateIntervalState }.value
                )
            }
            SettingsScreenRouter.Appearance.route -> {
                AppearanceSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateTo = { navController.navigate(it) },
                    onNavigateBack = { onBack() },
                    darkMode = remember { darkModeState }.value
                )
            }
            SettingsScreenRouter.Unit.route -> {
                UnitSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    updateWidgetIfNecessary = { context: Context ->
                        scope.launch {
                            refreshHelper.updateWidgetIfNecessary(context)
                        }
                    },
                    updateNotificationIfNecessary = { context: Context ->
                        scope.launch {
                            refreshHelper.updateNotificationIfNecessary(context)
                        }
                    }
                )
            }
            SettingsScreenRouter.MainScreen.route -> {
                MainScreenSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    cardDisplayList = remember { cardDisplayState }.value.toImmutableList(),
                    dailyTrendDisplayList = remember { dailyTrendDisplayState }.value.toImmutableList(),
                    hourlyTrendDisplayList = remember { hourlyTrendDisplayState }.value.toImmutableList(),
                    updateWidgetIfNecessary = { context: Context ->
                        scope.launch {
                            refreshHelper.updateWidgetIfNecessary(context)
                        }
                    }
                )
            }
            SettingsScreenRouter.Notifications.route -> {
                NotificationsSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    hasNotificationPermission = hasNotificationPermission,
                    postNotificationPermissionEnsurer = { postNotificationPermission(it) },
                    todayForecastEnabled = remember { todayForecastEnabledState }.value,
                    tomorrowForecastEnabled = remember { tomorrowForecastEnabledState }.value
                )
            }
            SettingsScreenRouter.Widgets.route -> {
                ModulesSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    hasNotificationPermission = hasNotificationPermission,
                    notificationEnabled = remember { notificationEnabledState }.value,
                    postNotificationPermissionEnsurer = { postNotificationPermission(it) },
                    updateWidgetIfNecessary = { context: Context ->
                        scope.launch {
                            refreshHelper.updateWidgetIfNecessary(context)
                        }
                    },
                    updateNotificationIfNecessary = { context: Context ->
                        scope.launch {
                            refreshHelper.updateNotificationIfNecessary(context)
                        }
                    },
                    broadcastDataIfNecessary = { context: Context, sourceId: String ->
                        scope.launch {
                            refreshHelper.broadcastDataIfNecessary(context, sourceId)
                        }
                    },
                    broadcastSources = sourceManager.getBroadcastSources()
                )
            }
            SettingsScreenRouter.Location.route -> {
                LocationSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    locationSources = sourceManager.getLocationSources()
                )
            }
            SettingsScreenRouter.WeatherProviders.route -> {
                WeatherSourcesSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    worldwideSources = sourceManager
                        .getSupportedFeatureSources(SourceFeature.FORECAST, Location()),
                    configurableSources = sourceManager.getConfigurableSources()
                )
            }
            SettingsScreenRouter.Debug.route -> {
                DebugSettingsScreen(
                    context = this@SettingsActivity,
                    onNavigateBack = { onBack() },
                    hasNotificationPermission = hasNotificationPermission,
                    postNotificationPermissionEnsurer = { postNotificationPermission(it) }
                )
            }
            else -> Unit
        }
    }

    /** Saves the route back stack as a plain list of strings. */
    private val routeStackSaver = listSaver<SnapshotStateList<String>, String>(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )

    private fun postNotificationPermission(
        succeedCallback: () -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !this.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            requestPostNotificationPermissionSucceedCallback = succeedCallback

            PermissionHelper.requestPermissionWithFallback(
                activity = this,
                permission = Manifest.permission.POST_NOTIFICATIONS,
                requestCode = PERMISSION_CODE_POST_NOTIFICATION,
                fallback = { IntentHelper.startNotificationSettingsActivity(this) }
            )
        } else {
            succeedCallback()
        }
    }

    /*@Preview
    @Composable
    private fun DefaultPreview() {
        BreezyWeatherTheme {
            ContentView()
        }
    }*/
}
