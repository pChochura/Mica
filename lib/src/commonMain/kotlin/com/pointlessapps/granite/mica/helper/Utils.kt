package com.pointlessapps.granite.mica.helper

internal fun Any?.className(): String = this?.let { it::class.simpleName }.orEmpty()
