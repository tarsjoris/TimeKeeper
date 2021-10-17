package be.t_ars.timekeeper.data

class Song(name: String, var tempo: Int?, var scoreLink: String? = null) : AbstractEntry(name) {
    constructor(other: Song) : this(other.name, other.tempo, other.scoreLink)
}
