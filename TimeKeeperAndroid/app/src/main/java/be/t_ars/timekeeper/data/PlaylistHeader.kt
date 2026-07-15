package be.t_ars.timekeeper.data


open class PlaylistHeader(var id: Long, name: String, var stereo: Boolean, var announceTitle: Boolean, var weight: Int) : AbstractEntry(name)
