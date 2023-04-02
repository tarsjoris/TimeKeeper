package be.t_ars.timekeeper.data

class Song(
    name: String,
    var click: ClickDescription,
    var scoreLink: String? = null
) : AbstractEntry(name) {
    constructor(other: Song) : this(
        other.name,
        other.click,
        other.scoreLink
    )

    fun displayName() =
        name
            .let { if (scoreLink != null) "$it\u00B2" else it}
            .let { if (click.trackPath != null) "$it\u00B3" else it}

    override fun hashCode() =
        name.hashCode()

    override fun equals(other: Any?) =
        if (other is Song) other.name == name && other.click == click && other.scoreLink == scoreLink else false
}
