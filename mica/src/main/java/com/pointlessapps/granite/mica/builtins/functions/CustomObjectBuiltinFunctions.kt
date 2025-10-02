package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
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

@Suppress("UNCHECKED_CAST")
private val setPropertyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "setProperty",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyCustomType),
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val type = args[0].toType()
        val customObject = args[0].asCustomType() as MutableMap<String, Any?>
        val propertyName = args[1].asStringType()
        if (!customObject.containsKey(propertyName)) {
            throw IllegalStateException(
                "Property $propertyName does not exist in the $type type",
            )
        }

        val propertyType = requireNotNull(
            value = customObject[propertyName],
            lazyMessage = {
                "Property $propertyName does not exist in the $type type"
            },
        ).toType()
        val valueType = args[2].toType()
        if (!valueType.isSubtypeOf(propertyType)) {
            throw IllegalStateException(
                "Property $propertyName type mismatch: expected $propertyType, got $valueType",
            )
        }

        customObject[propertyName] = args[2].asType(propertyType)
        return@create null
    },
)

internal val customObjectBuiltinFunctions = listOf(setPropertyFunction)
