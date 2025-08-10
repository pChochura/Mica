package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.model.State

internal object FunctionCallExpressionExecutor {

    fun execute(
        expression: FunctionCallExpression,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: (Statement, State, Scope, TypeResolver) -> Unit,
    ): Any {
        val function = findFunctionDeclaration(expression, scope, typeResolver)
        val localScope = Scope(
            scopeType = ScopeType.Function(function),
            parent = scope,
        )
        val newTypeResolver = TypeResolver(localScope)
        val localState = State.from(state)

        // Declare parameters as variables
        function.parameters.zip(expression.arguments).forEach { (declaration, expression) ->
            val statement = createVariableDeclarationStatement(declaration, expression)
            localScope.declareVariable(statement)

            localState.declareVariable(
                name = statement.lhsToken.value,
                value = onAnyExpressionCallback(
                    statement.rhs,
                    localState,
                    localScope,
                    newTypeResolver,
                ),
                originalType = newTypeResolver.resolveExpressionType(statement.rhs),
                variableType = newTypeResolver.resolveExpressionType(declaration.typeExpression),
            )
        }

        val localReturnValue = function.body.firstNotNullOfOrNull {
            onStatementExecutionCallback(it, localState, localScope, newTypeResolver)
            (localScope.controlFlowBreakValue as? ControlFlowBreak.Return)?.value
        }

        return localReturnValue ?: Any()
    }

    private fun findFunctionDeclaration(
        expression: FunctionCallExpression,
        scope: Scope,
        typeResolver: TypeResolver,
    ): FunctionDeclarationStatement = requireNotNull(scope.functions[expression.nameToken.value])
        .firstNotNullOf {
            if (it.value.parameters.size != expression.arguments.size) {
                return@firstNotNullOf null
            }

            val matchesSignature = it.value.parameters.zip(expression.arguments)
                .all { (parameter, argument) ->
                    val parameterType = typeResolver.resolveExpressionType(parameter.typeExpression)
                    val argumentType = typeResolver.resolveExpressionType(argument)
                    argumentType.canBeCoercedTo(parameterType)
                }

            if (matchesSignature) it.value else null
        }

    private fun createVariableDeclarationStatement(
        declaration: FunctionParameterDeclarationStatement,
        expression: Expression,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = declaration.nameToken,
        colonToken = declaration.colonToken,
        typeExpression = declaration.typeExpression,
        equalSignToken = Token.Equals(Location.EMPTY),
        rhs = expression,
    )
}
