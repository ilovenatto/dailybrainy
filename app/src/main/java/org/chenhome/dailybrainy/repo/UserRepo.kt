package org.chenhome.dailybrainy.repo

import android.content.Context
import org.chenhome.dailybrainy.repo.local.PlayerSession
import org.chenhome.dailybrainy.repo.local.genGuid
import timber.log.Timber

/**
 * Information related to current user. Some info stored in application local preferences
 */
class UserRepo(val context: Context) {

    private val KEY_CURRENT_GAMEID = "CURRENT_GAMEID"
    private val KEY_PLAYER_GUID = PlayerSession::guid.name
    private val PREF_NAME = UserRepo::class.qualifiedName
    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)


    /**
     * @return 0 if no currentGameId is set
     */
    var currentGameId: Long
        get() {
            return pref.getLong(KEY_CURRENT_GAMEID, 0)
        }
        set(gameId: Long) {
            pref
                .edit()
                .putLong(KEY_CURRENT_GAMEID, gameId)
                .commit()
        }

    var currentPlayerGuid: String = ""
        get() {
            if (field.isEmpty()) {
                // get from pref
                val newGuid = genGuid()
                if (pref.contains(KEY_PLAYER_GUID)) {
                    field = pref.getString(KEY_PLAYER_GUID, newGuid) ?: newGuid
                } else {
                    field = newGuid
                    if (!pref.edit().putString(KEY_PLAYER_GUID, newGuid).commit()) {
                        Timber.w("Unable to commit player GUID $newGuid")
                    }
                }
                Timber.d("Initialized currentPlayerGuid to $field");
            }
            return field
        }
        // read-only value
        private set

    fun clearUserPrefs() {
        if (!pref.edit().clear().commit()) {
            Timber.w("Unable to clear preferences")
        }
    }
}
