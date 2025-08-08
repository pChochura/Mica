package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.State

internal object AssignmentStatementExecutor {

    fun execute(
        statement: AssignmentStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ) {
        if (!scope.variables.containsKey(statement.lhsToken.value)) {
            val type = typeResolver.resolveExpressionType(statement.rhs)
            scope.declareVariable(createVariableDeclarationStatement(statement, type))
            state.declareVariable(
                name = statement.lhsToken.value,
                value = onAnyExpressionCallback(statement.rhs),
                originalType = type,
                variableType = type,
            )
        } else {
            state.assignValue(
                name = statement.lhsToken.value,
                value = onAnyExpressionCallback(statement.rhs),
                originalType = StringType,
            )
        }
    }

    private fun createVariableDeclarationStatement(
        statement: AssignmentStatement,
        type: Type,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = statement.lhsToken,
        colonToken = Token.Colon(Location.EMPTY),
        typeToken = Token.Symbol(
            location = Location.EMPTY,
            value = type.name,
        ),
        equalSignToken = statement.equalSignToken,
        rhs = statement.rhs,
    )
}
