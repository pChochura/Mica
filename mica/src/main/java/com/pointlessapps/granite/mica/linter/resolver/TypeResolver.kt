package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
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
            is NumberLiteralExpression -> when (expression.token.type) {
                Token.NumberLiteral.Type.Real, Token.NumberLiteral.Type.Exponent -> RealType
                else -> IntType
            }

            is ArrayIndexExpression -> resolveArrayIndexExpressionType(expression)
            is ArrayLiteralExpression -> resolveArrayLiteralExpressionType(expression)
            is ArrayTypeExpression -> ArrayType(resolveExpressionType(expression.typeExpression))
            is SymbolTypeExpression -> expression.symbolToken.toType()
            is ParenthesisedExpression -> resolveExpressionType(expression.expression)
            is SymbolExpression -> resolveSymbolType(expression.token)
            is FunctionCallExpression -> resolveFunctionCallExpressionType(expression)
            is BinaryExpression -> resolveBinaryExpressionType(expression)
            is UnaryExpression -> resolveUnaryExpressionType(expression)
            is PrefixAssignmentExpression -> resolveSymbolType(expression.symbolToken)
            is PostfixAssignmentExpression -> resolveSymbolType(expression.symbolToken)
            is EmptyExpression -> throw IllegalStateException("Empty expression should not be resolved")
        }

        expressionTypes[expression] = type

        return type
    }

    private fun resolveArrayIndexExpressionType(expression: ArrayIndexExpression): Type {
        val arrayType = resolveExpressionType(expression.arrayExpression)
        val arrayIndex = resolveExpressionType(expression.indexExpression)

        if (!arrayType.isSubtypeOf<ArrayType>()) {
            scope.addError(
                message = "Cannot index non-array type, got $arrayType",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        if (!arrayIndex.isSubtypeOf(IntType)) {
            scope.addError(
                message = "Array index must be of type int, got ${arrayIndex.name}",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        return arrayType.superTypes.filterIsInstance<ArrayType>().first().elementType
    }

    private fun resolveArrayLiteralExpressionType(expression: ArrayLiteralExpression): Type {
        if (expression.elements.isEmpty()) return EmptyArrayType

        return ArrayType(expression.elements.map(::resolveExpressionType).commonSupertype())
    }

    private fun resolveSymbolType(symbol: Token.Symbol): Type {
        val builtinType = symbol.toType().takeIf { it != UndefinedType }
        val variableType = scope.variables[symbol.value]

        val resolvedType = builtinType ?: variableType
        if (resolvedType == null || resolvedType is UndefinedType) {
            scope.addError(
                message = "Symbol ${symbol.value} is not defined",
                token = symbol,
            )

            return UndefinedType
        }

        return resolvedType
    }

    private fun resolveFunctionCallExpressionType(expression: FunctionCallExpression): Type {
        val argumentTypes = expression.arguments.map(::resolveExpressionType)
        val function = scope.functions.getMatchingFunctionDeclaration(
            name = expression.nameToken.value,
            arguments = argumentTypes,
        )

        if (function == null) {
            scope.addError(
                message = "Function ${expression.nameToken.value}(${
                    argumentTypes.joinToString { it.name }
                }) is not declared",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        return function(argumentTypes)
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
            operator = expression.operatorToken
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
