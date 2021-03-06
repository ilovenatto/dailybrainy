package org.chenhome.dailybrainy.repo

import android.net.Uri
import com.google.firebase.database.Exclude
import org.chenhome.dailybrainy.repo.image.AvatarImage
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
    val youtubeId: String?, // only set for Lesson category. This is the youtube ID
    var imageUri: Uri?, // Permanent URI where this image lives. Set by the local observer and not persisted in reomte db.

) {
    // No-arg constructor so that Firebase can create this POJO
    constructor() : this(
        "", "", "", "",
        Category.CHALLENGE, null, null, null
    )
    enum class Category {
        LESSON,// lesson, where there's generally an associated youtube video
        CHALLENGE
    }

    /**
     * Steps in each challenge, broken down by Phase
     */
    enum class Step(
        val titleRsc: Int,
        val allowedSecs: Long,
    ) {

        // Brainstorm
        GEN_IDEA(
            org.chenhome.dailybrainy.R.string.genidea,
            TimeUnit.MINUTES.toSeconds(3)
        ),
        VOTE_IDEA(
            org.chenhome.dailybrainy.R.string.voteidea,
            TimeUnit.MINUTES.toSeconds(3)
        ),

        // Sketch
        GEN_SKETCH(
            org.chenhome.dailybrainy.R.string.gensketch,
            TimeUnit.MINUTES.toSeconds(5),
        ),

        VOTE_SKETCH(
            org.chenhome.dailybrainy.R.string.votesketch,
            TimeUnit.MINUTES.toSeconds(2)
        ),

        // Share
        CREATE_STORYBOARD(
            org.chenhome.dailybrainy.R.string.createstoryboard,
            TimeUnit.MINUTES.toSeconds(10)
        ),
        VIEW_STORYBOARD(
            org.chenhome.dailybrainy.R.string.review_story,
            TimeUnit.MINUTES.toSeconds(10)
        );

        // Whether current step less than other step (in ordinal value)
        fun less(other: Step): Boolean = this.ordinal < other.ordinal

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
    var storyDesc: String?,

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
    var guid: String,
    var userGuid: String, // There is one application user per device.
    var gameGuid: String,
    var name: String,
    var imgFn: String?,
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

    @Exclude
    fun avatarImage(): AvatarImage {
        if (!imgFn.isNullOrEmpty()) {
            return AvatarImage.valueOf(imgFn!!)
        }
        return AvatarImage.A1 // just return default avatar
    }

    @Exclude
    fun isValid(): Boolean = guid.isNotEmpty()
            && userGuid.isNotEmpty()
            && gameGuid.isNotEmpty()
            && name.isNotEmpty()
            && imgFn?.isNotEmpty() ?: false

}

data class Idea(
    var guid: String, // can be modified

    // Foreign key to its parent, Game
    val gameGuid: String,

    // Unique user that originated this idea. Helpful
    // for counting points per player
    val playerGuid: String,
    var playerName: String?, // set on insertion

    // Maps to unique name [Challenge.Phase]
    val origin: Origin,

    // defaults to 0
    var votes: Int = 0,
    var title: String?,
    var imgFn: String?,
    var imgUri: String?,

    ) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(
        "", "", "", "",
        Origin.BRAINSTORM, 0, null, null, null
    )

    // Convenience constructor for an empty object that has all its required fields set
    constructor(guid: String, gameGuid: String, playerGuid: String, origin: Origin) : this(
        guid = guid,
        gameGuid = gameGuid,
        playerGuid = playerGuid,
        playerName = "Unknown",
        origin = origin,
        votes = 0,
        title = null,
        imgFn = null,
        imgUri = null
    )

    fun vote() {
        votes += 1
    }

    // Which part of the game that the idea originated
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