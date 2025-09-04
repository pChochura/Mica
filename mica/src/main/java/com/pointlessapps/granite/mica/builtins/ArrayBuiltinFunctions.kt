package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType
import com.pointlessapps.granite.mica.runtime.resolver.AnyComparator
import com.pointlessapps.granite.mica.runtime.resolver.compareTo

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = IntType,
    execute = { args -> VariableType.Value(args[0].value.asArrayType().size.toLong()) },
)

private val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
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

@Suppress("UNCHECKED_CAST")
private val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
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
                "Function insertAt expects ${elementType.name} as a third argument",
            )
        }

        val index = args[1].value.asIntType()
        list.add(index.toInt(), args[2].value)
        return@create VariableType.Undefined
    },
)

@Suppress("UNCHECKED_CAST")
private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
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
                "Function insert expects ${elementType.name} as a second argument",
            )
        }

        list.add(args[1].value)
        return@create VariableType.Undefined
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
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
                "Function contains expects ${elementType.name} as a second argument",
            )
        }

        return@create VariableType.Value(
            list.firstOrNull { it.compareTo(args[1].value.asType(it.toType())) == 0 } != null,
        )
    },
)

private val indexOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "indexOf",
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
                "Function indexOf expects ${elementType.name} as a second argument",
            )
        }

        return@create VariableType.Value(
            list.indexOfFirst { it.compareTo(args[1].value.asType(it.toType())) == 0 }.toLong(),
        )
    },
)

@Suppress("UNCHECKED_CAST")
private val sortFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sort",
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    returnType = UndefinedType,
    execute = { args ->
        args[0].value.asArrayType().sortWith(AnyComparator)
        return@create VariableType.Undefined
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
)
