package be.t_ars.timekeeper.data

enum class EClickType(val value: Int) {
    COWBELL(1),
    SINE(2),
    SHAKER(3);

    companion object {
        val DEFAULT = COWBELL
        fun of(value: Int) =
            values().firstOrNull { it.value == value } ?: DEFAULT
    }
}