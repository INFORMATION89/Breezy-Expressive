package io.github.information89.breezyexpressive.ui.locations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.ui.components.PagePlaceholder

@Composable
fun LocationsPage(modifier: Modifier = Modifier) {
    PagePlaceholder(
        title = stringResource(R.string.page_locations),
        description = stringResource(R.string.placeholder_locations),
        modifier = modifier,
    )
}
