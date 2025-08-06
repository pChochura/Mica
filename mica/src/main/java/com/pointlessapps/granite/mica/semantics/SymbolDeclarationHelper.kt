package com.pointlessapps.granite.mica.semantics

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.Scope

internal object SymbolDeclarationHelper {

    fun List<Statement>.declareScope(
        parentScope: Scope? = null,
        allowVariables: Boolean = true,
        allowFunctions: Boolean = true,
    ): Scope {
        val functions: MutableMap<String, FunctionDeclarationStatement> =
            parentScope?.functions?.toMutableMap() ?: mutableMapOf()
        val variables: MutableMap<String, VariableDeclarationStatement> =
            parentScope?.variables?.toMutableMap() ?: mutableMapOf()

        val localErrors: MutableList<ReportedError> = mutableListOf()

        forEach { statement ->
            when (statement) {
                is FunctionDeclarationStatement -> if (!allowFunctions) {
                    localErrors.add(
                        ReportedError(
                            message = "Function declaration is not allowed in this scope",
                            token = statement.startingToken,
                        ),
                    )
                } else statement.declareFunction(functions, localErrors)

                is VariableDeclarationStatement -> if (!allowVariables) {
                    localErrors.add(
                        ReportedError(
                            message = "Variable declaration is not allowed in this scope",
                            token = statement.startingToken,
                        ),
                    )
                } else statement.declareVariable(variables, localErrors)

                else -> {} // Ignore other statements
            }
        }

        return Scope(parentScope, functions, variables).apply {
            addErrors(localErrors)
        }
    }

    private fun FunctionDeclarationStatement.declareFunction(
        functions: MutableMap<String, FunctionDeclarationStatement>,
        errors: MutableList<ReportedError>,
    ) {
        val signature = signature
        if (functions[signature] != null) {
            errors.add(
                ReportedError(
                    message = "Redeclaration of the function: $signature",
                    token = startingToken,
                ),
            )

            return
        }

        functions[signature] = this
    }

    private fun VariableDeclarationStatement.declareVariable(
        variables: MutableMap<String, VariableDeclarationStatement>,
        errors: MutableList<ReportedError>,
    ) {
        val name = lhsToken.value
        val declaredVariable = variables[name]
        if (declaredVariable != null) {
            errors.add(
                ReportedError(
                    message = "Redeclaration of the variable: $name. Use the assignment operator instead",
                    token = startingToken,
                ),
            )

            return
        }

        variables[name] = this
    }
}
