package io.github.information89.breezyexpressive.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing for the two interactions the background redesign has to keep smooth:
 * scrolling the Forecast page and swiping between pages.
 *
 * Run on a connected phone:
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * Results (frameDurationCpuMs, frameOverrunMs: P50/P90/P95/P99) are printed and written as JSON
 * under baselineprofile/build/outputs/connected_android_test_additional_output/.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun forecastScroll() = measure { scrollForecast() }

    @Test
    fun pageSwipe() = measure { swipePages() }

    private fun measure(block: MacrobenchmarkScope.() -> Unit) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(), // uses the app's baseline profile, like an install from the store
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            Thread.sleep(4_000) // let the weather load and the Forecast page render
        },
        measureBlock = block,
    )

    private fun MacrobenchmarkScope.scrollForecast() {
        val w = device.displayWidth
        val h = device.displayHeight
        repeat(2) {
            device.swipe(w / 2, h * 3 / 4, w / 2, h / 4, 20)
            device.waitForIdle()
        }
        repeat(2) {
            device.swipe(w / 2, h / 4, w / 2, h * 3 / 4, 20)
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.swipePages() {
        val w = device.displayWidth
        val h = device.displayHeight
        repeat(2) {
            device.swipe(w * 9 / 10, h / 2, w / 10, h / 2, 15)
            Thread.sleep(800)
        }
        repeat(2) {
            device.swipe(w / 10, h / 2, w * 9 / 10, h / 2, 15)
            Thread.sleep(800)
        }
    }

    private companion object {
        const val PACKAGE = "io.github.information89.breezyexpressive"
    }
}
