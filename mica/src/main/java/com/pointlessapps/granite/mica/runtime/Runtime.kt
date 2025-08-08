package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.FunctionCallExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.IfConditionStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ReturnStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.VariableDeclarationStatementExecutor
import com.pointlessapps.granite.mica.runtime.helper.toNumber
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal class Runtime(private val rootAST: Root) {

    // TODO allow to provide the input
    private val inputSequence = sequenceOf(
        "Some",
        "Input",
        "Provided",
        "By",
        "The",
        "User",
    ).iterator()

    private val scope: Scope = Scope(scopeType = ScopeType.Root, parent = null)
    private val rootState = State(mutableMapOf())

    fun execute() {
        val typeResolver = TypeResolver(scope)
        rootAST.statements.forEach { executeStatement(it, scope, typeResolver) }
    }

    private fun executeStatement(
        statement: Statement,
        scope: Scope,
        typeResolver: TypeResolver,
    ) {
        when (statement) {
            is FunctionDeclarationStatement -> scope.declareFunction(statement)
            is VariableDeclarationStatement -> VariableDeclarationStatementExecutor.execute(
                statement = statement,
                rootState = rootState,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, scope, typeResolver) },
            )

            is AssignmentStatement -> rootState.assignValue(
                name = statement.lhsToken.value,
                value = executeExpression(statement.rhs, scope, typeResolver),
                originalType = typeResolver.resolveExpressionType(statement.rhs),
            )

            is ReturnStatement -> ReturnStatementExecutor.execute(
                statement = statement,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, scope, typeResolver) },
            )

            is IfConditionStatement -> IfConditionStatementExecutor.execute(
                statement = statement,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = ::executeExpression,
                onStatementExecutionCallback = ::executeStatement,
            )

            is UserInputCallStatement -> rootState.assignValue(
                name = statement.contentToken.value,
                value = inputSequence.next(),
                originalType = StringType,
            )

            is UserOutputCallStatement -> {
                // TODO provide a better way to print results
                val type = typeResolver.resolveExpressionType(statement.contentExpression)
                println(
                    executeExpression(statement.contentExpression, scope, typeResolver)
                        .coerceToType(type, StringType),
                )
            }

            is ExpressionStatement ->
                executeExpression(statement.expression, scope, typeResolver)

            is FunctionCallStatement ->
                executeExpression(statement.functionCallExpression, scope, typeResolver)
        }
    }

    private fun executeExpression(
        expression: Expression,
        scope: Scope,
        typeResolver: TypeResolver,
    ): Any = when (expression) {
        is SymbolExpression -> requireNotNull(rootState.variables[expression.token.value]?.value)
        is CharLiteralExpression -> expression.token.value
        is StringLiteralExpression -> expression.token.value
        is BooleanLiteralExpression -> expression.token.value.toBooleanStrict()
        is NumberLiteralExpression -> expression.token.value.toNumber()
        is ParenthesisedExpression -> executeExpression(
            expression.expression,
            scope,
            typeResolver
        )

        is UnaryExpression -> PrefixUnaryOperatorExpressionExecutor.execute(
            expression = expression,
            typeResolver = typeResolver,
            onAnyExpressionCallback = { executeExpression(it, scope, typeResolver) },
        )

        is BinaryExpression -> BinaryOperatorExpressionExecutor.execute(
            expression = expression,
            typeResolver = typeResolver,
            onAnyExpressionCallback = { executeExpression(it, scope, typeResolver) },
        )

        is FunctionCallExpression -> FunctionCallExpressionExecutor.execute(
            expression = expression,
            rootState = rootState,
            scope = scope,
            typeResolver = typeResolver,
            onAnyExpressionCallback = ::executeExpression,
            onStatementExecutionCallback = ::executeStatement,
        )

        is EmptyExpression -> throw IllegalStateException("Empty expression should not be resolved")
    }
}