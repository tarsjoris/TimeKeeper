package be.t_ars.timekeeper.data

enum class ECue(val id: String, val label: String) {
    A_CAPELLA("acapella", "A capella"),
    ACOUSTIC("acoustic", "Acoustic"),
    ALL_IN("allin", "All in"),
    BAND("band", "Band"),
    BASS("bass", "Bass"),
    BREAK("break", "Break"),
    BREAKDOWN("breakdown", "Breakdown"),
    BRIDGE("bridge", "Bridge"),
    BRING_IT_DOWN("bringitdown", "Bring it down"),
    BUILD_IT_UP("builditup", "Build it up"),
    CHORUS("chorus", "Chorus"),
    DOUBLE_TIME("doubletime", "Double time"),
    DRUMS("drums", "Drums"),
    END("end", "End"),
    FULL_BAND("fullband", "Full band"),
    GUITAR("guitar", "Guitar"),
    HALF_TIME("halftime", "Half time"),
    INSTRUMENTAL("instrumental", "Instrumental"),
    INTERLUDE("interlude", "Interlude"),
    INTRO("intro", "Intro"),
    KEYS("keys", "Keys"),
    LAST_TIME("lasttime", "Last time"),
    OUTRO("outro", "Outro"),
    PRE_CHORUS("prechorus", "Pre-chorus"),
    REPEAT("repeat", "Repeat"),
    SOLO("solo", "Solo"),
    SWELL("swell", "Swell"),
    VERSE("verse", "Verse");

    companion object {
        fun of(id: String) =
            ECue.values().firstOrNull { it.id == id }
    }
}