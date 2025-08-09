package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
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
    ): VariableDeclarationStatement {
        val typeExpression = if (type is ArrayType) {
            ArrayTypeExpression(
                openBracketToken = Token.SquareBracketOpen(Location.EMPTY),
                closeBracketToken = Token.SquareBracketClose(Location.EMPTY),
                typeExpression = SymbolTypeExpression(
                    symbolToken = Token.Symbol(Location.EMPTY, type.elementType.name),
                ),
            )
        } else {
            SymbolTypeExpression(Token.Symbol(Location.EMPTY, type.name))
        }

        return VariableDeclarationStatement(
            lhsToken = statement.lhsToken,
            colonToken = Token.Colon(Location.EMPTY),
            typeExpression = typeExpression,
            equalSignToken = statement.equalSignToken,
            rhs = statement.rhs,
        )
    }
}
