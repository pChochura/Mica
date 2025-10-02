package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.resolver.compareTo

private val keysFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "keys",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyMapType)),
    getReturnType = { _, args -> ArrayType((args[0] as MapType).keyType) },
    execute = { _, args -> args[0].asMapType().keys.toMutableList() },
)

private val valuesFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "values",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyMapType)),
    getReturnType = { _, args -> ArrayType((args[0] as MapType).valueType) },
    execute = { _, args -> args[0].asMapType().values.toMutableList() },
)

private val containsKeyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "containsKey",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(MapType(EmptyGenericType, AnyType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val map = args[0].asMapType()
        return@create map.keys.firstOrNull {
            it.compareTo(args[1].asType(it.toType())) == 0
        } != null
    },
)

private val containsValueFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "containsValue",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(MapType(AnyType, EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val map = args[0].asMapType()
        return@create map.values.firstOrNull {
            it.compareTo(args[1].asType(it.toType())) == 0
        } != null
    },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(MapType(EmptyGenericType, AnyType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    getReturnType = { _, args -> (args[0] as MapType).valueType },
    execute = { _, args ->
        val map = args[0].asMapType()
        val keyType = (map.toType() as MapType).keyType
        val key = args[1].asType(keyType)
        return@create map.remove(key)
    },
)

@Suppress("UNCHECKED_CAST")
private val putFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "put",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(MapType(EmptyGenericType, AnyType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val map = args[0].asMapType() as MutableMap<Any?, Any?>
        val mapType = map.toType() as MapType
        val keyType = mapType.keyType
        val valueType = mapType.valueType
        if (!args[2].toType().isSubtypeOf(valueType)) {
            throw IllegalArgumentException("Function put expects $keyType as a second argument")
        }

        map[args[1].asType(keyType)] = args[2].asType(valueType)

        return@create null
    },
)

internal val mapBuiltinFunctions = listOf(
    keysFunction,
    valuesFunction,
    containsKeyFunction,
    containsValueFunction,
    removeFunction,
    putFunction,
)
