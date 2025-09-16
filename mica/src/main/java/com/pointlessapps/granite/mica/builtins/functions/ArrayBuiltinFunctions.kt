package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asNumberType
import com.pointlessapps.granite.mica.mapper.asString
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType
import com.pointlessapps.granite.mica.runtime.resolver.AnyComparator
import com.pointlessapps.granite.mica.runtime.resolver.compareTo
import kotlin.math.max
import kotlin.math.min

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = IntType,
    execute = { _, args -> VariableType.Value(args[0].value.asArrayType().size.toLong()) },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val list = args[0].value.asArrayType()
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function remove expects $elementType as a first argument",
            )
        }

        return@create VariableType.Value(list.remove(args[1].value))
    },
)

private val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    getReturnType = { _, args -> (args[0] as ArrayType).elementType },
    execute = { _, args ->
        val list = args[0].value.asArrayType()
        val index = args[1].value.asIntType()
        return@create VariableType.Value(list.removeAt(index.toInt()))
    },
)

private val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[2].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function insertAt expects $elementType as a second argument",
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
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function insert expects $elementType as a first argument",
            )
        }

        list.add(args[1].value)
        return@create VariableType.Undefined
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function contains expects $elementType as a first argument",
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
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = IntType,
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function indexOf expects $elementType as a first argument",
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
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    returnType = UndefinedType,
    execute = { _, args ->
        args[0].value.asArrayType().sortWith(AnyComparator)
        return@create VariableType.Undefined
    },
)

private val sortedFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sorted",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    getReturnType = { _, args -> args[0] },
    execute = { _, args ->
        VariableType.Value(args[0].value.asArrayType().sortedWith(AnyComparator))
    },
)

private val minFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "min",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(NumberType))),
    getReturnType = { _, args ->
        args[0].superTypes.filterIsInstance<ArrayType>().first().elementType
    },
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        var min = Double.MAX_VALUE
        list.forEach { min = min(min, it.asNumberType().toDouble()) }

        return@create VariableType.Value(min)
    },
)

private val minOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "minOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(NumberType), vararg = true)),
    getReturnType = { _, args -> args.commonSupertype() },
    execute = { _, args ->
        var min = Double.MAX_VALUE
        args.forEach { min = min(min, it.value.asNumberType().toDouble()) }

        return@create VariableType.Value(min)
    },
)

private val maxFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "max",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(NumberType))),
    getReturnType = { _, args ->
        args[0].superTypes.filterIsInstance<ArrayType>().first().elementType
    },
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        var max = Double.MIN_VALUE
        list.forEach { max = max(max, it.asNumberType().toDouble()) }

        return@create VariableType.Value(max)
    },
)

private val maxOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "maxOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(NumberType), vararg = true)),
    getReturnType = { _, args -> args.commonSupertype() },
    execute = { _, args ->
        var max = Double.MIN_VALUE
        args.forEach { max = max(max, it.value.asNumberType().toDouble()) }

        return@create VariableType.Value(max)
    },
)

private val joinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "join",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = StringType,
    execute = { _, args ->
        VariableType.Value(
            args[0].value.asArrayType().joinToString(
                transform = Any?::asString,
            ),
        )
    },
)

private val joinWithSeparatorFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "join",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = StringType,
    execute = { _, args ->
        VariableType.Value(
            args[0].value.asArrayType().joinToString(
                separator = args[1].value.asStringType(),
                transform = Any?::asString,
            ),
        )
    },
)

private val deppJoinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "deepJoin",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = StringType,
    execute = { _, args ->
        fun Any?.join(): String {
            return if (this.toType().isSubtypeOf(EmptyArrayType)) {
                asArrayType().joinToString(transform = Any?::join)
            } else {
                asString()
            }
        }

        VariableType.Value(args[0].value.asArrayType().join())
    },
)

private val deepJoinWithSeparatorFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "deepJoin",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = StringType,
    execute = { _, args ->
        val separator = args[1].value.asStringType()
        fun Any?.join(): String {
            return if (this.toType().isSubtypeOf(EmptyArrayType)) {
                asArrayType().joinToString(separator = separator, transform = Any?::join)
            } else {
                asString()
            }
        }

        VariableType.Value(args[0].value.asArrayType().join())
    },
)

private val arrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "array",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    getReturnType = { _, args -> ArrayType(args[1]) },
    execute = { _, args ->
        VariableType.Value(MutableList(args[0].value.asIntType().toInt()) { args[1].value })
    },
)

private val fillFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "fill",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val list = args[0].value.asArrayType() as MutableList<Any?>
        val elementType = (list.toType() as ArrayType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function fill expects $elementType as a first argument",
            )
        }

        list.fill(args[1].value)
        return@create VariableType.Undefined
    },
)

internal val arrayBuiltinFunctions = listOf(
    lengthFunction,
    removeFunction,
    removeAtFunction,
    insertAtFunction,
    insertFunction,
    containsFunction,
    indexOfFunction,
    sortFunction,
    sortedFunction,
    minFunction,
    minOfFunction,
    maxFunction,
    maxOfFunction,
    joinFunction,
    joinWithSeparatorFunction,
    deppJoinFunction,
    deepJoinWithSeparatorFunction,
    arrayFunction,
    fillFunction,
)
