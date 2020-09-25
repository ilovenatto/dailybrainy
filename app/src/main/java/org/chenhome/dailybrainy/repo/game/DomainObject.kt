package org.chenhome.dailybrainy.repo.game

import android.content.Context
import android.net.Uri
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.PlayerSession
import timber.log.Timber
import java.util.*

/**
 * Domain objects that align more closely with how the UI renders the data.
 * Typically composed of Data objects.
 */

/**
 * Represents the basic details of a user's
 * participation in a Game.
 */
data class GameStub(
    val game: Game,
    val challenge: Challenge?,
    val players: List<PlayerSession>,
) {

    fun date(context: Context): String? =
        game.sessionStartMillis?.let { millis ->
            android.text.format.DateFormat.getDateFormat(context).format(Date(millis))
        }

    fun prettySummary(context: Context): String {
        val head = "You joined " + date(context) + " with "
        var tail = ""
        players.forEachIndexed { index, p ->
            tail += p.name
            if (index < players.size - 1) {
                tail += ", "
            }
        }
        return head + tail
    }
}

/**
 * Data object representing the full and comprehensive state
 * of the game, which includes the ideas generated during the game
 * and the players that participated.
 */
data class FullGame(
    var game: Game = Game(),
    var players: MutableList<PlayerSession> = mutableListOf(),
    var challenge: Challenge = Challenge(), // current challenge
) {
    private var _ideas: MutableList<Idea> = mutableListOf()

    fun mostPopularSketchUri(origin: Idea.Origin): Uri? =
        ideas(origin).maxByOrNull { it.votes }?.let { Sketch(it) }?.imgUri

    fun mostPopularSketch(origin: Idea.Origin): Sketch? =
        ideas(origin).maxByOrNull { it.votes }?.let { Sketch(it) }


    fun ideas(origin: Idea.Origin): List<Idea> = _ideas.filter { it.origin == origin }
    fun ideasCount(origin: Idea.Origin): Int = _ideas.filter { it.origin == origin }.size
    fun ideasCount(origin: Idea.Origin, playerGuid: String): Int = _ideas
        .filter { it.origin == origin && it.playerGuid == playerGuid }.size

    fun voteCount(origin: Idea.Origin): Int = _ideas
        .filter {
            it.origin == origin
        }.fold(0, { sum, idea ->
            sum + idea.votes
        })


    fun add(idea: Idea) {
        // doesn't exist
        if (_ideas.firstOrNull { idea.guid == it.guid } == null) {
            _ideas.add(idea)
        }
    }

    fun update(idea: Idea) {
        // doesn't exist
        val index = _ideas.indexOfFirst { idea.guid == it.guid }
        if (index >= 0) {
            _ideas[index] = idea
        }
    }

    fun remove(idea: Idea) {
        _ideas.removeIf { idea.guid == it.guid }
    }
}

/**
 * Idea decorated with an image URI
 */
data class Sketch(
    val idea: Idea,
) {
    val imgUri: Uri?
        get() = idea.imgUri?.let {
            val uri = Uri.parse(it)
            Timber.d("Parsed as uri $uri")
            uri
        }
}


/**
 * Challenge decorated with Lesson properties
 *
 * @property challenge
 */
data class Lesson(
    val challenge: Challenge,
) {

    fun toYoutubeUri(): Uri? = challenge.youtubeId?.let {
        Uri.parse("https://youtu.be/" + it)
    }

    fun toYoutubeThumbUri(): Uri? = challenge.youtubeId?.let {
        Uri.parse("https://img.youtube.com/vi/"
                + it
                + "/hq1.jpg"
        )
    }
}
