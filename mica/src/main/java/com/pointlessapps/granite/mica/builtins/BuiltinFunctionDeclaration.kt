package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.Type

/**
 * Represents a builtin function.
 */
internal class BuiltinFunctionDeclaration(
    val name: String,
    val parameters: List<Pair<String, Type>>,
    val getReturnType: (argumentTypes: List<Type>) -> Type?,
    val execute: (arguments: List<Pair<Type, Any>>) -> Any,
) {
    /**
     * Function signature in a format:
     * <function name>(<parameter type>,<parameter type>, ...)
     */
    val signature: String = "$name(${parameters.joinToString { it.second.name }})"
}
