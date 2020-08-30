package org.chenhome.dailybrainy.repo

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
    val playerSession: PlayerSession
)

/**
 * Data object representing the full and comprehensive state
 * of the game, which includes the ideas generated during the game
 * and the players that participated.
 */
data class FullGame(
    var game: Game = Game(),
    var players: MutableList<PlayerSession> = mutableListOf(),
    var ideas: MutableList<Idea> = mutableListOf(),
    var challenge: Challenge = Challenge() // current challenge
) {
    fun mySession(currentPlayerGuid: String): PlayerSession? {
        return players.firstOrNull {
            it.userGuid == currentPlayerGuid
        }
    }
}