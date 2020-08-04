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

val egChall1 =
    ChallengeDb(0, "adsaf", "Getting down and up", "HMW do this", "HMW do this and that", "asdadsf")
val egGame = GameDb(0, genGuid(), 0, "1234", System.currentTimeMillis())
val egPlayer = PlayerDb(
    id = 0,
    gameId = 0,
    name = "p1",
    points = 100,
    imgFn = "file://foobar",
    guid = "sadfadf"
)
val egStory = StoryboardDb(0,0,"3 little pigs", "an awseoms tory", "asdfasf", "asdfadsf","asdfadsf")
val egIdea = IdeaDb(0, "asdfadsf", "asdfasdf", 0, 0, ChallengeDb.Phase.BRAINSTORM)
val egLesson = LessonDb(0, genGuid(), "asdfadsf", "asdfadsf", "asdfadsf", "asdfadf")
