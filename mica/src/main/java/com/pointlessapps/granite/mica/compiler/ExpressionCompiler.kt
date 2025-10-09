package com.pointlessapps.granite.mica.compiler

import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.IfConditionExpression
import com.pointlessapps.granite.mica.ast.expressions.InterpolatedStringExpression
import com.pointlessapps.granite.mica.ast.expressions.MapLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.SetLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeCoercionExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.compiler.errors.CompileTimeException
import com.pointlessapps.granite.mica.compiler.helper.toIntNumber
import com.pointlessapps.granite.mica.compiler.helper.toRealNumber
import com.pointlessapps.granite.mica.compiler.helper.uniqueId
import com.pointlessapps.granite.mica.compiler.model.AccessBuiltinProperty
import com.pointlessapps.granite.mica.compiler.model.AccessIndex
import com.pointlessapps.granite.mica.compiler.model.BinaryAdd
import com.pointlessapps.granite.mica.compiler.model.BinaryAnd
import com.pointlessapps.granite.mica.compiler.model.BinaryDivide
import com.pointlessapps.granite.mica.compiler.model.BinaryEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryExponent
import com.pointlessapps.granite.mica.compiler.model.BinaryGraterThan
import com.pointlessapps.granite.mica.compiler.model.BinaryGraterThanOrEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryLessThan
import com.pointlessapps.granite.mica.compiler.model.BinaryLessThanOrEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryModulo
import com.pointlessapps.granite.mica.compiler.model.BinaryMultiply
import com.pointlessapps.granite.mica.compiler.model.BinaryNotEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryOr
import com.pointlessapps.granite.mica.compiler.model.BinaryRange
import com.pointlessapps.granite.mica.compiler.model.BinarySubtract
import com.pointlessapps.granite.mica.compiler.model.Call
import com.pointlessapps.granite.mica.compiler.model.CallBuiltin
import com.pointlessapps.granite.mica.compiler.model.CastAs
import com.pointlessapps.granite.mica.compiler.model.CompilerContext
import com.pointlessapps.granite.mica.compiler.model.CompilerInstruction
import com.pointlessapps.granite.mica.compiler.model.Dup
import com.pointlessapps.granite.mica.compiler.model.Jump
import com.pointlessapps.granite.mica.compiler.model.JumpIfFalse
import com.pointlessapps.granite.mica.compiler.model.JumpIfTrue
import com.pointlessapps.granite.mica.compiler.model.Label
import com.pointlessapps.granite.mica.compiler.model.Load
import com.pointlessapps.granite.mica.compiler.model.NewArray
import com.pointlessapps.granite.mica.compiler.model.NewMap
import com.pointlessapps.granite.mica.compiler.model.NewSet
import com.pointlessapps.granite.mica.compiler.model.Pop
import com.pointlessapps.granite.mica.compiler.model.Push
import com.pointlessapps.granite.mica.compiler.model.Rot2
import com.pointlessapps.granite.mica.compiler.model.Rot3
import com.pointlessapps.granite.mica.compiler.model.Store
import com.pointlessapps.granite.mica.compiler.model.StoreAtIndex
import com.pointlessapps.granite.mica.compiler.model.UnaryAdd
import com.pointlessapps.granite.mica.compiler.model.UnaryNot
import com.pointlessapps.granite.mica.compiler.model.UnarySubtract
import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.helper.inferTypeParameter
import com.pointlessapps.granite.mica.helper.isTypeParameter
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.UndefinedType

