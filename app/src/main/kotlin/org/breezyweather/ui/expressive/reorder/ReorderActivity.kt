/*
 * This file is part of Solstice, a fork of Breezy Weather.
 *
 * Solstice is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Solstice is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Solstice. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.expressive.reorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.breezyweather.R
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.bus.EventBus
import org.breezyweather.common.options.BaseEnum
import org.breezyweather.common.options.appearance.CardDisplay
import org.breezyweather.common.options.appearance.DailyTrendDisplay
import org.breezyweather.common.options.appearance.HourlyTrendDisplay
import org.breezyweather.domain.settings.SettingsChangedMessage
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.common.widgets.generateCollapsedScrollBehavior
import org.breezyweather.ui.common.widgets.insets.FitStatusBarTopAppBar
import org.breezyweather.ui.expressive.components.SolsticeIcons
import org.breezyweather.ui.expressive.theme.Space
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme

/**
 * Compose replacement for upstream's three reorder screens (main-screen blocks, daily trends,
 * hourly trends). Same stored settings; saved when the screen is left, as upstream does.
 */
class ReorderActivity : BreezyActivity() {

    private lateinit var spec: Spec<out BaseEnum>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spec = when (intent.getStringExtra(EXTRA_KIND)) {
            KIND_DAILY -> Spec(
                titleId = R.string.settings_main_daily_trends_title,
                all = DailyTrendDisplay.entries,
                current = SettingsManager.getInstance(this).dailyTrendDisplayList,
                defaults = DailyTrendDisplay.toDailyTrendDisplayList(SettingsManager.DEFAULT_DAILY_TREND_DISPLAY),
                save = { s, list -> s.dailyTrendDisplayList = list },
                postChange = false
            )
            KIND_HOURLY -> Spec(
                titleId = R.string.settings_main_hourly_trends_title,
                all = HourlyTrendDisplay.entries,
                current = SettingsManager.getInstance(this).hourlyTrendDisplayList,
                defaults = HourlyTrendDisplay.toHourlyTrendDisplayList(SettingsManager.DEFAULT_HOURLY_TREND_DISPLAY),
                save = { s, list -> s.hourlyTrendDisplayList = list },
                postChange = false
            )
            else -> Spec(
                titleId = R.string.settings_main_cards_title,
                all = CardDisplay.entries,
                current = SettingsManager.getInstance(this).cardDisplayList,
                defaults = CardDisplay.toCardDisplayList(SettingsManager.DEFAULT_CARD_DISPLAY),
                save = { s, list -> s.cardDisplayList = list },
                postChange = true,
                configure = { it.configure }
            )
        }
        setContent {
            BreezyWeatherTheme {
                ReorderScreen(spec, onBack = { finish() })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (spec.saveIfChanged(this)) {
            // Main-screen blocks: tell the main screen to reload (upstream does this manually too).
            EventBus.instance.with(SettingsChangedMessage::class.java).postValue(SettingsChangedMessage())
        }
    }

    companion object {
        private const val EXTRA_KIND = "kind"
        const val KIND_CARDS = "cards"
        const val KIND_DAILY = "daily"
        const val KIND_HOURLY = "hourly"

        fun start(context: Context, kind: String) {
            context.startActivity(Intent(context, ReorderActivity::class.java).putExtra(EXTRA_KIND, kind))
        }
    }
}

private class Spec<T : BaseEnum>(
    @StringRes val titleId: Int,
    val all: List<T>,
    current: List<T>,
    val defaults: List<T>,
    private val save: (SettingsManager, List<T>) -> Unit,
    private val postChange: Boolean,
    val configure: (T) -> ((Activity) -> Unit)? = { null },
) {
    private val initial = current.toList()
    val shown = mutableStateListOf<T>().apply { addAll(current) }
    val hidden get() = all.filter { it !in shown }

    fun reset() {
        shown.clear()
        shown.addAll(defaults)
    }

    /** Saves when the order changed; returns true when the main screen must be told. */
    fun saveIfChanged(context: Context): Boolean {
        if (shown.toList() == initial) return false
        save(SettingsManager.getInstance(context), shown.toList())
        return postChange
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : BaseEnum> ReorderScreen(spec: Spec<T>, onBack: () -> Unit) {
    val scrollBehavior = generateCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FitStatusBarTopAppBar(
                title = stringResource(spec.titleId),
                onBackPressed = onBack,
                actions = {
                    TextButton(onClick = { spec.reset() }) {
                        Text(stringResource(R.string.action_reset_to_default))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val context = androidx.compose.ui.platform.LocalContext.current
        ReorderList(
            items = spec.shown,
            key = { it.id },
            label = { it.getName(context) },
            onRemove = { spec.shown.remove(it) },
            trailing = { item ->
                spec.configure(item)?.let { configure ->
                    IconButton(onClick = { (context as? Activity)?.let(configure) }) {
                        Icon(SolsticeIcons.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                }
            },
            contentPadding = PaddingValues(
                start = Space.S4,
                end = Space.S4,
                top = padding.calculateTopPadding() + Space.S2,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Space.S4
            ),
            footer = {
                val hidden = spec.hidden
                if (hidden.isNotEmpty()) {
                    item(key = "hidden") {
                        Text(
                            text = stringResource(R.string.reorder_hidden_expressive),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = Space.S2,
                                top = Space.S6,
                                bottom = Space.S2
                            ).animateItem()
                        )
                    }
                    item(key = "hiddenChips") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.S2),
                            modifier = Modifier.fillMaxWidth().animateItem()
                        ) {
                            hidden.forEach { item ->
                                AssistChip(
                                    onClick = { spec.shown.add(item) },
                                    label = { Text(item.getName(context)) },
                                    leadingIcon = { Icon(SolsticeIcons.Add, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
