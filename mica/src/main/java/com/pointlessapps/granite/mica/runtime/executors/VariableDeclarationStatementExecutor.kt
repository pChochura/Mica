package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.runtime.State

internal object VariableDeclarationStatementExecutor {

    fun execute(
        statement: VariableDeclarationStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ) {
        scope.declareVariable(statement)

        state.declareVariable(
            name = statement.lhsToken.value,
            value = onAnyExpressionCallback(statement.rhs),
            originalType = typeResolver.resolveExpressionType(statement.rhs),
            variableType = requireNotNull(statement.type),
        )
    }
}
