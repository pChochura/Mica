package com.pointlessapps.granite.mica.builtins.properties

import com.pointlessapps.granite.mica.model.Type

/**
 * Represents a builtin type property.
 */
internal class BuiltinTypePropertyDeclaration(
    val name: String,
    val receiverType: Type,
    val returnType: Type,
    val execute: (receiver: Any?) -> Any,
)
