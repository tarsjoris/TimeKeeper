package be.t_ars.timekeeper.components

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.core.widget.doOnTextChanged
import be.t_ars.timekeeper.SoundService
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.ClickDetails
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.databinding.TapPartBinding
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun parseTempo(tempo: CharSequence?): Int {
    if (tempo.isNullOrBlank()) {
        return ClickDescription.DEFAULT_TEMPO
    }
    try {
        return min(300, max(30, Integer.parseInt(tempo.toString())))
    } catch (e: NumberFormatException) {
        Log.e("TimeKeeper", "Invalid tempo: " + e.message, e)
    }
    return ClickDescription.DEFAULT_TEMPO
}

class TapPartComponent(
    private val context: Context,
    private val tapPart: TapPartBinding,
    private val getReturnActivityExtras: (ClickDetails) -> HashMap<String, Serializable>?
) {
    private val clickTypeSelection: ToggleGroup<EClickType> = ToggleGroup(
        arrayOf(
            ToggleEntry(EClickType.COWBELL, tapPart.clicktypeCowbell),
            ToggleEntry(EClickType.SINE, tapPart.clicktypeSine),
            ToggleEntry(EClickType.SHAKER, tapPart.clicktypeShaker),
        )
    ) {
        clickType = it
        clickChanged()
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
        clickChanged()
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
        clickChanged()
    }
    private val delayedUpdate = DelayedUpdate()
    private val timestamps = LongArray(17) { 0 }
    private var size = 0
    private var index = 0

    private var tempo = ClickDescription.DEFAULT_TEMPO
    private var clickType = EClickType.DEFAULT
    private var divisionCount = 1
    private var beatCount = 1
    private var countOff = ClickDescription.DEFAULT_COUNT_OFF
    private var twoBarCountoff = ClickDescription.DEFAULT_TWO_BAR_COUNT_OFF
    private var stereo = Playlist.DEFAULT_STEREO
    private var announceTitle = Playlist.DEFAULT_ANNOUNCE_TITLE
    private var playing = false

    private inner class DelayedUpdate : Runnable {
        private var hasRun = true

        @Suppress("DEPRECATION")
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
                clickChanged()
                hasRun = true
            }
        }
    }

    init {
        tapPart.buttonTap.setOnClickListener {
            doTap()
        }

        tapPart.textTempo.setText(ClickDescription.DEFAULT_TEMPO.toString())

        tapPart.textTempo.doOnTextChanged { text, _, _, _ ->
            tempo = parseTempo(text)
            delayedUpdate.update()
        }
        tapPart.checkboxCountOff.setOnCheckedChangeListener { _, newValue ->
            countOff = newValue
        }
        tapPart.checkboxTwoBarCountOff.setOnCheckedChangeListener { _, newValue ->
            twoBarCountoff = newValue
        }

        tapPart.buttonStart.setOnClickListener {
            playing = true
            startSound()
        }
        tapPart.buttonStop.setOnClickListener {
            playing = false
            stopSound()
        }
    }

    fun setClick(newClick: ClickDetails) {
        var changed = false
        if (newClick.clickDescription.bpm.toString() != tapPart.textTempo.text.toString()) {
            tapPart.textTempo.setText(newClick.clickDescription.bpm.toString())
            tempo = newClick.clickDescription.bpm
            changed = true
        }
        if (newClick.clickDescription.countOff != tapPart.checkboxCountOff.isChecked) {
            tapPart.checkboxCountOff.isChecked = newClick.clickDescription.countOff
            countOff = newClick.clickDescription.countOff
            changed = true
        }
        if (newClick.clickDescription.twoBarCountOff != tapPart.checkboxTwoBarCountOff.isChecked) {
            tapPart.checkboxTwoBarCountOff.isChecked = newClick.clickDescription.twoBarCountOff
            twoBarCountoff = newClick.clickDescription.twoBarCountOff
            changed = true
        }
        if (newClick.stereo != stereo) {
            stereo = newClick.stereo
            changed = true
        }
        if (newClick.announceTitle != announceTitle) {
            announceTitle = newClick.announceTitle
            changed = true
        }
        if (clickTypeSelection.setValue(newClick.clickDescription.type)) {
            clickType = newClick.clickDescription.type
            changed = true
        }
        if (divisionsSelection.setValue(newClick.clickDescription.divisionCount)) {
            divisionCount = newClick.clickDescription.divisionCount
            changed = true
        }
        if (beatsSelection.setValue(newClick.clickDescription.beatCount)) {
            beatCount = newClick.clickDescription.beatCount
            changed = true
        }

        if (changed) {
            clickChanged()
        }
    }

    fun getClick() =
        ClickDetails(ClickDescription(tempo, clickType, divisionCount, beatCount, countOff, twoBarCountoff), stereo, announceTitle)

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
            val tempoS = "$tempo"
            tapPart.tempo16.text = tempoS
            tapPart.textTempo.setText(tempoS)
            this.tempo = tempo
        }
    }

    private fun calculateBPM(granularity: Int): Int? {
        if (size > granularity) {
            val index2 = (index + (timestamps.size - granularity)) % timestamps.size
            val diff = timestamps[index] - timestamps[index2]
            return (60000.toDouble() * granularity.toDouble() / diff.toDouble()).roundToInt()
        }
        return null
    }

    private fun clickChanged() {
        if (playing) {
            startSound()
        }
    }

    private fun startSound() {
        val click = getClick()
        val returnActivityExtras = getReturnActivityExtras(click)
        val returnActivityClass = if (returnActivityExtras != null) context.javaClass else null
        SoundService.startSound(context, null, click, returnActivityClass, returnActivityExtras)
    }

    private fun stopSound() {
        SoundService.stopSound(context)
    }
}