internal fun Expression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = when (this) {
    is ParenthesisedExpression -> expression.unfoldExpression(context, keepReturnValue)

    is BooleanLiteralExpression -> unfoldExpression(keepReturnValue)
    is CharLiteralExpression -> unfoldExpression(keepReturnValue)
    is NumberLiteralExpression -> unfoldExpression(keepReturnValue)
    is StringLiteralExpression -> unfoldExpression(keepReturnValue)
    is SymbolExpression -> unfoldExpression(keepReturnValue)
    is ArrayLiteralExpression -> unfoldExpression(context, keepReturnValue)
    is SetLiteralExpression -> unfoldExpression(context, keepReturnValue)
    is MapLiteralExpression -> unfoldExpression(context, keepReturnValue)
    is InterpolatedStringExpression -> unfoldExpression(context, keepReturnValue)

    is UnaryExpression -> unfoldExpression(context, keepReturnValue)
    is BinaryExpression -> unfoldExpression(context, keepReturnValue)

    is AffixAssignmentExpression -> unfoldExpression(context, keepReturnValue)
    is FunctionCallExpression -> unfoldExpression(context, keepReturnValue)
    is IfConditionExpression -> unfoldExpression(context, keepReturnValue)
    is MemberAccessExpression -> unfoldExpression(context, keepReturnValue)
    is TypeCoercionExpression -> unfoldExpression(context, keepReturnValue)

    is TypeExpression -> unfoldExpression(context, keepReturnValue)
}

private fun BooleanLiteralExpression.unfoldExpression(keepReturnValue: Boolean): List<Push> =
    if (keepReturnValue) listOf(Push(this.token.value.toBooleanStrict())) else emptyList()

private fun CharLiteralExpression.unfoldExpression(keepReturnValue: Boolean): List<Push> =
    if (keepReturnValue) listOf(Push(this.token.value)) else emptyList()

private fun NumberLiteralExpression.unfoldExpression(keepReturnValue: Boolean): List<Push> =
    if (keepReturnValue) {
        listOf(
            Push(
                when (token.type) {
                    Token.NumberLiteral.Type.Real,
                    Token.NumberLiteral.Type.Exponent,
                        -> token.value.toRealNumber()

                    else -> token.value.toIntNumber()
                },
            ),
        )
    } else {
        emptyList()
    }

private fun StringLiteralExpression.unfoldExpression(keepReturnValue: Boolean): List<Push> =
    if (keepReturnValue) listOf(Push(this.token.value)) else emptyList()

private fun SymbolExpression.unfoldExpression(keepReturnValue: Boolean): List<Load> =
    if (keepReturnValue) listOf(Load(this.token.value)) else emptyList()

private fun ArrayLiteralExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    elements.forEach { addAll(it.unfoldExpression(context, keepReturnValue)) }
    if (keepReturnValue) add(NewArray(elements.size))
}

private fun SetLiteralExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    elements.forEach { addAll(it.unfoldExpression(context, keepReturnValue)) }
    if (keepReturnValue) add(NewSet(elements.size))
}

private fun MapLiteralExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    keyValuePairs.forEach {
        addAll(it.valueExpression.unfoldExpression(context, keepReturnValue))
        addAll(it.keyExpression.unfoldExpression(context, keepReturnValue))
    }
    if (keepReturnValue) add(NewMap(keyValuePairs.size))
}

private fun InterpolatedStringExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    expressions.forEach { addAll(it.unfoldExpression(context, keepReturnValue)) }
    if (keepReturnValue && expressions.singleOrNull() !is StringLiteralExpression) {
        add(NewArray(expressions.size))
        add(Push(""))
        add(CallBuiltin("join([any], string)"))
    }
}

private fun UnaryExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    addAll(rhs.unfoldExpression(context, keepReturnValue))
    if (keepReturnValue) {
        add(
            when (operatorToken.type) {
                Token.Operator.Type.Add -> UnaryAdd
                Token.Operator.Type.Subtract -> UnarySubtract
                Token.Operator.Type.Not -> UnaryNot
                else -> throw CompileTimeException {
                    "Unknown unary operator: ${operatorToken.type.literal}"
                }
            },
        )
    }
}

