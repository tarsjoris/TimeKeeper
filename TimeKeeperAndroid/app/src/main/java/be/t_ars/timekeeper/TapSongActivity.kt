package be.t_ars.timekeeper

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.tap_part.*
import kotlinx.android.synthetic.main.tap_song.*

class TapSongActivity : AbstractActivity() {
    private val delayedUpdate = DelayedUpdate()
    private val fTimestamps = LongArray(17) { 0 }
    private var fSize = 0
    private var fIndex = 0
    private var fPlaying = false

    private inner class DelayedUpdate : Runnable {
        private var tempo = 0
        private var hasRun = true

        fun update(newTempo: Int) {
            synchronized(this) {
                tempo = newTempo
                if (hasRun) {
                    hasRun = false
                    Handler(Looper.getMainLooper()).postDelayed(this, 500)
                }
            }
        }

        override fun run() {
            synchronized(this) {
                SoundService.startSound(this@TapSongActivity, null, tempo)
                hasRun = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tap_song)
        setSupportActionBar(toolbar)

        tempo_spinner.minValue = 10
        tempo_spinner.maxValue = 500
        tempo_spinner.value = 120

        button_tap.setOnTouchListener { _, motionEvent ->
            doTap(motionEvent)
            false
        }
        tempo_spinner.setOnValueChangedListener { _, _, newValue ->
            if (fPlaying) {
                delayedUpdate.update(newValue)
            }
        }
        button_start.setOnClickListener {
            fPlaying = true
            SoundService.startSound(this, null, tempo_spinner.value)
        }
        button_stop.setOnClickListener {
            fPlaying = false
            SoundService.stopSound(this)
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        loadIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tap_song_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tap_action_accept -> {
                Intent().also {
                    it.putExtra(kINTENT_DATA_NAME, name.text.toString())
                    it.putExtra(kINTENT_DATA_TEMPO, tempo_spinner.value)
                    setResult(RESULT_OK, it)
                }
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        intent.getStringExtra(kINTENT_DATA_NAME).let {
            name.setText(it)
        }
        val newTempo = intent.getIntExtra(kINTENT_DATA_TEMPO, -1)
        if (newTempo != -1) {
            setTempo(newTempo)
        }
    }

    private fun doTap(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            fIndex = (fIndex + 1) % fTimestamps.size
            fTimestamps[fIndex] = System.currentTimeMillis()
            if (fSize < fTimestamps.size) {
                ++fSize
            }
            displayStats()
            return true
        }
        return false
    }

    private fun displayStats() {
        calculateBPM(4)?.let { tempo ->
            tempo4.text = "$tempo"
        }
        calculateBPM(8)?.let { tempo ->
            tempo8.text = "$tempo"
        }
        calculateBPM(16)?.let { tempo ->
            tempo16.text = "$tempo"
            setTempo(tempo)
        }
    }

    private fun calculateBPM(granularity: Int): Int? {
        if (fSize > granularity) {
            val index2 = (fIndex + (fTimestamps.size - granularity)) % fTimestamps.size
            val diff = fTimestamps[fIndex] - fTimestamps[index2]
            return (60000 * granularity / diff).toInt()
        }
        return null
    }

    private fun setTempo(tempo: Int) {
        if (tempo >= tempo_spinner.minValue && tempo <= tempo_spinner.maxValue) {
            tempo_spinner.value = tempo
            if (fPlaying) {
                SoundService.startSound(this, null, tempo)
            }
        }
    }

    companion object {
        const val kINTENT_DATA_TEMPO = "tempo"
        const val kINTENT_DATA_NAME = "name"

        fun startActivityForResult(context: FragmentActivity, tempo: Int, name: String, requestCode: Int) =
                Intent(context, TapSongActivity::class.java)
                        .also { intent ->
                            intent.putExtra(kINTENT_DATA_TEMPO, tempo)
                            intent.putExtra(kINTENT_DATA_NAME, name)
                        }
                        .let { context.startActivityForResult(it, requestCode) }
    }
}