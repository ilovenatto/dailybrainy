package org.chenhome.dailybrainy.repo.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

interface CanValidate {
    fun canInsert(): Boolean
    fun canUpdate(): Boolean
}

/**
 * Globally unique id that won't collide w/ other ids. Used in remote database as identifier
 */
fun genGuid(): String {
    return java.util.UUID.randomUUID().toString()
}

@Entity
/**
 * Static set of challenges.
 */
@JsonClass(generateAdapter = true)
data class ChallengeDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    // Globally unique identifier
    val guid: String,

    // Not required
    // absolute local filename for the challenge's image
    val imgFn: String?,
    val title: String,
    val hmw: String,
    val hmwDesc: String
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, "", null, "", "", "")

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


@Entity(
    foreignKeys = [ForeignKey(
        entity = ChallengeDb::class,
        parentColumns = ["id"],
        childColumns = ["challengeId"],
        onDelete = CASCADE
    )]
)
data class GameDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    // use {@link EntitHelper.genGuid()}
    val guid: String,

    // Foreign key to parent Challenge
    val challengeId: Long,

    // Must be generated and set at insertion time
    val pin: String,

    // Session start time in Millis since epoch.
    // can be update
    var sessionStartMillisEpoch: Long = 0,

    var currentStep: ChallengeDb.Step = ChallengeDb.Step.GEN_IDEA

) : CanValidate {
    override fun canInsert(): kotlin.Boolean = challengeId > 0L
    override fun canUpdate(): Boolean = id > 0L && challengeId > 0L

    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, "", 0, "", 0, ChallengeDb.Step.GEN_IDEA)
}

/**
 * @param imgFn full filepath name to PlayerDb's avatar img
 */
@Entity(
    foreignKeys = [ForeignKey(
        entity = GameDb::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = CASCADE
    )]
)
data class PlayerDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    // Globally Unique key for the player
    val guid: String,

    // Foreign key to GameDb table
    val gameId: Long,

    val name: String,
    val points: Int,
    val imgFn: String?
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, "", 0, "", 0, "")
}


/**
 * Each Game associated with a final Storyboard
 * shared by the players
 * @param title title of the solution
 * @param descrip Description of the solution
 * @param imgSettingFn full filename path to sketch illustrating the solutions' setting
 * @param imgSolutionFn full filename path to sketch illustrating the solution
 * @param imgResolutionFn full filename path to sketch illustrating the solutions' resolution
 */
@Entity(
    foreignKeys = [ForeignKey(
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = CASCADE,
        entity = GameDb::class
    )]
)
data class StoryboardDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    // Foreign key
    val gameId: Long,

    val title: String?,
    val descrip: String?,
    val imgSettingFn: String?,
    val imgSolutionFn: String?,
    val imgResolutionFn: String?
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, 0, null, "", "", "", "")
}

@Entity(
    foreignKeys = [ForeignKey(
        entity = GameDb::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = CASCADE
    )]
)
data class IdeaDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    val title: String,
    val imgFn: String,

    // defaults to 0
    var votes: Int = 0,

    // Foreign key to its parent, Game
    val gameId: Long,

    // Maps to unique name {@link ChallengeDb.Phase}
    val phase: ChallengeDb.Phase
) : CanValidate {
    override fun canInsert(): Boolean = gameId > 0L
            && (title.isNotEmpty() || imgFn.isNotEmpty())

    override fun canUpdate(): Boolean = canInsert()
    fun vote() {
        votes += 1
    }

    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, "", "", 0, 0, ChallengeDb.Phase.BRAINSTORM)
}

@Entity
data class LessonDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    val guid: String,
    val title: String,
    val descrip: String,
    val youtubeUrl: String,
    val imgFn: String
) {
    // No-arg constructur so that Firebase can create this POJO
    constructor() : this(0, "", "", "", "", "")
}
