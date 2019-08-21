package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.MotionEvent
import kotlinx.android.synthetic.main.tap.*
import kotlinx.android.synthetic.main.tap_part.*
import java.io.Serializable

class TapActivity : AbstractActivity() {
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
                    Handler().postDelayed(this, 500)
                }
            }
        }

        override fun run() {
            synchronized(this) {
                startSound(tempo)
                hasRun = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tap)
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
            startSound(tempo_spinner.value)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                OverviewActivity.startActivity(this)
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        val extras = intent.extras
        if (extras != null) {
            val tempo = extras.getInt(kINTENT_DATA_TEMPO, 120)
            tempo_spinner.value = tempo
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
                startSound(tempo)
            }
        }
    }

    private fun startSound(tempo: Int) {
        val extras = HashMap<String, Serializable>().also {
            it[kINTENT_DATA_TEMPO] = tempo
        }
        SoundService.startSound(this, null, tempo, TapActivity::class.java, extras)
    }

    companion object {
        private const val kINTENT_DATA_TEMPO = "tempo"

        fun startActivity(context: Context, tempo: Int? = null) =
                Intent(context, TapActivity::class.java)
                        .also { intent ->
                            tempo?.let { t -> intent.putExtra(kINTENT_DATA_TEMPO, t) }
                        }
                        .let(context::startActivity)
    }
}