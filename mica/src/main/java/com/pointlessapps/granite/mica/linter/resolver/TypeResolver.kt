package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.IfConditionExpression
import com.pointlessapps.granite.mica.ast.expressions.InterpolatedStringExpression
import com.pointlessapps.granite.mica.ast.expressions.MapLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.MapTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.SetLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeCoercionExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.helper.inferTypeParameter
import com.pointlessapps.granite.mica.helper.isTypeParameter
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

/**
 * Resolves the type of an expression. If the expression type is not resolvable,
 * reports and error and returns [UndefinedType].
 */
internal class TypeResolver(private val scope: Scope) {
    private val expressionTypes = mutableMapOf<Expression, Type?>()

    fun resolveExpressionType(expression: Expression): Type {
        if (expressionTypes.containsKey(expression)) {
            return expressionTypes[expression]!!
        }

        val type = when (expression) {
            is BooleanLiteralExpression -> BoolType
            is CharLiteralExpression -> CharType
            is StringLiteralExpression -> StringType
            is InterpolatedStringExpression -> StringType
            is NumberLiteralExpression -> when (expression.token.type) {
                Token.NumberLiteral.Type.Real, Token.NumberLiteral.Type.Exponent -> RealType
                else -> IntType
            }

            is IfConditionExpression -> resolveIfConditionExpressionType(expression)
            is TypeCoercionExpression -> resolveTypeExpression(expression.typeExpression)
            is MemberAccessExpression -> resolveMemberAccessType(expression)
            is ArrayLiteralExpression -> resolveArrayLiteralExpressionType(expression)
            is SetLiteralExpression -> resolveSetLiteralExpressionType(expression)
            is MapLiteralExpression -> resolveMapLiteralExpressionType(expression)
            is TypeExpression -> resolveTypeExpression(expression)
            is ParenthesisedExpression -> resolveExpressionType(expression.expression)
            is SymbolExpression -> resolveSymbolType(expression.token)
            is FunctionCallExpression -> resolveFunctionCallExpressionType(expression)
            is BinaryExpression -> resolveBinaryExpressionType(expression)
            is UnaryExpression -> resolveUnaryExpressionType(expression)
            is AffixAssignmentExpression -> resolveAffixAssignmentExpressionType(expression)
        }

        expressionTypes[expression] = type

        return type
    }

    private fun resolveIfConditionExpressionType(expression: IfConditionExpression): Type {
        if (expression.elseDeclaration == null) {
            scope.addError(
                message = "If condition expression must be exhaustive (include an else declaration)",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        val flattenExpressions = listOf(expression.ifConditionDeclaration.ifConditionExpression) +
                expression.elseIfConditionDeclarations?.map { it.elseIfConditionExpression }
                    .orEmpty()

        flattenExpressions.forEach {
            val type = resolveExpressionType(it)
            if (!type.isSubtypeOf(BoolType)) {
                scope.addError(
                    message = "Type of the expression (${type.name}) doesn't resolve to a bool",
                    token = it.startingToken,
                )
            }
        }

        val resultTypes = mutableListOf<Type>()

        val lastIfBodyStatement = expression.ifConditionDeclaration.ifBody.statements.lastOrNull()
        if (lastIfBodyStatement !is ExpressionStatement) {
            scope.addError(
                message = "Last statement in the if body must be an expression",
                token = lastIfBodyStatement?.startingToken
                    ?: expression.ifConditionDeclaration.ifToken,
            )
        } else {
            resultTypes.add(resolveExpressionType(lastIfBodyStatement.expression))
        }

        expression.elseIfConditionDeclarations?.forEach {
            val lastStatement = it.elseIfBody.statements.lastOrNull()
            if (lastStatement !is ExpressionStatement) {
                scope.addError(
                    message = "Last statement in the else if body must be an expression",
                    token = lastStatement?.startingToken
                        ?: it.elseIfToken.first,
                )
            } else {
                resultTypes.add(resolveExpressionType(lastStatement.expression))
            }
        }

        val lastElseBodyStatement = expression.elseDeclaration.elseBody.statements.lastOrNull()
        if (lastElseBodyStatement !is ExpressionStatement) {
            scope.addError(
                message = "Last statement in the else body must be an expression",
                token = lastElseBodyStatement?.startingToken
                    ?: expression.elseDeclaration.elseToken,
            )
        } else {
            resultTypes.add(resolveExpressionType(lastElseBodyStatement.expression))
        }

        return resultTypes.commonSupertype()
    }

    private fun resolveMemberAccessType(expression: MemberAccessExpression): Type {
        var type = resolveExpressionType(expression.symbolExpression)
            .takeIf { it != UndefinedType } ?: return UndefinedType

        expression.accessorExpressions.forEach {
            type = when (it) {
                is ArrayIndexAccessorExpression ->
                    type.resolveArrayIndexAccessorExpressionType(it)

                is PropertyAccessAccessorExpression ->
                    type.resolvePropertyAccessAccessorExpressionType(it)
            }
        }

        return type
    }

    private fun Type.resolveArrayIndexAccessorExpressionType(
        expression: ArrayIndexAccessorExpression,
    ): Type {
        if (!isSubtypeOfAny(EmptyArrayType, EmptyMapType)) {
            scope.addError(
                message = "Can only index into arrays and maps, got $name",
                token = expression.openBracketToken,
            )

            return UndefinedType
        }

        val indexExpressionType = resolveExpressionType(expression.indexExpression)
        if (isSubtypeOf(EmptyArrayType)) {
            if (!indexExpressionType.isSubtypeOf(IntType)) {
                scope.addError(
                    message = "Array index must be of type int, got ${indexExpressionType.name}",
                    token = expression.indexExpression.startingToken,
                )

                return UndefinedType
            }

            return superTypes.filterIsInstance<ArrayType>().first().elementType
        } else if (isSubtypeOf(EmptyMapType)) {
            val map = superTypes.filterIsInstance<MapType>().first()
            if (!indexExpressionType.isSubtypeOf(map.keyType)) {
                scope.addError(
                    message = "Map index must be of type ${
                        map.keyType.name
                    }, got ${indexExpressionType.name}",
                    token = expression.indexExpression.startingToken,
                )

                return UndefinedType
            }

            return map.valueType
        }

        return UndefinedType
    }

    private fun Type.resolvePropertyAccessAccessorExpressionType(
        expression: PropertyAccessAccessorExpression,
    ): Type {
        val property = scope.getMatchingTypeProperty(this, expression.propertySymbolToken.value)
        if (property == null) {
            scope.addError(
                message = "Property ${
                    expression.propertySymbolToken.value
                } does not exist on type $name",
                token = expression.propertySymbolToken,
            )

            return UndefinedType
        }

        return property
    }

