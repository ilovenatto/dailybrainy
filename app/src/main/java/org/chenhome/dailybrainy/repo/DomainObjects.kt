package org.chenhome.dailybrainy.repo

import org.chenhome.dailybrainy.repo.local.ChallengeDb


/**
 * Game with only enough info to support ViewModels.
 * Other aspects of Game queries separately using {@link BrainyRepo}
 */
data class Game
    (
    val gameId: Long = 0, // database ID if this game already exists in DB
    val pin: String,

    // Mutable in app level
    var currentStep: ChallengeDb.Step,
    var sessionStartMillisEpoch: Long = 0,
    var step2Count: MutableMap<ChallengeDb.Step, Int?> = mutableMapOf(),

    // Immutable
    val challId: Long, // database id of challenge (which should already exist in db
    val challTitle: String,
    val challHmw: String,
    val challHmwDesc: String,
    val challImgFn: String?
) {

    companion object {
        fun genPin(): String {
            val buf = StringBuilder("")
            val range = IntRange(0, 9)
            for (i in 0..3) {
                buf.append(range.random().toString())
            }
            return buf.toString()
        }
    }
}
