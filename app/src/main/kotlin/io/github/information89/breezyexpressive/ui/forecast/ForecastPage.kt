package io.github.information89.breezyexpressive.ui.forecast

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.ui.components.PagePlaceholder
import io.github.information89.breezyexpressive.ui.theme.AppType

@Composable
fun ForecastPage(modifier: Modifier = Modifier) {
    PagePlaceholder(
        title = stringResource(R.string.page_forecast),
        description = stringResource(R.string.placeholder_forecast),
        modifier = modifier,
    ) {
        // Type specimen only: shows the two voices until real data arrives in M2.
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.placeholder_sample_temperature),
            style = AppType.voices.heroTemperature,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.placeholder_sample_summary),
            style = AppType.voices.narrativeLarge,
        )
    }
}
