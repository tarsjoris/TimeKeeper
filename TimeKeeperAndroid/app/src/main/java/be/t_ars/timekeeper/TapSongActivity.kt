package be.t_ars.timekeeper

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.ToggleEntry
import be.t_ars.timekeeper.components.ToggleGroup
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fDivisionSelection: ToggleGroup<Int>
    private val delayedUpdate = DelayedUpdate()
    private val fTimestamps = LongArray(17) { 0 }
    private var fSize = 0
    private var fIndex = 0
    private var fTempo = 120
    private var fDivisions = 1
    private var fPlaying = false

    private inner class DelayedUpdate : Runnable {
        private var hasRun = true

        fun update() {
            synchronized(this) {
                if (hasRun) {
                    hasRun = false
                    Handler(Looper.getMainLooper()).postDelayed(this, 500)
                }
            }
        }

        override fun run() {
            synchronized(this) {
                startSound()
                hasRun = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fBinding.tapPart.tempoSpinner.minValue = 10
        fBinding.tapPart.tempoSpinner.maxValue = 500
        fBinding.tapPart.tempoSpinner.value = 120

        fBinding.tapPart.buttonTap.setOnClickListener {
            doTap()
        }
        fBinding.tapPart.tempoSpinner.setOnValueChangedListener { _, _, newValue ->
            if (fPlaying) {
                fTempo = newValue
                delayedUpdate.update()
            }
        }
        fDivisionSelection = ToggleGroup(arrayOf(
            ToggleEntry(1, fBinding.tapPart.division1),
            ToggleEntry(2, fBinding.tapPart.division2),
            ToggleEntry(3, fBinding.tapPart.division3),
            ToggleEntry(4, fBinding.tapPart.division4),
            ToggleEntry(5, fBinding.tapPart.division5),
            ToggleEntry(7, fBinding.tapPart.division7),
        )) { count ->
            fDivisions = count
            if (fPlaying) {
                startSound()
            }
        }
        fBinding.tapPart.buttonStart.setOnClickListener {
            fPlaying = true
            startSound()
        }
        fBinding.tapPart.buttonStop.setOnClickListener {
            fPlaying = false
            SoundService.stopSound(this)
        }

        fBinding.tapPart.checkboxWithTempo.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
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
                fBinding.tapPart.tempoSpinner.clearFocus()

                Intent().also {
                    it.putExtra(kINTENT_DATA_NAME, fBinding.name.text.toString())
                    it.putExtra(kINTENT_DATA_SCORE_LINK, fBinding.scoreLink.text.toString())
                    it.putExtra(kINTENT_DATA_TEMPO, fTempo)
                    it.putExtra(kINTENT_DATA_DIVISIONS, fDivisions)
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
        fBinding.name.setText(intent.getStringExtra(kINTENT_DATA_NAME))

        val newTempo = intent.getIntExtra(kINTENT_DATA_TEMPO, -1)
        val withTempo = newTempo != -1
        fBinding.tapPart.checkboxWithTempo.isChecked = withTempo
        setTempo(if (withTempo) newTempo else 120)

        val newDivisions = intent.getIntExtra(kINTENT_DATA_DIVISIONS, 1)
        setDivisions(newDivisions)

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    private fun doTap() {
        fIndex = (fIndex + 1) % fTimestamps.size
        fTimestamps[fIndex] = System.currentTimeMillis()
        if (fSize < fTimestamps.size) {
            ++fSize
        }
        displayStats()
    }

    private fun displayStats() {
        calculateBPM(4)?.let { tempo ->
            fBinding.tapPart.tempo4.text = "$tempo"
        }
        calculateBPM(8)?.let { tempo ->
            fBinding.tapPart.tempo8.text = "$tempo"
        }
        calculateBPM(16)?.let { tempo ->
            fBinding.tapPart.tempo16.text = "$tempo"
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
        if (tempo >= fBinding.tapPart.tempoSpinner.minValue && tempo <= fBinding.tapPart.tempoSpinner.maxValue) {
            fBinding.tapPart.tempoSpinner.value = tempo
            fTempo = tempo
            if (fPlaying) {
                startSound()
            }
        }
    }

    private fun setDivisions(divisions: Int) {
        if (fDivisionSelection.setValue(divisions)) {
            fDivisions = divisions
            if (fPlaying) {
                startSound()
            }
        }
    }

    private fun startSound() {
        SoundService.startSound(this, null, fTempo, fDivisions)
    }

    companion object {
        const val kINTENT_DATA_TEMPO = "tempo"
        const val kINTENT_DATA_DIVISIONS = "divisions"
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_SCORE_LINK = "score_link"

        fun startActivityForResult(
            context: FragmentActivity,
            tempo: Int?,
            divisions: Int,
            name: String,
            scoreLink: String?,
            requestCode: Int
        ) =
            Intent(context, TapSongActivity::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_TEMPO, tempo)
                    intent.putExtra(kINTENT_DATA_DIVISIONS, divisions)
                    intent.putExtra(kINTENT_DATA_NAME, name)
                    if (scoreLink != null)
                        intent.putExtra(kINTENT_DATA_SCORE_LINK, scoreLink)
                }
                .let { context.startActivityForResult(it, requestCode) }
    }
}