package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.State

internal object UserInputCallStatementExecutor {

    // TODO allow to provide the input
    private val inputSequence = sequenceOf(
        "Some",
        "Input",
        "Provided",
        "By",
        "The",
        "User",
    ).iterator()

    fun execute(
        statement: UserInputCallStatement,
        state: State,
        scope: Scope,
    ) {
        if (!scope.variables.containsKey(statement.contentToken.value)) {
            scope.declareVariable(createVariableDeclarationStatement(statement.contentToken))
            state.declareVariable(
                name = statement.contentToken.value,
                value = inputSequence.next(),
                originalType = StringType,
                variableType = StringType,
            )
        } else {
            state.assignValue(
                name = statement.contentToken.value,
                value = inputSequence.next(),
                originalType = StringType,
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
