package com.pointlessapps.granite.mica.builtins.functions

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
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.GenericType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.resolver.AnyComparator
import com.pointlessapps.granite.mica.runtime.resolver.compareTo

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = IntType,
    execute = { _, args -> args[0].asArrayType().size.toLong() },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val list = args[0].asArrayType()
        return@create list.remove(args[1])
    },
)

private val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    getReturnType = { typeArg, args -> typeArg ?: UndefinedType },
    execute = { _, args ->
        val list = args[0].asArrayType()
        val index = args[1].asIntType()
        return@create list.removeAt(index.toInt())
    },
)

private val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val list = args[0].asArrayType() as MutableList<Any?>
        val index = args[1].asIntType()
        list.add(index.toInt(), args[2])
        return@create null
    },
)

private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val list = args[0].asArrayType() as MutableList<Any?>
        list.add(args[1])
        return@create null
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val list = args[0].asArrayType() as MutableList<Any?>
        return@create list.firstOrNull { it.compareTo(args[1].asType(it.toType())) == 0 } != null
    },
)

private val indexOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "indexOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = IntType,
    execute = { _, args ->
        val list = args[0].asArrayType() as MutableList<Any?>
        return@create list.indexOfFirst { it.compareTo(args[1].asType(it.toType())) == 0 }.toLong()
    },
)

private val sortFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sort",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    returnType = UndefinedType,
    execute = { _, args ->
        args[0].asArrayType().sortWith(AnyComparator)
        return@create null
    },
)

private val sortedFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sorted",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    getReturnType = { _, args -> args[0] },
    execute = { _, args -> args[0].asArrayType().sortedWith(AnyComparator) },
)

private val minFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "min",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = NumberType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(GenericType(NumberType)))),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { _, args ->
        val list = args.first().asArrayType()
        var min = list.first().asNumberType()
        list.forEach {
            val number = it.asNumberType()
            if (number.compareTo(min) < 0) {
                min = number
            }
        }

        return@create min
    },
)

private val minOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "minOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = NumberType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(
            type = ArrayType(GenericType(NumberType)),
            vararg = true,
        ),
    ),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { _, args ->
        val list = args.first().asArrayType()
        var min = list.first().asNumberType()
        list.forEach {
            val number = it.asNumberType()
            if (number.compareTo(min) < 0) {
                min = number
            }
        }

        return@create min
    },
)

private val maxFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "max",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = NumberType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(ArrayType(GenericType(NumberType)))),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { _, args ->
        val list = args.first().asArrayType()
        var max = list.first().asNumberType()
        list.forEach {
            val number = it.asNumberType()
            if (number.compareTo(max) > 0) {
                max = number
            }
        }

        return@create max
    },
)

private val maxOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "maxOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = NumberType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(
            type = ArrayType(GenericType(NumberType)),
            vararg = true,
        ),
    ),
    getReturnType = { typeArg, _ -> typeArg ?: UndefinedType },
    execute = { _, args ->
        val list = args.first().asArrayType()
        var max = list.first().asNumberType()
        list.forEach {
            val number = it.asNumberType()
            if (number.compareTo(max) > 0) {
                max = number
            }
        }

        return@create max
    },
)

private val joinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "join",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = StringType,
    execute = { _, args -> args[0].asArrayType().joinToString(transform = Any?::asString) },
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
        args[0].asArrayType().joinToString(
            separator = args[1].asStringType(),
            transform = Any?::asString,
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

        args[0].asArrayType().join()
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
        val separator = args[1].asStringType()
        fun Any?.join(): String {
            return if (this.toType().isSubtypeOf(EmptyArrayType)) {
                asArrayType().joinToString(separator = separator, transform = Any?::join)
            } else {
                asString()
            }
        }

        args[0].asArrayType().join()
    },
)

private val arrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "array",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(EmptyGenericType),
    ),
    getReturnType = { typeArg, args -> typeArg?.let(::ArrayType) ?: UndefinedType },
    execute = { typeArg, args ->
        val convertedValue = args[1].asType(typeArg ?: AnyType)
        MutableList(args[0].asIntType().toInt()) { convertedValue }
    },
)

private val fillFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "fill",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(ArrayType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = UndefinedType,
    execute = { typeArg, args ->
        val list = args[0].asArrayType() as MutableList<Any?>
        list.fill(args[1].asType(typeArg ?: AnyType))
        return@create null
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
