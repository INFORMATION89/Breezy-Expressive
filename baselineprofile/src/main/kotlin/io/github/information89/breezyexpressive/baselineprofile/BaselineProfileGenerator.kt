package io.github.information89.breezyexpressive.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code used on startup and the most common interactions (scrolling the forecast,
 * swiping between pages) so it's compiled ahead of time on install.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "io.github.information89.breezyexpressive") {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        Thread.sleep(4_000) // let the weather load and the Forecast page render

        val w = device.displayWidth
        val h = device.displayHeight
        // Scroll the forecast down and up.
        device.swipe(w / 2, h * 3 / 4, w / 2, h / 4, 20)
        device.waitForIdle()
        device.swipe(w / 2, h / 4, w / 2, h * 3 / 4, 20)
        device.waitForIdle()
        // Swipe to Your day and Locations, then back.
        repeat(2) {
            device.swipe(w * 9 / 10, h / 2, w / 10, h / 2, 15)
            Thread.sleep(800)
        }
        repeat(2) {
            device.swipe(w / 10, h / 2, w * 9 / 10, h / 2, 15)
            Thread.sleep(800)
        }
    }
}
