package be.t_ars.timekeeper.data

const val kID_NEW = -1L

open class AbstractEntry(var id: Long, var name: String, var weight: Int) {
    fun isNew() = id == kID_NEW
}
