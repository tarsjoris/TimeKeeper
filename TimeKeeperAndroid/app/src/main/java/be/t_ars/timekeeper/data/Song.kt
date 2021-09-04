package be.t_ars.timekeeper.data

class Song(name: String, weight: Int = -1, var tempo: Int?, var scoreLink: String? = null) : AbstractEntry(name, weight) {
    constructor(other: Song) : this(other.name, other.weight, other.tempo, other.scoreLink)
}
