package be.t_ars.timekeeper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager


const val kFREQUENCY = "frequency"
const val kDURATION = "duration"
const val kSCREEN_ORIENTATION = "screenorientation"

fun getIntPreference(context: Context, key: String, defaultValue: Int): Int {
    val result = PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue.toString())
    return try {
        result?.toInt() ?: defaultValue
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
    }

    companion object {
        fun startActivity(context: Context) =
                Intent(context, SettingsActivity::class.java)
                        .let(context::startActivity)
    }
}
