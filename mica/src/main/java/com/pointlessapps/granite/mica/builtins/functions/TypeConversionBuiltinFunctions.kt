package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asBoolType
import com.pointlessapps.granite.mica.mapper.asCharRangeType
import com.pointlessapps.granite.mica.mapper.asCharType
import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.asSetType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType

private val toTypeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "to",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { typeArg, args ->
        if (typeArg == null) {
            return@create null
        }

        return@create args[0].asType(typeArg, true)
    },
)

private val toIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toInt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntType,
    execute = { _, args -> args[0].asIntType(true) },
)

private val toRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toReal",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealType,
    execute = { _, args -> args[0].asRealType(true) },
)

private val toBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toBool",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = BoolType,
    execute = { _, args -> args[0].asBoolType(true) },
)

private val toCharFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toChar",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharType,
    execute = { _, args -> args[0].asCharType(true) },
)

private val toStringFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toString",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { _, args -> args[0].asStringType(true) },
)

private val toArrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toArray",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType))),
    getReturnType = { typeArg, args -> typeArg?.let(::ArrayType) ?: UndefinedType },
    execute = { typeArg, args -> args[0].asArrayType().map { it.asType(typeArg ?: AnyType) } },
)

private val toSetFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toSet",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType))),
    getReturnType = { typeArg, args -> typeArg?.let(::SetType) ?: UndefinedType },
    execute = { _, args -> args[0].asSetType(true) },
)

private val toMapFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toMap",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyMapType)),
    getReturnType = { _, args -> args[0].superTypes.filterIsInstance<MapType>().first() },
    execute = { _, args -> args[0].asMapType() },
)

private val toIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toIntRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntRangeType,
    execute = { _, args -> args[0].asIntRangeType(true) },
)

private val toRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toRealRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealRangeType,
    execute = { _, args -> args[0].asRealRangeType(true) },
)

private val toCharRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toCharRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharRangeType,
    execute = { _, args -> args[0].asCharRangeType(true) },
)

internal val typeConversionBuiltinFunctions = listOf(
    toTypeFunction,
    toIntFunction,
    toRealFunction,
    toBoolFunction,
    toCharFunction,
    toStringFunction,
    toArrayFunction,
    toSetFunction,
    toMapFunction,
    toIntRangeFunction,
    toRealRangeFunction,
    toCharRangeFunction,
)
