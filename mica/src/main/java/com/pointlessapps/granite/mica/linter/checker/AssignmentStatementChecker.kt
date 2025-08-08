package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.UndefinedType

internal class AssignmentStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<AssignmentStatement>(scope) {

    // TODO add support for multiple declarations at once

    override fun check(statement: AssignmentStatement) {
        // Check whether the variable is declared
        statement.declareIfNecessary()

        // Check whether the expression type is resolvable and matches the variable declaration
        statement.checkExpressionType()
    }

    private fun AssignmentStatement.declareIfNecessary() {
        if (!scope.variables.containsKey(lhsToken.value)) {
            val type = typeResolver.resolveExpressionType(rhs)
            if (type is UndefinedType) return

            scope.addWarning(
                message = "The type of the variable will be inferred from the expression. " +
                        "Use `${lhsToken.value}: ${
                            type.name
                        } = value` to be explicit.",
                token = startingToken,
            )

            scope.declareVariable(createVariableDeclarationStatement(this))
        }
    }

    private fun createVariableDeclarationStatement(
        statement: AssignmentStatement,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = statement.lhsToken,
        colonToken = Token.Colon(Location.EMPTY),
        typeToken = Token.Symbol(
            location = Location.EMPTY,
            value = typeResolver.resolveExpressionType(statement.rhs).name,
        ),
        equalSignToken = statement.equalSignToken,
        rhs = statement.rhs,
    )

    private fun AssignmentStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        val variable = scope.variables[lhsToken.value]
        if (variable != null && variable.type != null && !expressionType.canBeCoercedTo(variable.type)) {
            scope.addError(
                message = "Type mismatch: expected ${variable.type.name}, got ${expressionType.name}",
                token = rhs.startingToken,
            )
        }
    }
}
