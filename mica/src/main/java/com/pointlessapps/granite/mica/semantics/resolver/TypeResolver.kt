package com.pointlessapps.granite.mica.semantics.resolver

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.semantics.mapper.toType
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.CharType
import com.pointlessapps.granite.mica.semantics.model.NumberType
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.model.Type
import com.pointlessapps.granite.mica.semantics.model.VoidType

internal class TypeResolver(private val scope: Scope) {
    private val expressionTypes = mutableMapOf<Expression, Type?>()

    private fun FunctionCallExpression.getSignature() = "${nameToken.value}(${
        arguments.joinToString { resolveExpressionType(it).name }
    })"

    fun resolveExpressionType(expression: Expression): Type {
        if (expressionTypes.containsKey(expression)) {
            return expressionTypes[expression]!!
        }

        val type = when (expression) {
            is BooleanLiteralExpression -> BoolType
            is CharLiteralExpression -> CharType
            is StringLiteralExpression -> StringType
            is NumberLiteralExpression -> NumberType
            is ParenthesisedExpression -> resolveExpressionType(expression)
            is SymbolExpression -> resolveSymbolExpressionType(expression)
            is FunctionCallExpression -> resolveFunctionCallExpressionType(expression)
            is BinaryExpression -> resolveBinaryExpressionType(expression)
            is UnaryExpression -> resolveUnaryExpressionType(expression)
        }

        expressionTypes[expression] = type

        return type
    }

    private fun resolveSymbolExpressionType(expression: SymbolExpression): Type {
        val builtinType = expression.token.toType()
        val variable = scope.variables[expression.token.value]?.typeToken

        val resolvedType = builtinType ?: variable?.toType()
        if (resolvedType == null) {
            scope.addError(
                message = "Symbol ${expression.token.value} is not defined",
                token = expression.token,
            )

            return VoidType
        }

        return resolvedType
    }

    private fun resolveFunctionCallExpressionType(expression: FunctionCallExpression): Type {
        // TODO find a function that matches the name and then try to coerce the arguments
        val existingFunction = scope.functions[expression.getSignature()]

        if (existingFunction == null) {
            scope.addError(
                message = "Function ${expression.getSignature()} is not declared",
                token = expression.startingToken,
            )

            return VoidType
        } else if (existingFunction.returnType == null) {
            scope.addError(
                message = "Function ${expression.nameToken.value} has no return type",
                token = expression.startingToken,
            )

            return VoidType
        }

        return existingFunction.returnType
    }

    private fun resolveBinaryExpressionType(expression: BinaryExpression): Type {
        val lhsType = resolveExpressionType(expression.lhs)
        val rhsType = resolveExpressionType(expression.rhs)

        val resolvedType = TypeCoercionResolver.resolveBinaryOperator(
            lhs = lhsType,
            rhs = rhsType,
            operator = expression.operatorToken,
        )

        if (resolvedType == null) {
            scope.addError(
                message = "Type mismatch: $lhsType != $rhsType",
                token = expression.operatorToken,
            )

            return VoidType
        }

        return resolvedType
    }

    private fun resolveUnaryExpressionType(expression: UnaryExpression): Type {
        val rhsType = resolveExpressionType(expression.rhs)

        val resolvedType = TypeCoercionResolver.resolvePrefixUnaryOperator(
            rhs = rhsType,
            operator = expression.operatorToken
        )

        if (resolvedType == null) {
            scope.addError(
                message = "Operator ${
                    expression.operatorToken.type.valueLiteral()
                } is not applicable for $rhsType",
                token = expression.startingToken,
            )

            return VoidType
        }

        return resolvedType
    }
}
