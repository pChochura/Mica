package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
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
        val type = typeResolver.resolveExpressionType(
            MemberAccessExpression(
                symbolExpression = SymbolExpression(symbolToken),
                accessorExpressions = this.accessorExpressions,
            ),
        )

        val expressionType = typeResolver.resolveExpressionType(rhs)
        if (!expressionType.isSubtypeOf(type)) {
            scope.addError(
                message = "Type of the expression (${
                    expressionType.name
                }) doesn't resolve to ${type.name}",
                token = rhs.startingToken,
            )
        }
    }
}
