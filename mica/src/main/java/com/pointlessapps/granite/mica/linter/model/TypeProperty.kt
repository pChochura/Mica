package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.model.Type

internal data class TypeProperty(
    val name: String,
    val receiverType: Type,
    val returnType: Type,
    val hasDefaultValue: Boolean,
    val isBuiltin: Boolean,
)
