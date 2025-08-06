package com.pointlessapps.granite.mica.model

data class Location(
    val line: Int,
    val column: Int,
    val length: Int,
) : Comparable<Location> {

    override fun compareTo(other: Location) =
        compareValuesBy(this, other, { it.line }, { it.column }, { it.length })

    companion object {
        val EMPTY = Location(-1, -1, 0)
    }
}
