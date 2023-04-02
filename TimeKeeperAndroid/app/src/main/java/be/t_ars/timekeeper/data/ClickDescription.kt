package be.t_ars.timekeeper.data

data class ClickDescription(val bpm: Int, val type: EClickType, val countOff: Boolean, val trackPath: String? = null) {
    companion object {
        const val DEFAULT_TEMPO = 120
        const val DEFAULT_COUNT_OFF = false
    }
}