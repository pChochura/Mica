package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.UndefinedType
import kotlin.random.Random
import kotlin.random.nextLong

private var random: Random = Random

private val setSeedFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "setSeed",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(IntType)),
    returnType = UndefinedType,
    execute = { _, args ->
        random = Random(args[0].asIntType())

        return@create null
    },
)

private val randomIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(IntType)),
    returnType = IntType,
    execute = { _, args -> random.nextLong(args[0].asIntType()) },
)

private val randomIntFromFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    returnType = IntType,
    execute = { _, args -> random.nextLong(args[0].asIntType(), args[1].asIntType()) },
)

private val randomIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(IntRangeType)),
    returnType = IntType,
    execute = { _, args -> random.nextLong(args[0].asIntRangeType()) },
)

private val randomRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(RealType)),
    returnType = RealType,
    execute = { _, args -> random.nextDouble(args[0].asRealType()) },
)

private val randomRealFromFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(RealType),
        Resolver.SUBTYPE_MATCH.of(RealType),
    ),
    returnType = RealType,
    execute = { _, args -> random.nextDouble(args[0].asRealType(), args[1].asRealType()) },
)

private val randomRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { _, args ->
        val range = args[0].asRealRangeType()
        return@create random.nextDouble(range.start, range.endInclusive)
    },
)

private val randomBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomBool",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = emptyList(),
    returnType = BoolType,
    execute = { _, args -> random.nextBoolean() },
)

private val randomArrayElementFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "random",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType))),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { _, args -> args[0].asArrayType().random(random) },
)

internal val randomBuiltinFunctions = listOf(
    setSeedFunction,
    randomIntFunction,
    randomIntFromFunction,
    randomIntRangeFunction,
    randomRealFunction,
    randomRealFromFunction,
    randomRealRangeFunction,
    randomBoolFunction,
    randomArrayElementFunction,
)
