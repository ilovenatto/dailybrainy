package org.chenhome.dailybrainy.repo

import android.content.Context
import org.chenhome.dailybrainy.repo.local.BrainyDb
import org.chenhome.dailybrainy.repo.local.ChallengeDb
import org.chenhome.dailybrainy.repo.local.GameDb

/**
 * Maps between Database objects and Domain objects
 */
class DomainObjectMapper (
    val db: BrainyDb,
    val context: Context
) {

    /**
     * @return Game instance mapped from GameDb
     */
    fun toGame(game: GameDb, challenge:ChallengeDb) : Game {
        var to = Game(
            gameId = game.id,
            pin = game.pin,
            sessionStartMillisEpoch = game.sessionStartMillisEpoch,
            currentStep = game.currentStep,
            challId = challenge.id,
            challTitle = challenge.title,
            challHmw = challenge.hmw,
            challHmwDesc = challenge.hmwDesc,
            challImgFn = challenge.imgFn
        )
        val ideas = db.ideaDAO.getAll(to.gameId)
        ChallengeDb.Step.values().forEach {step->
            var count :Int? = null
            when (step.countType) {
                ChallengeDb.CountType.NUM_IDEAS ->
                    count = ideas.count { it.phase== step.phase}
                ChallengeDb.CountType.NUM_POPULAR ->
                    // POpular defined as more than 1 vote
                    count = ideas.count { it.votes > 1}
                ChallengeDb.CountType.NUM_VOTES ->
                    // POpular defined as more than 1 vote
                    count = ideas.sumBy { it.votes}
                ChallengeDb.CountType.NONE ->
                    count = null
            }
            to.step2Count.put(step, count)
        }
        return to
    }


    /**
     * @return GameDb from values found in Game. May not be appropriate for insert of update.
     * Use {@link CanValidate} methods to check writeability
     */
    fun toGameDb(from: Game) : GameDb =
        GameDb(
            id = from.gameId, // could be 0 in case of new Game instance
            challengeId = from.challId, // must be set, even for new Game instance
            pin = from.pin,
            sessionStartMillisEpoch = from.sessionStartMillisEpoch,
            currentStep = from.currentStep
        )


}