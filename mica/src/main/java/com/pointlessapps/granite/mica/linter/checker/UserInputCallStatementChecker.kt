package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token

internal class UserInputCallStatementChecker(
    scope: Scope,
) : StatementChecker<UserInputCallStatement>(scope) {

    override fun check(statement: UserInputCallStatement) {
        // Check whether the variable is resolvable to String
        statement.checkVariableType()
    }

    private fun UserInputCallStatement.checkVariableType() {
        val variable = scope.variables[contentToken.value]
        val variableType = variable?.type
        if (variable == null || variableType == null) {
            scope.declareVariable(createVariableDeclarationStatement(contentToken))

            return
        }

        if (!StringType.canBeCoercedTo(variableType)) {
            scope.addError(
                message = "Type of the variable ${contentToken.value} doesn't resolve to a string",
                token = contentToken,
            )
        } else if (variableType !is StringType) {
            scope.addWarning(
                message = "Variable ${
                    contentToken.value
                } is not a string. The input will be coerced to a string",
                token = contentToken,
            )
        }
    }

    private fun createVariableDeclarationStatement(
        nameToken: Token.Symbol,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = nameToken,
        colonToken = Token.Colon(Location.EMPTY),
        typeToken = Token.Symbol(Location.EMPTY, StringType.name),
        equalSignToken = Token.Equals(Location.EMPTY),
        rhs = EmptyExpression,
    )
}
