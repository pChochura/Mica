package com.pointlessapps.granite.mica.model

data class Location(
    val line: Int,
    val column: Int,
    val length: Int,
) {
    companion object {
        val EMPTY = Location(-1, -1, 0)
    }
}
