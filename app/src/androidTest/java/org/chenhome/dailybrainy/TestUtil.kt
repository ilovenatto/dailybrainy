package org.chenhome.dailybrainy

import androidx.lifecycle.*
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