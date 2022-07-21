package be.t_ars.timekeeper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager


private const val kFREQUENCY = "frequency"
private const val kDURATION = "duration"
private const val kSCREEN_ORIENTATION = "screenorientation"
private const val kFOLDER = "folder"
private const val kAUTOPLAY = "autoplay"
private const val kCURRENT_PLAYLIST_ID = "currentplaylistid"
private const val kCURRENT_SONG_INDEX = "currentsongindex"

fun getSettingFrequency(context: Context) =
    getIntPreference(context, kFREQUENCY, 880)

fun getSettingDuration(context: Context) =
    getIntPreference(context, kDURATION, 20)

fun getSettingScreenOrientation(context: Context) =
    getIntPreference(context, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)

fun getSettingFolder(context: Context) =
    getStringPreference(
        context,
        kFOLDER,
        "content://com.android.externalstorage.documents/tree/primary%3ATimeKeeper"
    )

fun setSettingFolder(context: Context, uri: String) =
    setStringPreference(context, kFOLDER, uri)

fun getSettingAutoplay(context: Context) =
    getBoolPreference(context, kAUTOPLAY, true)

fun getSettingCurrentPlaylistID(context: Context): Long? =
    getLongPreference(context, kCURRENT_PLAYLIST_ID)

fun setSettingCurrentPlaylistID(context: Context, id: Long) =
    setLongPreference(context, kCURRENT_PLAYLIST_ID, id)

fun getSettingCurrentSongIndex(context: Context) =
    getIntPreference(context, kCURRENT_SONG_INDEX, 0)

fun setSettingCurrentSongIndex(context: Context, index: Int) =
    setIntPreference(context, kCURRENT_SONG_INDEX, index)

private fun getIntPreference(context: Context, key: String, defaultValue: Int): Int {
    val result = getStringPreference(context, key, defaultValue.toString())
    return try {
        result?.toInt() ?: defaultValue
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

private fun setIntPreference(context: Context, key: String, value: Int) =
    setStringPreference(context, key, value.toString())

private fun getLongPreference(context: Context, key: String): Long? {
    val result = getStringPreference(context, key)
    return try {
        result?.toLong()
    } catch (e: NumberFormatException) {
        null
    }
}

private fun setLongPreference(context: Context, key: String, value: Long) =
    setStringPreference(context, key, value.toString())

private fun getBoolPreference(context: Context, key: String, defaultValue: Boolean): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue)
}

private fun getStringPreference(
    context: Context,
    key: String,
    defaultValue: String? = null
): String? {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue)
}

private fun setStringPreference(context: Context, key: String, value: String) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(key, value)
        .apply()
}

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation =
            getSettingScreenOrientation(this)
    }

    companion object {
        fun startActivity(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .let(context::startActivity)
    }
}
