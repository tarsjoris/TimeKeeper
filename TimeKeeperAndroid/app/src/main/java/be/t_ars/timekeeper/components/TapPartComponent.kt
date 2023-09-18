package be.t_ars.timekeeper.components

import android.os.Handler
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.databinding.TapPartBinding

class TapPartComponent(
    private val tapPart: TapPartBinding,
    private val startSoundCallback: (ClickDescription) -> Unit,
    private val stopSound: () -> Unit
) {
    private val clickTypeSelection: ToggleGroup<EClickType> = ToggleGroup(
        arrayOf(
            ToggleEntry(EClickType.COWBELL, tapPart.clicktypeCowbell),
            ToggleEntry(EClickType.SINE, tapPart.clicktypeSine),
            ToggleEntry(EClickType.SHAKER, tapPart.clicktypeShaker),
        )
    ) {
        clickType = it
        if (playing) {
            startSound()
        }
    }
    private val divisionsSelection: ToggleGroup<Int> = ToggleGroup(
        arrayOf(
            ToggleEntry(1, tapPart.divisions1),
            ToggleEntry(2, tapPart.divisions2),
            ToggleEntry(3, tapPart.divisions3),
            ToggleEntry(4, tapPart.divisions4),
        )
    ) {
        divisionCount = it
        if (playing) {
            startSound()
        }
    }
    private val beatsSelection: ToggleGroup<Int> = ToggleGroup(
        arrayOf(
            ToggleEntry(1, tapPart.beats1),
            ToggleEntry(2, tapPart.beats2),
            ToggleEntry(3, tapPart.beats3),
            ToggleEntry(4, tapPart.beats4),
            ToggleEntry(5, tapPart.beats5),
            ToggleEntry(6, tapPart.beats6),
            ToggleEntry(7, tapPart.beats7),
        )
    ) {
        beatCount = it
        if (playing) {
            startSound()
        }
    }
    private val delayedUpdate = DelayedUpdate()
    private val timestamps = LongArray(17) { 0 }
    private var size = 0
    private var index = 0
    private var playing = false

    private var tempo = ClickDescription.DEFAULT_TEMPO
    private var clickType = EClickType.DEFAULT
    private var divisionCount = 1
    private var beatCount = 1
    private var countOff = ClickDescription.DEFAULT_COUNT_OFF

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
        tapPart.tempoSpinner.value = ClickDescription.DEFAULT_TEMPO

        tapPart.tempoSpinner.setOnValueChangedListener { _, _, newValue ->
            tempo = newValue
            if (playing) {
                delayedUpdate.update()
            }
        }
        tapPart.checkboxCountOff.setOnCheckedChangeListener { _, newValue ->
            countOff = newValue
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

    fun setDivisionCount(divisions: Int) {
        if (divisionsSelection.setValue(divisions)) {
            divisionCount = divisions
            if (playing) {
                startSound()
            }
        }
    }

    fun setBeatCount(beats: Int) {
        if (beatsSelection.setValue(beats)) {
            beatCount = beats
            if (playing) {
                startSound()
            }
        }
    }

    fun setCountOff(countOff: Boolean) {
        tapPart.checkboxCountOff.isChecked = countOff
        this.countOff = countOff
    }

    fun getTempo() =
        tempo

    fun getClickType() =
        clickType

    fun getDivisionCount() =
        divisionCount

    fun getBeatCount() =
        beatCount

    fun isCountOff() =
        countOff

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
        startSoundCallback(ClickDescription(tempo, clickType, divisionCount, beatCount, countOff))
    }
}