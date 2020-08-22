package org.chenhome.dailybrainy

import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import timber.log.Timber


@HiltAndroidTest
class DailyBrainyActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Test
    fun happyPath() {
        Timber.d("Starting test")
        ActivityScenario.launch(DailyBrainyActivity::class.java)

        // Check Buttons fragment screen is displayed
        //       onView(withId(R.id.textView)).check(matches(isDisplayed()))
    }
}