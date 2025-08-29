package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
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

            is MemberAccessExpression -> resolveMemberAccessType(expression)
            is ArrayIndexExpression -> resolveArrayIndexExpressionType(expression)
            is ArrayLiteralExpression -> resolveArrayLiteralExpressionType(expression)
            is TypeExpression -> resolveTypeExpression(expression)
            is ParenthesisedExpression -> resolveExpressionType(expression.expression)
            is SymbolExpression -> resolveSymbolType(expression.token)
            is FunctionCallExpression -> resolveFunctionCallExpressionType(expression)
            is BinaryExpression -> resolveBinaryExpressionType(expression)
            is UnaryExpression -> resolveUnaryExpressionType(expression)
            is AffixAssignmentExpression -> resolveAffixAssignmentExpressionType(expression)
            is EmptyExpression -> throw IllegalStateException("Empty expression should not be resolved")
        }

        expressionTypes[expression] = type

        return type
    }

    private fun resolveMemberAccessType(expression: MemberAccessExpression): Type {
        val lhsType = resolveExpressionType(expression.lhs)
        if (!lhsType.isSubtypeOf(EmptyCustomType)) {
            scope.addError(
                message = "${lhsType.name} does not have any properties",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        val typeName = lhsType.superTypes.filterIsInstance<CustomType>().first().name
        val properties = requireNotNull(scope.getType(typeName)).second
        val property = properties[expression.propertySymbolToken.value]
        if (property == null) {
            scope.addError(
                message = "Property ${
                    expression.propertySymbolToken.value
                } does not exist on type ${lhsType.name}",
                token = expression.propertySymbolToken,
            )

            return UndefinedType
        }

        return property
    }

    private fun resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
        is ArrayTypeExpression -> ArrayType(resolveExpressionType(expression.typeExpression))
        is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
            ?: scope.getType(expression.symbolToken.value)?.first ?: let {
                scope.addError(
                    message = "Type ${expression.symbolToken.value} is not declared",
                    token = expression.startingToken,
                )

                UndefinedType
            }
    }

    private fun resolveAffixAssignmentExpressionType(expression: AffixAssignmentExpression): Type {
        val symbolType = resolveSymbolType(expression.symbolToken)
        val isArrayLike = symbolType.isSubtypeOf(EmptyArrayType)
        if (!isArrayLike && expression.indexExpressions.isNotEmpty()) {
            scope.addError(
                message = "Cannot index non-array type, got ${symbolType.name}",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        if (!isArrayLike) {
            if (!symbolType.isSubtypeOf(IntType)) {
                scope.addError(
                    message = "Variable ${expression.symbolToken.value} must be of type int",
                    token = expression.symbolToken,
                )

                return UndefinedType
            }

            return symbolType
        }

        var currentType: Type = symbolType
        expression.indexExpressions.forEach {
            if (!currentType.isSubtypeOf(EmptyArrayType)) {
                scope.addError(
                    message = "Cannot index non-array type, got ${currentType.name}",
                    token = it.openBracketToken,
                )

                return UndefinedType
            }

            currentType = currentType.superTypes.filterIsInstance<ArrayType>().first().elementType
        }

        if (!currentType.isSubtypeOf(IntType)) {
            scope.addError(
                message = "Expression ${expression.symbolToken.value} must be of type int",
                token = expression.symbolToken,
            )

            return UndefinedType
        }

        return currentType
    }

    private fun resolveArrayIndexExpressionType(expression: ArrayIndexExpression): Type {
        val arrayType = resolveExpressionType(expression.arrayExpression)
        val arrayIndex = resolveExpressionType(expression.indexExpression)

        if (!arrayType.isSubtypeOf(EmptyArrayType)) {
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
        if (symbol is Token.Keyword) {
            scope.addError(
                message = "Cannot use keyword ${symbol.value} in this context",
                token = symbol,
            )

            return UndefinedType
        }

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
        val argumentTypes = expression.arguments.map(::resolveExpressionType)
        val function = scope.getMatchingFunctionDeclaration(
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

        if (expression.isMemberFunctionCall && !function.accessType.allowMemberFunctionCalls()) {
            scope.addError(
                message = "${expression.nameToken.value} cannot be called as a member function",
                token = expression.startingToken,
            )

            return UndefinedType
        } else if (!expression.isMemberFunctionCall && function.accessType == FunctionOverload.AccessType.MEMBER_ONLY) {
            scope.addError(
                message = "${expression.nameToken.value} has be called as a member function",
                token = expression.startingToken,
            )

            return UndefinedType
        }

        return function.getReturnType(argumentTypes)
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
