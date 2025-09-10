package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType
import com.pointlessapps.granite.mica.runtime.resolver.AnyComparator
import com.pointlessapps.granite.mica.runtime.resolver.compareTo

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = IntType,
    execute = { args -> VariableType.Value(args[0].value.asArrayType().size.toLong()) },
)

private val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    getReturnType = { (it[0] as ArrayType).elementType },
    execute = { args ->
        val list = args[0].value.asArrayType()
        val index = args[1].value.asIntType()
        return@create VariableType.Value(list.removeAt(index.toInt()))
    },
)

private val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[2].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function insertAt expects ${elementType.name} as a second argument",
            )
        }

        val index = args[1].value.asIntType()
        list.add(index.toInt(), args[2].value)
        return@create VariableType.Undefined
    },
)

private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function insert expects ${elementType.name} as a first argument",
            )
        }

        list.add(args[1].value)
        return@create VariableType.Undefined
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = BoolType,
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function contains expects ${elementType.name} as a first argument",
            )
        }

        return@create VariableType.Value(
            list.firstOrNull { it.compareTo(args[1].value.asType(it.toType())) == 0 } != null,
        )
    },
)

private val indexOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "indexOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = IntType,
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function indexOf expects ${elementType.name} as a first argument",
            )
        }

        return@create VariableType.Value(
            list.indexOfFirst { it.compareTo(args[1].value.asType(it.toType())) == 0 }.toLong(),
        )
    },
)

private val sortFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sort",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    returnType = UndefinedType,
    execute = { args ->
        args[0].value.asArrayType().sortWith(AnyComparator)
        return@create VariableType.Undefined
    },
)

private val sortedFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sorted",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0] },
    execute = { args -> VariableType.Value(args[0].value.asArrayType().sortedWith(AnyComparator)) },
)

private val minFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "min",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0].superTypes.filterIsInstance<ArrayType>().first().elementType },
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!elementType.isSubtypeOfAny(IntType, RealType)) {
            throw IllegalArgumentException(
                "Function min is not applicable to [${elementType.name}]",
            )
        }

        return@create if (elementType.isSubtypeOf(IntType)) {
            VariableType.Value(list.minOf(Any?::asIntType))
        } else {
            VariableType.Value(list.minOf(Any?::asRealType))
        }
    },
)

private val maxFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "max",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0].superTypes.filterIsInstance<ArrayType>().first().elementType },
    execute = { args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!elementType.isSubtypeOfAny(IntType, RealType)) {
            throw IllegalArgumentException(
                "Function max is not applicable to [${elementType.name}]",
            )
        }

        return@create if (elementType.isSubtypeOf(IntType)) {
            VariableType.Value(list.maxOf(Any?::asIntType))
        } else {
            VariableType.Value(list.maxOf(Any?::asRealType))
        }
    },
)

private val joinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "join",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = StringType,
    execute = { VariableType.Value(it[0].value.asArrayType().joinToString("")) },
)

private val joinWithDelimiterFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "join",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(ArrayType(StringType)),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = StringType,
    execute = {
        VariableType.Value(
            it[0].value.asArrayType().joinToString(
                separator = it[1].value.asStringType(),
                transform = Any?::asStringType,
            ),
        )
    },
)

internal val arrayBuiltinFunctions = listOf(
    lengthFunction,
    removeAtFunction,
    insertAtFunction,
    insertFunction,
    containsFunction,
    indexOfFunction,
    sortFunction,
    sortedFunction,
    minFunction,
    maxFunction,
    joinFunction,
    joinWithDelimiterFunction,
)
