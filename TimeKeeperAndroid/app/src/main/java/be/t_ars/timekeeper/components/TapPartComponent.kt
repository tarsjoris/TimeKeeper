package be.t_ars.timekeeper.components

import android.os.Handler
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.databinding.TapPartBinding

class TapPartComponent(
    private val tapPart: TapPartBinding,
    private val startSound: (Int, EClickType) -> Unit,
    private val stopSound: () -> Unit
) {
    private val clickTypeSelection: ToggleGroup<EClickType> = ToggleGroup(
        arrayOf(
            ToggleEntry(EClickType.DIVISIONS_1, tapPart.clicktype1),
            ToggleEntry(EClickType.DIVISIONS_2, tapPart.clicktype2),
            ToggleEntry(EClickType.DIVISIONS_3, tapPart.clicktype3),
            ToggleEntry(EClickType.DIVISIONS_4, tapPart.clicktype4),
            ToggleEntry(EClickType.DIVISIONS_5, tapPart.clicktype5),
            ToggleEntry(EClickType.DIVISIONS_7, tapPart.clicktype7),
            ToggleEntry(EClickType.SHAKER, tapPart.clicktypeShaker),
        )
    ) { type ->
        clickType = type
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
    private var clickType = EClickType.DIVISIONS_1

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

    fun setClickType(type: EClickType) {
        if (clickTypeSelection.setValue(type)) {
            clickType = type
            if (playing) {
                startSound()
            }
        }
    }

    fun getTempo() =
        tempo

    fun getClickType() =
        clickType

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
        startSound(tempo, clickType)
    }
}