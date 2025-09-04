package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asCustomType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType

@Suppress("UNCHECKED_CAST")
private val setPropertyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "setProperty",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyCustomType),
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val type = args[0].value.toType()
        val customObject = args[0].value.asCustomType() as MutableMap<String, Any?>
        val propertyName = args[1].value.asStringType()
        if (customObject.containsKey(propertyName)) {
            throw IllegalStateException(
                "Property $propertyName does not exist in the ${type.name} type",
            )
        }

        val propertyType = requireNotNull(
            value = customObject[propertyName],
            lazyMessage = {
                "Property $propertyName does not exist in the ${type.name} type"
            },
        ).toType()
        val valueType = args[2].value.toType()
        if (!valueType.isSubtypeOf(propertyType)) {
            throw IllegalStateException(
                "Property $propertyName type mismatch: expected ${
                    propertyType.name
                }, got ${valueType.name}",
            )
        }

        customObject[propertyName] = args[2].value.asType(propertyType)
        return@create VariableType.Undefined
    },
)

internal val customObjectBuiltinFunctions = listOf(setPropertyFunction)
