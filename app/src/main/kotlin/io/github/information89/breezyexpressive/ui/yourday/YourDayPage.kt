package io.github.information89.breezyexpressive.ui.yourday

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.ui.components.PagePlaceholder

@Composable
fun YourDayPage(modifier: Modifier = Modifier) {
    PagePlaceholder(
        title = stringResource(R.string.page_your_day),
        description = stringResource(R.string.placeholder_your_day),
        modifier = modifier,
    )
}
