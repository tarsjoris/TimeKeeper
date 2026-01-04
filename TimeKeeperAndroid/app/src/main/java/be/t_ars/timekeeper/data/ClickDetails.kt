package be.t_ars.timekeeper.data

import java.io.Serializable

data class ClickDetails(
    val clickDescription: ClickDescription,
    val stereo: Boolean
) : Serializable {
    fun trackPath() =
        clickDescription.trackPath(stereo)

    companion object {
        const val DEFAULT_STEREO = true
    }
}