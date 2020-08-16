package org.chenhome.dailybrainy.repo

import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Data objects that align exactly with how the data is
 * stored in the remote database, Firebase.
 */

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
data class Challenge(
    val guid: String,
    val imgFn: String,
    val title: String,
    val desc: String,
    val category: Category,
    val hmw: String?, // only set for Challenge category
    val youtubeUrl: String? // only set for Lesson category

) {
    // No-arg constructor so that Firebase can create this POJO
    constructor() : this(
        "", "", "", "",
        Category.CHALLENGE, null, null
    )

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


data class Game(
    // Firebase Guid. If set, it means that this entity exists in the remote db.
    // Used to identify entities that have not yet been inserted in the remote db
    val guid: String,

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
    constructor() : this(
        "", "", "", "", 0L,
        Challenge.Step.GEN_IDEA, null, null
    )

    // Convenience constructor for an empty object that has all its required fields set
    constructor(guid: String, challengeGuid: String, playerGuid: String) : this(
        guid = guid,
        challengeGuid = challengeGuid,
        playerGuid = playerGuid,
        pin = genPin(),
        sessionStartMillis = 0L,
        currentStep = Challenge.Step.GEN_IDEA,
        storyTitle = null,
        storyDesc = null
    )
}


data class PlayerSession(
    val guid: String,
    val userGuid: String, // There is one application user per device.
    val gameGuid: String,
    val name: String,
    var imgFn: String?
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this("", "", "", "", null)

    // Convenience constructor for an empty object that has all its required fields set
    constructor(guid: String, userGuid: String, gameGuid: String, name: String) : this(
        guid = guid,
        userGuid = userGuid,
        gameGuid = gameGuid,
        name = name,
        imgFn = null
    )

}

data class Idea(
    val guid: String,

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
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(
        "", "", "",
        Origin.BRAINSTORM, 0, null, null
    )

    // Convenience constructor for an empty object that has all its required fields set
    constructor(guid: String, gameGuid: String, playerGuid: String, origin: Origin) : this(
        guid = guid,
        gameGuid = gameGuid,
        playerGuid = playerGuid,
        origin = origin,
        votes = 0,
        title = null,
        imgFn = null
    )

    fun vote() {
        votes += 1
    }

    // Which part of the game that the idea orginated
    enum class Origin {
        BRAINSTORM,
        SKETCH,
        STORY_SETTING,
        STORY_SOLUTION,
        STORY_RESOLUTION
    }
}

enum class DbFolder(val path: String) {
    GAMES("games"),
    CHALLENGES("challenges"),
    PLAYERSESSION("playersessions"),
    IDEAS("ideas")
}