package org.chenhome.dailybrainy.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

// Constants
private const val KEY_CURRENT_GAMEID = "CURRENT_GAMEID"
private val KEY_PLAYER_GUID = PlayerSession::guid.name
private val PREF_NAME = UserRepo::class.qualifiedName


/**
 * Information related to current user. Some info stored in application local preferences
 */
class UserRepo @Inject constructor(@ApplicationContext val context: Context) {
    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * @return null if no currentGameId is set
     */
    var currentGameGuid: String?
        get() {
            return pref.getString(KEY_CURRENT_GAMEID, "")
        }
        set(gameGuid) {
            pref
                .edit()
                .putString(KEY_CURRENT_GAMEID, gameGuid)
                .apply()
        }

    var currentPlayerGuid: String = ""
        get() {
            // Generate for this user
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
                Timber.d("Initialized currentPlayerGuid to $field")
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
