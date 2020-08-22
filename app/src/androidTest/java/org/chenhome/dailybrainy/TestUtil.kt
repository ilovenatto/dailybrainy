package org.chenhome.dailybrainy

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


// Lifecycle fixture
class TestLifecycleOwner : LifecycleOwner {
    val reg = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = reg
}


/**
 * Helper function to observe LiveData response. Used
 * for unit tests.
 */
fun <T> LiveData<T>.blockingObserve(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)

    val observer = Observer<T> { t ->
        value = t
        latch.countDown()
    }

    observeForever(observer)

    latch.await(2, TimeUnit.SECONDS)
    return value
}

// A custom runner to set up the instrumented application class for tests.
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        Timber.plant(Timber.DebugTree())
        Timber.d("Starting CustomTestRunner")
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}