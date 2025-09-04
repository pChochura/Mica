package com.pointlessapps.granite.mica.runtime.model

internal data class PropertyAlias(
    val onVariableCallback: () -> VariableType.Value,
    val onValueChangedCallback: (VariableType.Value) -> Unit,
)
