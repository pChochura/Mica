package com.pointlessapps.granite.mica.runtime.model

internal data class PropertyAlias(
    val onVariableCallback: () -> Variable<*>,
    val onValueChangedCallback: (Variable<*>) -> Unit,
)
