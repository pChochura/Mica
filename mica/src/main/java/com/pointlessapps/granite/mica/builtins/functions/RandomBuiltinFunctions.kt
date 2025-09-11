package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType
import kotlin.random.Random
import kotlin.random.nextLong

private var random: Random = Random

private val setSeedFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "setSeed",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(IntType)),
    returnType = UndefinedType,
    execute = { args ->
        random = Random(args[0].value.asIntType())

        return@create VariableType.Undefined
    },
)

private val randomIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(IntType)),
    returnType = IntType,
    execute = { args -> VariableType.Value(random.nextLong(args[0].value.asIntType())) },
)

private val randomIntFromFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    returnType = IntType,
    execute = { args ->
        VariableType.Value(random.nextLong(args[0].value.asIntType(), args[1].value.asIntType()))
    },
)

private val randomIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomInt",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.EXACT_MATCH.of(IntRangeType)),
    returnType = IntType,
    execute = { args -> VariableType.Value(random.nextLong(args[0].value.asIntRangeType())) },
)

private val randomRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(RealType)),
    returnType = RealType,
    execute = { args -> VariableType.Value(random.nextDouble(args[0].value.asRealType())) },
)

private val randomRealFromFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(RealType),
        Resolver.SUBTYPE_MATCH.of(RealType),
    ),
    returnType = RealType,
    execute = { args ->
        VariableType.Value(
            random.nextDouble(
                args[0].value.asRealType(),
                args[1].value.asRealType()
            )
        )
    },
)

private val randomRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomReal",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { args ->
        val range = args[0].value.asRealRangeType()
        return@create VariableType.Value(random.nextDouble(range.start, range.endInclusive))
    },
)

private val randomBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "randomBool",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = emptyList(),
    returnType = BoolType,
    execute = { args -> VariableType.Value(random.nextBoolean()) },
)

private val randomArrayElementFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "random",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0].superTypes.filterIsInstance<ArrayType>().first().elementType },
    execute = { args -> VariableType.Value(args[0].value.asArrayType().random(random)) },
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
