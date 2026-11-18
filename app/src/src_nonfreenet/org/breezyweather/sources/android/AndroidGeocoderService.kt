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

package org.breezyweather.sources.android

import android.content.Context
import android.location.Geocoder
import breezyweather.domain.location.model.LocationAddressInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.rx3.rxObservable
import org.breezyweather.common.extensions.currentLocale
import javax.inject.Inject

class AndroidGeocoderService @Inject constructor() : AndroidGeocoderServiceStub() {

    override fun requestNearestLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): Observable<List<LocationAddressInfo>> {
        val geocoder = Geocoder(context, context.currentLocale)
        return rxObservable {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            val locationList = mutableListOf<LocationAddressInfo>()
            addresses?.getOrNull(0)?.let {
                locationList.add(
                    LocationAddressInfo(
                        city = it.locality,
                        district = it.subLocality,
                        admin1 = it.adminArea,
                        // Solstice: some geocoders leave the borough out (e.g. Mexico City's alcaldías);
                        // it's the part of the address line just before the postcode and city.
                        admin2 = it.subAdminArea ?: boroughFromAddressLine(it),
                        country = it.countryName,
                        countryCode = it.countryCode
                    )
                )
            }
            send(locationList)
        }
    }
}

/**
 * Solstice: the borough from a full address line like "Calle 1, San Miguel Chapultepec I Secc,
 * Miguel Hidalgo, 11850 Ciudad de México, CDMX, Mexico": the part right before the one that
 * holds the postcode or the city, when it isn't the neighbourhood itself.
 */
private fun boroughFromAddressLine(address: android.location.Address): String? {
    val line = address.getAddressLine(0) ?: return null
    val parts = line.split(",").map { it.trim() }
    val anchor = parts.indexOfFirst { part ->
        (address.postalCode != null && part.contains(address.postalCode!!)) ||
            (address.locality != null && part.startsWith(address.locality!!))
    }
    if (anchor <= 0) return null
    val candidate = parts[anchor - 1]
    return candidate.takeIf {
        it.isNotEmpty() && it != address.subLocality && it != address.locality &&
            it.none { c -> c.isDigit() }
    }
}
