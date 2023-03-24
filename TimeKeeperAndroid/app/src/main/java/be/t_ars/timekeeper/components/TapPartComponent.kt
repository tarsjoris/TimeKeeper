package be.t_ars.timekeeper.components

import android.os.Handler
import be.t_ars.timekeeper.databinding.TapPartBinding

class TapPartComponent(
    private val tapPart: TapPartBinding,
    private val startSound: (Int, Int) -> Unit,
    private val stopSound: () -> Unit
) {
    private val divisionSelection: ToggleGroup<Int> = ToggleGroup(
        arrayOf(
            ToggleEntry(1, tapPart.division1),
            ToggleEntry(2, tapPart.division2),
            ToggleEntry(3, tapPart.division3),
            ToggleEntry(4, tapPart.division4),
            ToggleEntry(5, tapPart.division5),
            ToggleEntry(7, tapPart.division7),
        )
    ) { count ->
        divisions = count
        if (playing) {
            startSound()
        }
    }
    private val delayedUpdate = DelayedUpdate()
    private val timestamps = LongArray(17) { 0 }
    private var size = 0
    private var index = 0
    private var playing = false

    private var tempo = 120
    private var divisions = 1

    private inner class DelayedUpdate : Runnable {
        private var hasRun = true

        fun update() {
            synchronized(this) {
                if (hasRun) {
                    hasRun = false
                    Handler().postDelayed(this, 500)
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

    init {
        tapPart.buttonTap.setOnClickListener {
            doTap()
        }

        tapPart.tempoSpinner.minValue = 10
        tapPart.tempoSpinner.maxValue = 500
        tapPart.tempoSpinner.value = 120

        tapPart.tempoSpinner.setOnValueChangedListener { _, _, newValue ->
            if (playing) {
                tempo = newValue
                delayedUpdate.update()
            }
        }
        tapPart.buttonStart.setOnClickListener {
            playing = true
            tempo = tapPart.tempoSpinner.value
            startSound()
        }
        tapPart.buttonStop.setOnClickListener {
            playing = false
            stopSound()
        }
    }

    fun setTempo(newTempo: Int) {
        if (newTempo >= tapPart.tempoSpinner.minValue && newTempo <= tapPart.tempoSpinner.maxValue) {
            tapPart.tempoSpinner.value = newTempo
            tempo = newTempo
            if (playing) {
                startSound()
            }
        }
    }

    fun setDivisions(newDivisions: Int) {
        if (divisionSelection.setValue(newDivisions)) {
            divisions = newDivisions
            if (playing) {
                startSound()
            }
        }
    }

    fun getTempo() =
        tempo

    fun getDivisions() =
        divisions

    private fun doTap() {
        index = (index + 1) % timestamps.size
        timestamps[index] = System.currentTimeMillis()
        if (size < timestamps.size) {
            ++size
        }
        displayStats()
    }

    private fun displayStats() {
        calculateBPM(4)?.let { tempo ->
            tapPart.tempo4.text = "$tempo"
        }
        calculateBPM(8)?.let { tempo ->
            tapPart.tempo8.text = "$tempo"
        }
        calculateBPM(16)?.let { tempo ->
            tapPart.tempo16.text = "$tempo"
            setTempo(tempo)
        }
    }

    private fun calculateBPM(granularity: Int): Int? {
        if (size > granularity) {
            val index2 = (index + (timestamps.size - granularity)) % timestamps.size
            val diff = timestamps[index] - timestamps[index2]
            return (60000 * granularity / diff).toInt()
        }
        return null
    }

    private fun startSound() {
        startSound(tempo, divisions)
    }
}