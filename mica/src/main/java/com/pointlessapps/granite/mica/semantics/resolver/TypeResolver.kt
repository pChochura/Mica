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
import com.pointlessapps.granite.mica.semantics.model.UndefinedType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo

/**
 * Resolves the type of an expression. If the expression type is not resolvable,
 * reports and error and returns [UndefinedType].
 */
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
            is ParenthesisedExpression -> resolveExpressionType(expression.expression)
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

            return UndefinedType
        }

        return resolvedType
    }

    private fun resolveFunctionCallExpressionType(expression: FunctionCallExpression): Type {
        val existingFunctionOverloads = scope.functions[expression.nameToken.value]

        val existingFunction = existingFunctionOverloads?.firstNotNullOfOrNull {
            if (it.value.parameterTypes.size != expression.arguments.size) {
                return@firstNotNullOfOrNull null
            }

            val matchesSignature = it.value.parameterTypes.values.zip(expression.arguments)
                .all { (type, argument) ->
                    val argumentType = resolveExpressionType(argument)
                    type != null && argumentType.canBeCoercedTo(type)
                }

            if (matchesSignature) it.value else null
        }

        if (existingFunction == null) {
            scope.addError(
                message = "Function ${expression.getSignature()} is not declared",
                token = expression.startingToken,
            )
        }

        return existingFunction?.returnType ?: UndefinedType
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
                message = "Type mismatch: ${lhsType.name} != ${rhsType.name}",
                token = expression.operatorToken,
            )

            return UndefinedType
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

            return UndefinedType
        }

        return resolvedType
    }
}
