package be.t_ars.timekeeper.data

import java.io.Serializable

data class Section(val barCount: Int, val cue: ECue?) : Serializable {
    companion object {
        const val DEFAULT_BARCOUNT = 8
    }
}