package be.t_ars.timekeeper.data

class Song(id: Long = kID_NEW, name: String, weight: Int = -1, var tempo: Int?, var scoreLink: String? = null) : AbstractEntry(id, name, weight) {
    constructor(other: Song) : this(kID_NEW, other.name, other.weight, other.tempo, other.scoreLink)
}
