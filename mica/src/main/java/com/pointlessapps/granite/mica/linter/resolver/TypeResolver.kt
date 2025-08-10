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
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveCommonBaseType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveElementTypeCoercedToArray
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

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
            is ArrayIndexExpression -> resolveArrayIndexExpressionType(expression)
            is ArrayLiteralExpression -> resolveArrayLiteralExpressionType(expression)
            is ArrayTypeExpression -> ArrayType(resolveExpressionType(expression.typeExpression))
            is SymbolTypeExpression -> expression.symbolToken.toType()
            is ParenthesisedExpression -> resolveExpressionType(expression.expression)
            is SymbolExpression -> resolveSymbolExpressionType(expression)
            is FunctionCallExpression -> resolveFunctionCallExpressionType(expression)
            is BinaryExpression -> resolveBinaryExpressionType(expression)
            is UnaryExpression -> resolveUnaryExpressionType(expression)
            is EmptyExpression -> throw IllegalStateException("Empty expression should not be resolved")
        }

        expressionTypes[expression] = type

        return type ?: UndefinedType
    }

    private fun resolveArrayIndexExpressionType(expression: ArrayIndexExpression): Type {
        // Return the array type if the index evaluates to an array
        // Otherwise return the element type of the array
        val arrayType = resolveExpressionType(expression.arrayExpression)
        val elementType = arrayType.resolveElementTypeCoercedToArray()
        if (resolveExpressionType(expression.indexExpression).canBeCoercedTo(ArrayType(NumberType))) {
            return ArrayType(elementType)
        }

        return elementType
    }

    private fun resolveArrayLiteralExpressionType(expression: ArrayLiteralExpression): ArrayType {
        if (expression.elements.isEmpty()) return EmptyArrayType

        return ArrayType(
            expression.elements.map(::resolveExpressionType)
                .resolveCommonBaseType(),
        )
    }

    private fun resolveSymbolExpressionType(expression: SymbolExpression): Type {
        val builtinType = expression.token.toType()
        val variable = scope.variables[expression.token.value]?.typeExpression

        val resolvedType = builtinType ?: variable?.let(::resolveExpressionType)
        if (resolvedType == null || resolvedType is UndefinedType) {
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
            if (it.value.parameters.size != expression.arguments.size) {
                return@firstNotNullOfOrNull null
            }

            val matchesSignature = it.value.parameters.zip(expression.arguments)
                .all { (parameter, argument) ->
                    val parameterType = resolveExpressionType(parameter.typeExpression)
                    val argumentType = resolveExpressionType(argument)
                    argumentType.canBeCoercedTo(parameterType)
                }

            if (matchesSignature) it.value else null
        }

        if (existingFunction == null) {
            scope.addError(
                message = "Function ${expression.getSignature()} is not declared",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        return existingFunction.returnTypeExpression?.let(::resolveExpressionType) ?: UndefinedType
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

        val resolvedType = TypeCoercionResolver.resolvePrefixUnaryOperator(
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
