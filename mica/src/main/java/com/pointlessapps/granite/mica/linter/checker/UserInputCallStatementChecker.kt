package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token

internal class UserInputCallStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<UserInputCallStatement>(scope) {

    override fun check(statement: UserInputCallStatement) {
        // Check whether the variable is resolvable to String
        statement.checkVariableType()
    }

    private fun UserInputCallStatement.checkVariableType() {
        val variable = scope.variables[contentToken.value]
        if (variable == null) {
            scope.declareVariable(createVariableDeclarationStatement(contentToken))

            return
        }

        val variableType = typeResolver.resolveExpressionType(variable.typeExpression)

        if (variableType != StringType) {
            scope.addError(
                message = "Type of the variable ${contentToken.value} (${
                    variableType.name
                }) doesn't resolve to a string",
                token = contentToken,
            )
        }
    }

    private fun createVariableDeclarationStatement(
        nameToken: Token.Symbol,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = nameToken,
        colonToken = Token.Colon(Location.EMPTY),
        typeExpression = SymbolTypeExpression(
            symbolToken = Token.Symbol(Location.EMPTY, StringType.name),
        ),
        equalSignToken = Token.Equals(Location.EMPTY),
        rhs = EmptyExpression,
    )
}
