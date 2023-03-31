package be.t_ars.timekeeper.data

enum class EClickType(val value: Int) {
    DIVISIONS_1(1),
    DIVISIONS_2(2),
    DIVISIONS_3(3),
    DIVISIONS_4(4),
    DIVISIONS_5(5),
    DIVISIONS_7(7),
    SHAKER(-1);

    companion object {
        val DEFAULT = DIVISIONS_1
        fun of(value: Int) =
            values().firstOrNull { it.value == value } ?: DEFAULT
    }
}