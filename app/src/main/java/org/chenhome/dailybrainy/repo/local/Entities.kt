package org.chenhome.dailybrainy.repo.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Globally unique id that won't collide w/ other ids. Used in remote database as identifier
 */
val secureRandom = SecureRandom()
val encoder: Base64.Encoder = Base64.getUrlEncoder()
fun genGuid(): String {
    val bytes = ByteArray(6)
    secureRandom.nextBytes(bytes)
    return encoder.encodeToString(bytes)
}

/**
 * Generate numeric pin as secret to get into a game
 */
fun genPin(): String {
    val buf = StringBuilder("")
    val range = IntRange(0, 9)
    for (i in 0..3) {
        buf.append(range.random().toString())
    }
    return buf.toString()
}


/**
 * Static set of challenges.
 */
@Entity
data class Challenge(
    // Globally unique identifier
    @PrimaryKey
    val guid: String,

    val imgFn: String,
    val title: String,
    val desc: String,
    val category: Category,

    val hmw: String?, // only set for Challenge category
    val youtubeUrl: String? // only set for Lesson category

) {
    // No-arg constructor so that Firebase can create this POJO
    constructor() : this("", "", "", "", Category.CHALLENGE, null, null)

    enum class Category {
        LESSON,// lesson, where there's generally an associated youtube video
        CHALLENGE
    }

    /**
     * The phases in each challenge, in ordinal order
     */
    enum class Phase {
        BRAINSTORM,
        SKETCH,
        SHARE
    }

    enum class CountType {
        NUM_VOTES,
        NUM_IDEAS,
        NUM_POPULAR,
        NONE
    }

    /**
     * Steps in each challenge, broken down by Phase
     */
    enum class Step(
        val titleRsc: Int,
        val phase: Phase,
        val allowedSecs: Long,
        val countType: CountType
    ) {
        // Brainstorm
        GEN_IDEA(
            org.chenhome.dailybrainy.R.string.genidea,
            Phase.BRAINSTORM,
            TimeUnit.MINUTES.toSeconds(3),
            CountType.NUM_IDEAS
        ),
        VOTE_IDEA(
            org.chenhome.dailybrainy.R.string.voteidea,
            Phase.BRAINSTORM,
            TimeUnit.MINUTES.toSeconds(3),
            CountType.NUM_VOTES
        ),
        REVIEW_IDEA(
            org.chenhome.dailybrainy.R.string.reviewidea,
            Phase.BRAINSTORM,
            TimeUnit.MINUTES.toSeconds(1),
            CountType.NUM_POPULAR
        ),

        // Sketch
        GEN_SKETCH(
            org.chenhome.dailybrainy.R.string.gensketch,
            Phase.SKETCH,
            TimeUnit.MINUTES.toSeconds(5),
            CountType.NUM_IDEAS
        ),

        VOTE_SKETCH(
            org.chenhome.dailybrainy.R.string.votesketch,
            Phase.SKETCH,
            TimeUnit.MINUTES.toSeconds(2),
            CountType.NUM_VOTES
        ),

        REVIEW_SKETCH(
            org.chenhome.dailybrainy.R.string.reviewsketch,
            Phase.SKETCH,
            TimeUnit.MINUTES.toSeconds(2),
            CountType.NUM_POPULAR
        ),

        // Share
        CREATE_STORYBOARD(
            org.chenhome.dailybrainy.R.string.createstoryboard,
            Phase.SHARE,
            TimeUnit.MINUTES.toSeconds(10),
            CountType.NONE
        ),
        VIEW_STORYBOARD(
            org.chenhome.dailybrainy.R.string.viewstoryboard,
            Phase.SHARE,
            TimeUnit.MINUTES.toSeconds(10),
            CountType.NONE
        )
    }
}


@Entity
data class Game(
    @PrimaryKey
    val guid: String,

    // Firebase Guid. If set, it means that this entity exists in the remote db.
    // Used to identify entities that have not yet been inserted in the remote db
    val fireGuid: String?,

    // Foreign key to parent Challenge
    val challengeGuid: String,

    // Identifying the player that started this game
    val playerGuid: String,

    // Must be generated and set at insertion time
    val pin: String,

    // Session start time in Millis since epoch.
    // can be updated
    var sessionStartMillis: Long?,
    var currentStep: Challenge.Step = Challenge.Step.GEN_IDEA,

    var storyTitle: String?,
    var storyDesc: String?

) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this("", null, "", "", "", null, Challenge.Step.GEN_IDEA, null, null)
}

/**
 * @param imgFn full filepath name to PlayerSession's avatar img
 */
@Entity
data class PlayerSession(
    @PrimaryKey
    val guid: String,

    // Firebase Guid. If set, it means that this entity exists in the remote db.
    // Used to identify entities that have not yet been inserted in the remote db
    val fireGuid: String?,

    val playerGuid: String, // There is one player per device.
    val gameGuid: String,
    val name: String,
    val imgFn: String
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this("", null, "", "", "", "")
}

@Entity
data class Idea(
    @PrimaryKey
    val guid: String,

    // Firebase Guid. If set, it means that this entity exists in the remote db.
    // Used to identify entities that have not yet been inserted in the remote db
    var fireGuid: String?,

    // Foreign key to its parent, Game
    val gameGuid: String,

    // Unique user that originated this idea. Helpful
    // for counting points per player
    val playerGuid: String,

    // Maps to unique name [Challenge.Phase]
    val origin: Origin,

    // defaults to 0
    var votes: Int = 0,
    var title: String?,
    var imgFn: String?


) {
    fun vote() {
        votes += 1
    }

    // No-arg constructur so that Firebase can create this POJO
    constructor() : this("", null, "", "", Origin.BRAINSTORM, 0, null, null)

    // Which part of the game that the idea orginated
    enum class Origin {
        BRAINSTORM,
        SKETCH,
        STORY_SETTING,
        STORY_SOLUTION,
        STORY_RESOLUTION
    }
}