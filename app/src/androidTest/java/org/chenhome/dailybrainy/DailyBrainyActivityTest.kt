package org.chenhome.dailybrainy

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DailyBrainyActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Test
    fun happyPath() {
        ActivityScenario.launch(DailyBrainyActivity::class.java)

        // Check Buttons fragment screen is displayed
        //   onView(withId(R.id.textView)).check(matches(isDisplayed()))
    }
}