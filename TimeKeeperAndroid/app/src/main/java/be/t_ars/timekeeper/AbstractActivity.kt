package be.t_ars.timekeeper

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

open class AbstractActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let(this::setIntent)
    }
}