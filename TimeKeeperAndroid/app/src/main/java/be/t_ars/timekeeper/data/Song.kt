package be.t_ars.timekeeper.data

class Song(name: String, var tempo: Int?, var scoreLink: String? = null) : AbstractEntry(name) {
    constructor(other: Song) : this(other.name, other.tempo, other.scoreLink)

    override fun hashCode() =
        name.hashCode()

    override fun equals(other: Any?) =
        if (other is Song) other.name == name && other.tempo == tempo && other.scoreLink == scoreLink else false
}
