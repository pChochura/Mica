package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.model.UndefinedVariable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

@Suppress("UNCHECKED_CAST")
internal val setPropertyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "setProperty",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyCustomType),
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val customObject = args[0].value as CustomObject
        val propertyName = args[1].type.valueAsSupertype<StringType>(args[1].value) as String
        if (propertyName !in customObject.keys) {
            throw IllegalStateException(
                "Property $propertyName does not exist in the ${args[0].type.name} type",
            )
        }

        val propertyType = requireNotNull(customObject[propertyName]).type
        if (!args[2].type.isSubtypeOf(propertyType)) {
            throw IllegalStateException(
                "Property $propertyName type mismatch: expected ${
                    propertyType.name
                }, got ${args[2].type.name}",
            )
        }

        customObject[propertyName] = propertyType.toVariable(
            args[2].type.valueAsSupertype(args[2].value, propertyType),
        )
        return@create UndefinedVariable
    },
)