    private fun resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
        is ArrayTypeExpression -> ArrayType(resolveExpressionType(expression.typeExpression))
        is SetTypeExpression -> SetType(resolveExpressionType(expression.typeExpression))
        is MapTypeExpression -> MapType(
            keyType = resolveExpressionType(expression.keyTypeExpression),
            valueType = resolveExpressionType(expression.valueTypeExpression),
        )

        is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
            ?: scope.getType(expression.symbolToken.value) ?: let {
                scope.addError(
                    message = "Type ${expression.symbolToken.value} is not declared",
                    token = expression.startingToken,
                )

                UndefinedType
            }
    }

    private fun resolveAffixAssignmentExpressionType(expression: AffixAssignmentExpression): Type {
        val type = resolveMemberAccessType(
            MemberAccessExpression(
                symbolExpression = SymbolExpression(expression.symbolToken),
                accessorExpressions = expression.accessorExpressions,
            ),
        )

        if (!type.isSubtypeOf(IntType)) {
            scope.addError(
                message = "Expression must be of type int to use the affix operator",
                token = expression.symbolToken,
            )

            return UndefinedType
        }

        return type
    }

    private fun resolveArrayLiteralExpressionType(expression: ArrayLiteralExpression): Type {
        if (expression.elements.isEmpty()) return EmptyArrayType

        return ArrayType(expression.elements.map(::resolveExpressionType).commonSupertype())
    }

    private fun resolveSetLiteralExpressionType(expression: SetLiteralExpression): Type {
        if (expression.elements.isEmpty()) return EmptySetType

        return SetType(expression.elements.map(::resolveExpressionType).commonSupertype())
    }

    private fun resolveMapLiteralExpressionType(expression: MapLiteralExpression): Type {
        if (expression.keyValuePairs.isEmpty()) return EmptyMapType

        return MapType(
            keyType = expression.keyValuePairs.map {
                resolveExpressionType(it.keyExpression)
            }.commonSupertype(),
            valueType = expression.keyValuePairs.map {
                resolveExpressionType(it.valueExpression)
            }.commonSupertype(),
        )
    }

    private fun resolveSymbolType(symbol: Token.Symbol): Type {
        val variableType = scope.getVariable(symbol.value)
        if (variableType == null || variableType is UndefinedType) {
            scope.addError(
                message = "Symbol ${symbol.value} is not defined",
                token = symbol,
            )

            return UndefinedType
        }

        return variableType
    }

    private fun resolveFunctionCallExpressionType(expression: FunctionCallExpression): Type {
        scope.declareGenericType()
        return resolveFunctionCallExpressionTypeImpl(expression).also {
            scope.undeclareGenericType()
        }
    }

    private fun resolveFunctionCallExpressionTypeImpl(expression: FunctionCallExpression): Type {
        var typeArgument = expression.typeArgument?.let(::resolveTypeExpression)
        val argumentTypes = expression.arguments.map(::resolveExpressionType)
        val function = scope.getMatchingFunctionDeclaration(
            name = expression.nameToken.value,
            arguments = argumentTypes,
        )

        if (function == null) {
            val possibleOverloads = scope.getFunctionOverloadsSignatures(expression.nameToken.value)
            scope.addError(
                message = buildString {
                    append(
                        "Function ${
                            getSignature(
                                name = expression.nameToken.value,
                                parameters = argumentTypes,
                                isMember = expression.isMemberFunctionCall,
                                isVararg = false,
                            )
                        } is not declared",
                    )
                    if (possibleOverloads.isNotEmpty()) {
                        append(". Possible overloads: ${possibleOverloads.joinToString()}")
                    }
                },
                token = expression.startingToken,
            )

            return UndefinedType
        }

        if (expression.isMemberFunctionCall && !function.accessType.allowMemberFunctionCalls()) {
            scope.addError(
                message = "Function ${expression.nameToken.value} cannot be called as a member function",
                token = expression.startingToken,
            )

            return UndefinedType
        } else if (!expression.isMemberFunctionCall && function.accessType == FunctionOverload.AccessType.MEMBER_ONLY) {
            scope.addError(
                message = "Function ${expression.nameToken.value} has to be called as a member function",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        if (function.typeParameterConstraint != null) {
            val argumentsToInfer = mutableListOf<Type?>()
            function.parameters.forEachIndexed { index, parameter ->
                if (parameter.type.isTypeParameter()) {
                    argumentsToInfer.add(
                        parameter.type.inferTypeParameter(
                            if (parameter.vararg) {
                                ArrayType(
                                    argumentTypes.subList(index, argumentTypes.size)
                                        .commonSupertype()
                                )
                            } else {
                                argumentTypes[index]
                            },
                        ),
                    )
                }
            }

            if (argumentsToInfer.any { it == null }) {
                scope.addError(
                    message = "Couldn't infer the type argument for the function",
                    token = expression.openBracketToken,
                )

                return UndefinedType
            }

            if (typeArgument == null && argumentsToInfer.isEmpty()) {
                scope.addError(
                    message = "Function ${expression.nameToken.value} requires a type argument",
                    token = expression.openBracketToken,
                )

                return UndefinedType
            }

            val commonSupertype = argumentsToInfer.filterNotNull().commonSupertype()
            if (argumentsToInfer.isNotEmpty() && !commonSupertype.isSubtypeOf(function.typeParameterConstraint)) {
                scope.addError(
                    message = "Type argument mismatch: expected ${
                        function.typeParameterConstraint.name
                    }, got ${commonSupertype.name}",
                    token = expression.openBracketToken,
                )

                return UndefinedType
            }

            if (typeArgument != null && !typeArgument.isSubtypeOf(function.typeParameterConstraint)) {
                scope.addError(
                    message = "Type argument mismatch: expected ${
                        function.typeParameterConstraint.name
                    }, got ${typeArgument.name}",
                    token = expression.typeArgument?.startingToken
                        ?: expression.openBracketToken,
                )

                return UndefinedType
            } else if (
                typeArgument != null &&
                argumentsToInfer.isNotEmpty() &&
                !commonSupertype.isSubtypeOf(typeArgument)
            ) {
                scope.addError(
                    message = "Inferred type argument mismatch: expected: ${
                        typeArgument.name
                    }, got: ${commonSupertype.name}",
                    token = expression.openBracketToken,
                )

                return UndefinedType
            }

            typeArgument = typeArgument ?: commonSupertype
        }

        if (typeArgument != null && function.typeParameterConstraint == null) {
            scope.addError(
                message = "Function ${expression.nameToken.value} cannot have a type argument",
                token = requireNotNull(expression.typeArgument).startingToken,
            )

            return UndefinedType
        }

        return function.getReturnType(typeArgument, argumentTypes)
    }

    private fun resolveBinaryExpressionType(expression: BinaryExpression): Type {
        val lhsType = resolveExpressionType(expression.lhs)
        val rhsType = resolveExpressionType(expression.rhs)

        val resolvedType = TypeOperationResolver.resolveBinaryOperator(
            lhs = lhsType,
            rhs = rhsType,
            operator = expression.operatorToken,
        )

        if (resolvedType == null) {
            scope.addError(
                message = "Operator ${
                    expression.operatorToken.type.literal
                } is not applicable to ${lhsType.name} and ${rhsType.name}",
                token = expression.operatorToken,
            )

            return UndefinedType
        }

        return resolvedType
    }

    private fun resolveUnaryExpressionType(expression: UnaryExpression): Type {
        val rhsType = resolveExpressionType(expression.rhs)

        val resolvedType = TypeOperationResolver.resolvePrefixUnaryOperator(
            rhs = rhsType,
            operator = expression.operatorToken,
        )

        if (resolvedType == null) {
            scope.addError(
                message = "Operator ${
                    expression.operatorToken.type.literal
                } is not applicable to $rhsType",
                token = expression.operatorToken,
            )

            return UndefinedType
        }

        return resolvedType
    }
}