private fun BinaryExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    val skipBranchLabel = "SkipBranch_$uniqueId"
    when (operatorToken.type) {
        Token.Operator.Type.Or -> {
            addAll(lhs.unfoldExpression(context, keepReturnValue = true))
            if (keepReturnValue) add(Dup)
            add(JumpIfTrue(skipBranchLabel))
        }

        Token.Operator.Type.And -> {
            addAll(lhs.unfoldExpression(context, keepReturnValue = true))
            if (keepReturnValue) add(Dup)
            add(JumpIfFalse(skipBranchLabel))
        }

        else -> addAll(lhs.unfoldExpression(context, keepReturnValue))
    }

    addAll(rhs.unfoldExpression(context, keepReturnValue))
    if (keepReturnValue) {
        add(
            when (operatorToken.type) {
                Token.Operator.Type.Add -> BinaryAdd
                Token.Operator.Type.Subtract -> BinarySubtract
                Token.Operator.Type.Multiply -> BinaryMultiply
                Token.Operator.Type.Divide -> BinaryDivide
                Token.Operator.Type.Modulo -> BinaryModulo
                Token.Operator.Type.Exponent -> BinaryExponent
                Token.Operator.Type.Or -> BinaryOr
                Token.Operator.Type.And -> BinaryAnd
                Token.Operator.Type.Equals -> BinaryEquals
                Token.Operator.Type.NotEquals -> BinaryNotEquals
                Token.Operator.Type.GraterThan -> BinaryGraterThan
                Token.Operator.Type.LessThan -> BinaryLessThan
                Token.Operator.Type.GraterThanOrEquals -> BinaryGraterThanOrEquals
                Token.Operator.Type.LessThanOrEquals -> BinaryLessThanOrEquals
                Token.Operator.Type.Range -> BinaryRange
                else -> throw CompileTimeException {
                    "Unknown binary operator: ${operatorToken.type.literal}"
                }
            },
        )
    }
    add(Label(skipBranchLabel))
}

private fun AffixAssignmentExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    val hasAccessors = accessorExpressions.isNotEmpty()
    if (hasAccessors) {
        add(Push(symbolToken.value))
    } else {
        add(Load(symbolToken.value))
    }

    accessorExpressions.forEachIndexed { index, it ->
        // Use the value of the symbol right before the last accessor to have it on the stack
        // to be used in the assignment.
        if (index == accessorExpressions.lastIndex) add(Dup)

        when (it) {
            is PropertyAccessAccessorExpression -> add(Push(it.propertySymbolToken.value))
            is ArrayIndexAccessorExpression ->
                addAll(it.indexExpression.unfoldExpression(context, keepReturnValue = true))
        }
        add(AccessIndex)
    }

    if (keepReturnValue && this@unfoldExpression is PostfixAssignmentExpression) {
        add(Dup)
        if (hasAccessors) add(Rot3)
    }

    add(Push(1L))
    add(if (operatorToken is Token.Increment) BinaryAdd else BinarySubtract)

    if (keepReturnValue && this@unfoldExpression is PrefixAssignmentExpression) {
        add(Dup)
        if (hasAccessors) add(Rot3)
    }

    if (hasAccessors) {
        when (val accessor = accessorExpressions.last()) {
            is PropertyAccessAccessorExpression -> add(Push(accessor.propertySymbolToken.value))
            is ArrayIndexAccessorExpression ->
                addAll(accessor.indexExpression.unfoldExpression(context, keepReturnValue = true))
        }
        add(Rot2)
        add(StoreAtIndex)
    } else {
        add(Store(symbolToken.value))
    }
}

