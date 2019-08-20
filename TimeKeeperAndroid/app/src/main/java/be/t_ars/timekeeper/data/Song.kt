package be.t_ars.timekeeper.data

class Song(id: Long, name: String, weight: Int, var tempo: Int) : AbstractEntry(id, name, weight) {
    constructor(name: String, tempo: Int) : this(kID_NEW, name, -1, tempo)

    constructor(other: Song) : this(kID_NEW, other.name, other.weight, other.tempo)
}
