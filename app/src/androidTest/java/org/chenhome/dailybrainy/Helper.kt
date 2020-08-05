package org.chenhome.dailybrainy

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.chenhome.dailybrainy.repo.local.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Helper function to observe LiveData response
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

val egPlayerGuid = genGuid()
val egChall1 = Challenge(
    genGuid(),
    "adsaf",
    "Getting down and up",
    "HMW do this",
    Challenge.Category.CHALLENGE,
    "asdadsf",
    "asdf"
)
val egChall2 = Challenge(
    genGuid(),
    "adsaf34324",
    "Getting down and up",
    "HMW do this",
    Challenge.Category.CHALLENGE,
    "asdadsf",
    "asdf"
)
val egGame =
    Game(genGuid(), egChall1.guid, egPlayerGuid, "234", 0, Challenge.Step.GEN_IDEA, null, null)

val egPlayer = PlayerSession(genGuid(), egPlayerGuid, egGame.guid, "Sammy", "foobar")
val egIdea =
    Idea(genGuid(), egGame.guid, egPlayerGuid, Idea.Origin.BRAINSTORM, 0, "foobar", "asdfadf")
