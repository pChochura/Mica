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
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val toTypeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "to",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { typeArg, args ->
        if (typeArg == null) {
            throw IllegalStateException("Function to requires a type argument")
        }

        return@create VariableType.Value(args[0].value.asType(typeArg.type, true))
    },
)

private val toIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toInt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntType,
    execute = { _, args -> VariableType.Value(args[0].value.asIntType(true)) },
)

private val toRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toReal",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealType,
    execute = { _, args -> VariableType.Value(args[0].value.asRealType(true)) },
)

private val toBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toBool",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = BoolType,
    execute = { _, args -> VariableType.Value(args[0].value.asBoolType(true)) },
)

private val toCharFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toChar",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharType,
    execute = { _, args -> VariableType.Value(args[0].value.asCharType(true)) },
)

private val toStringFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toString",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { _, args -> VariableType.Value(args[0].value.asStringType(true)) },
)

private val toArrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toArray",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { _, args -> args[0].superTypes.filterIsInstance<ArrayType>().first() },
    execute = { _, args -> VariableType.Value(args[0].value.asArrayType()) },
)

private val toSetFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toSet",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { _, args ->
        SetType(args[0].superTypes.filterIsInstance<ArrayType>().first().elementType)
    },
    execute = { _, args -> VariableType.Value(args[0].value.asSetType(true)) },
)

private val toMapFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toMap",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyMapType)),
    getReturnType = { _, args -> args[0].superTypes.filterIsInstance<MapType>().first() },
    execute = { _, args -> VariableType.Value(args[0].value.asMapType()) },
)

private val toIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toIntRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntRangeType,
    execute = { _, args -> VariableType.Value(args[0].value.asIntRangeType(true)) },
)

private val toRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toRealRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealRangeType,
    execute = { _, args -> VariableType.Value(args[0].value.asRealRangeType(true)) },
)

private val toCharRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toCharRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharRangeType,
    execute = { _, args -> VariableType.Value(args[0].value.asCharRangeType(true)) },
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
