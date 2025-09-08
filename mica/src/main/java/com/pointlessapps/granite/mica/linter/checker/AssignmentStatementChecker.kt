package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.MemberAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.UndefinedType

internal class AssignmentStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<AssignmentStatement>(scope) {

    // TODO add support for multiple declarations at once
    // TODO follow the flow of the program instead of iterating through the statements

    override fun check(statement: AssignmentStatement) {
        // Check whether the variable is declared
        statement.declareIfNecessary()

        // Check whether the expression type is resolvable and matches the variable declaration
        statement.checkExpressionType()
    }

    private fun AssignmentStatement.declareIfNecessary() {
        if (!scope.containsVariable(symbolToken.value)) {
            if (equalSignToken !is Token.Equals) {
                scope.addError(
                    message = "Variable ${symbolToken.value} must be declared before being assigned to",
                    token = startingToken,
                )

                return
            }

            val type = typeResolver.resolveExpressionType(rhs)
            if (type is UndefinedType) {
                scope.addError(
                    message = "Type of the variable ${symbolToken.value} could not be determined",
                    token = rhs.startingToken,
                )

                return
            }

            scope.declareVariable(
                startingToken = startingToken,
                name = symbolToken.value,
                type = type,
            )
        }
    }

    private fun AssignmentStatement.checkExpressionType() {
        var type = typeResolver.resolveExpressionType(SymbolExpression(symbolToken))
        if (type is UndefinedType) {
            scope.addError(
                message = "Type of the variable ${symbolToken.value} could not be determined",
                token = rhs.startingToken,
            )

            return
        }

        accessorExpressions.forEach {
            when (it) {
                is ArrayIndexAccessorExpression -> {
                    if (!type.isSubtypeOf(EmptyArrayType)) {
                        scope.addError(
                            message = "Cannot index non-array type, got ${type.name}",
                            token = it.openBracketToken,
                        )

                        return
                    }

                    val indexExpressionType = typeResolver.resolveExpressionType(it.indexExpression)
                    if (!indexExpressionType.isSubtypeOf(IntType)) {
                        scope.addError(
                            message = "Array index must be of type int, got ${type.name}",
                            token = it.indexExpression.startingToken,
                        )

                        return
                    }

                    type = type.superTypes.filterIsInstance<ArrayType>().first().elementType
                }

                is MemberAccessAccessorExpression -> {
                    if (!type.isSubtypeOf(EmptyCustomType)) {
                        scope.addError(
                            message = "${type.name} does not have any properties",
                            token = it.dotToken,
                        )

                        return
                    }

                    val typeName = type.superTypes.filterIsInstance<CustomType>().first().name
                    val properties = requireNotNull(
                        value = scope.getType(typeName),
                        lazyMessage = { "Type $typeName is not declared" },
                    ).second
                    val property = properties[it.propertySymbolToken.value]
                    if (property == null) {
                        scope.addError(
                            message = "Property ${
                                it.propertySymbolToken.value
                            } does not exist on type ${type.name}",
                            token = it.propertySymbolToken,
                        )

                        return
                    }

                    type = property
                }
            }
        }

        val expressionType = typeResolver.resolveExpressionType(rhs)
        if (!type.isSubtypeOf(expressionType)) {
            scope.addError(
                message = "Type of the expression (${
                    expressionType.name
                }) doesn't resolve to ${type.name}",
                token = rhs.startingToken,
            )
        }
    }
}
