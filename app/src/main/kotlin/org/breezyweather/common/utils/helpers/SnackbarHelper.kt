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

package org.breezyweather.common.utils.helpers

import android.view.View
import org.breezyweather.BreezyWeather
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.snackbar.Snackbar

object SnackbarHelper {

    /**
     * Solstice: set by the main screen while its shell is showing. Returns true when it took the
     * message (shown in the new message pill); otherwise the old View snackbar is used.
     */
    var shell: ((content: String, action: String?, onAction: (() -> Unit)?) -> Boolean)? = null

    fun showSnackbar(
        content: String,
        action: String? = null,
        activity: BreezyActivity? = null,
        listener: View.OnClickListener? = null,
    ) {
        if (action != null && listener == null) {
            throw RuntimeException("Must send a non null listener as parameter.")
        }
        if (activity == null && shell?.invoke(content, action, listener?.let { { it.onClick(null) } }) == true) {
            return
        }
        val container = (activity ?: BreezyWeather.instance.topActivity ?: return).provideSnackbarContainer()
        Snackbar.make(container.container, content, Snackbar.LENGTH_LONG, container.cardStyle)
            .setAction(action, listener)
            .setCallback(Snackbar.Callback())
            .show()
    }
}
