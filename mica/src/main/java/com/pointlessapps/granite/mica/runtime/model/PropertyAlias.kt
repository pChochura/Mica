package com.pointlessapps.granite.mica.runtime.model

internal data class PropertyAlias(
    val value: Variable<*>,
    val onValueChangedCallback: (Variable<*>) -> Unit,
)
