package be.t_ars.timekeeper.data

import java.io.Serializable

data class Section(val barCount: Int, val cue: ECue?) : Serializable {
    companion object {
        const val DEFAULT_BARCOUNT = 8
    }
}

data class ClickDescription(
    val bpm: Int,
    val type: EClickType,
    val divisionCount: Int,
    val beatCount: Int,
    val countOff: Boolean,
    val sections: List<Section> = emptyList(),
    val trackPath: String? = null
) : Serializable {
    companion object {
        const val DEFAULT_TEMPO = 120
        const val DEFAULT_DIVISION_COUNT = 1
        const val DEFAULT_BEAT_COUNT = 1
        const val DEFAULT_COUNT_OFF = false
    }
}