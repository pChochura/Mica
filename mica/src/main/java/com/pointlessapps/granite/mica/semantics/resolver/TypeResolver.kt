package com.pointlessapps.granite.mica.semantics.resolver

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.mapper.toType
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.NumberType
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.model.Type

internal class TypeResolver(
    private val functions: Map<String, FunctionDeclarationStatement>,
    private val variables: Map<String, VariableDeclarationStatement>,
) {
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
        val variable = variables[expression.token.value]?.typeToken

        return builtinType ?: variable?.toType() ?: ErrorType(
            message = "Undefined symbol",
            token = expression.token,
        )
    }

    private fun resolveFunctionCallExpressionType(expression: FunctionCallExpression): Type {
        val existingFunction = functions[expression.getSignature()]

        return if (existingFunction == null) ErrorType(
            message = "Undefined function",
            token = expression.nameToken,
        ) else existingFunction.returnType ?: ErrorType(
            message = "Function ${expression.nameToken.value} has no return type",
            token = expression.nameToken,
        )
    }

    private fun resolveBinaryExpressionType(expression: BinaryExpression): Type {
        val lhsType = resolveExpressionType(expression.lhs)
        val rhsType = resolveExpressionType(expression.rhs)

        return TypeCoercionResolver.resolveBinaryOperator(
            lhs = lhsType,
            rhs = rhsType,
            operator = expression.operatorToken,
        ) ?: ErrorType("Type mismatch ($lhsType <> $rhsType)", expression.operatorToken)
    }

    private fun resolveUnaryExpressionType(expression: UnaryExpression): Type {
        val rhsType = resolveExpressionType(expression.rhs)

        return TypeCoercionResolver.resolvePrefixUnaryOperator(
            rhs = rhsType,
            operator = expression.operatorToken
        ) ?: ErrorType(
            message = "Operator ${
                expression.operatorToken.type.valueLiteral()
            } is not applicable for $rhsType",
            token = expression.operatorToken,
        )
    }
}
