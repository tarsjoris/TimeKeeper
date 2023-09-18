package be.t_ars.timekeeper.data

data class ClickDescription(
    val bpm: Int,
    val type: EClickType,
    val divisionCount: Int,
    val beatCount: Int,
    val countOff: Boolean,
    val trackPath: String? = null) {
    companion object {
        const val DEFAULT_TEMPO = 120
        const val DEFAULT_DIVISION_COUNT = 1
        const val DEFAULT_BEAT_COUNT = 1
        const val DEFAULT_COUNT_OFF = false
    }
}