private fun FunctionCallExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    arguments.forEach { addAll(it.unfoldExpression(context, keepReturnValue = true)) }

    val argumentTypes = arguments.map { context.resolveExpressionType(it) }
    val function = context.getMatchingFunctionDeclaration(nameToken.value, argumentTypes)

    val isVararg = function.parameters.lastOrNull()?.vararg == true
    if (isVararg) add(NewArray(arguments.size - function.parameters.size + 1))

    var typeArgument = typeArgument?.let(context::resolveExpressionType)
    if (typeArgument == null && function.typeParameterConstraint != null) {
        val collapsedArguments = if (isVararg) {
            argumentTypes.take(function.parameters.size) +
                    ArrayType(argumentTypes.drop(function.parameters.size).commonSupertype())
        } else {
            argumentTypes
        }

        // At this point this array cannot be empty
        val argumentsToInfer = buildList {
            function.parameters.zip(collapsedArguments).forEach { (parameter, argument) ->
                if (
                    parameter.resolver != FunctionOverload.Parameter.Resolver.EXACT_MATCH &&
                    parameter.type.isTypeParameter()
                ) {
                    parameter.type.inferTypeParameter(argument)?.let(::add)
                }
            }
        }

        typeArgument = argumentsToInfer.commonSupertype()
    }

    if (function.isBuiltin && typeArgument != null) {
        add(Push(typeArgument))
    }

    val signature = getSignature(nameToken.value, function.parameters)
    add(if (function.isBuiltin) CallBuiltin(signature) else Call(signature, typeArgument))
    val hasReturnValue = function.getReturnType(typeArgument, argumentTypes) != UndefinedType
    if (!keepReturnValue && hasReturnValue) add(Pop)
}

private fun IfConditionExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    val ifId = uniqueId
    val branchBaseLabel = "IfBranch_${ifId}_"

    val bodies = buildList {
        add(ifConditionDeclaration.ifConditionExpression to ifConditionDeclaration.ifBody.statements)
        elseIfConditionDeclarations?.let { elseIf ->
            addAll(elseIf.map { it.elseIfConditionExpression to it.elseIfBody.statements })
        }
        elseDeclaration?.let { add(null to it.elseBody.statements) }
    }

    bodies.forEachIndexed { index, (conditionExpression, body) ->
        add(Label("${branchBaseLabel}$index"))
        if (conditionExpression != null) {
            addAll(conditionExpression.unfoldExpression(context, keepReturnValue = true))
            add(JumpIfFalse("${branchBaseLabel}${index + 1}"))
        }

        body.forEachIndexed { index, statement ->
            // If it's an if expression, leave the last expression result on the stack
            if (statement is ExpressionStatement && index == body.lastIndex) {
                addAll(statement.expression.unfoldExpression(context, keepReturnValue))
            } else {
                addAll(traverseStatement(statement, context))
            }
        }
        // Jump out of the loop after executing the body or fallthrough in the last branch
        if (index != bodies.lastIndex) {
            add(Jump("${branchBaseLabel}${bodies.size}"))
        }
    }
    add(Label("${branchBaseLabel}${bodies.size}"))
}

private fun MemberAccessExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    addAll(symbolExpression.unfoldExpression(context, keepReturnValue = true))
    var currentType = context.resolveExpressionType(symbolExpression)
    accessorExpressions.forEach {
        when (it) {
            is PropertyAccessAccessorExpression -> {
                val property = it.propertySymbolToken.value
                add(Push(property))

                val typeProperty = context.getMatchingTypeProperty(currentType, property)
                add(
                    if (typeProperty.isBuiltin) {
                        AccessBuiltinProperty(typeProperty.receiverType)
                    } else {
                        AccessIndex
                    },
                )
            }

            is ArrayIndexAccessorExpression -> {
                addAll(it.indexExpression.unfoldExpression(context, keepReturnValue = true))
                add(AccessIndex)

                currentType = if (currentType.isSubtypeOf(EmptyArrayType)) {
                    currentType.superTypes.filterIsInstance<ArrayType>().first().elementType
                } else {
                    currentType.superTypes.filterIsInstance<MapType>().first().valueType
                }
            }
        }
    }
    if (!keepReturnValue) add(Pop)
}

private fun TypeCoercionExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = buildList {
    addAll(lhs.unfoldExpression(context, keepReturnValue))
    if (keepReturnValue) add(CastAs(context.resolveExpressionType(typeExpression)))
}

private fun TypeExpression.unfoldExpression(
    context: CompilerContext,
    keepReturnValue: Boolean,
): List<CompilerInstruction> = if (keepReturnValue) {
    listOf(Push(context.resolveExpressionType(this)))
} else {
    emptyList()
}
