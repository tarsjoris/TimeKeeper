package be.t_ars.timekeeper.data

import java.io.Serializable

data class ClickDetails(
    val clickDescription: ClickDescription,
    val stereo: Boolean,
    val announceTitle: Boolean
) : Serializable {
    fun trackPath() =
        clickDescription.trackPath(stereo)
}