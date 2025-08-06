package com.pointlessapps.granite.mica.semantics

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.Report
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType

internal object SymbolDeclarationHelper {

    fun List<Statement>.declareScope(
        scopeType: ScopeType,
        parentScope: Scope? = null,
        allowVariables: Boolean = true,
        allowFunctions: Boolean = true,
    ): Scope {
        val functions: MutableMap<String, Map<String, FunctionDeclarationStatement>> =
            parentScope?.functions?.toMutableMap() ?: mutableMapOf()
        val variables: MutableMap<String, VariableDeclarationStatement> =
            parentScope?.variables?.toMutableMap() ?: mutableMapOf()

        val localErrors: MutableList<Report> = mutableListOf()

        forEach { statement ->
            when (statement) {
                is FunctionDeclarationStatement -> if (!allowFunctions) {
                    localErrors.add(
                        Report(
                            type = Report.ReportType.ERROR,
                            message = "Function declaration is not allowed in this scope",
                            token = statement.startingToken,
                        ),
                    )
                } else statement.declareFunction(functions, localErrors)

                is VariableDeclarationStatement -> if (!allowVariables) {
                    localErrors.add(
                        Report(
                            type = Report.ReportType.ERROR,
                            message = "Variable declaration is not allowed in this scope",
                            token = statement.startingToken,
                        ),
                    )
                } else statement.declareVariable(variables, localErrors)

                else -> {} // Ignore other statements
            }
        }

        return Scope(scopeType, parentScope, functions, variables)
            .apply { addReports(localErrors) }
    }

    private fun FunctionDeclarationStatement.declareFunction(
        functions: MutableMap<String, Map<String, FunctionDeclarationStatement>>,
        errors: MutableList<Report>,
    ) {
        val existingFunctionOverloads = functions[nameToken.value]
        if (existingFunctionOverloads != null && existingFunctionOverloads.containsKey(signature)) {
            errors.add(
                Report(
                    type = Report.ReportType.ERROR,
                    message = "Redeclaration of the function: $signature",
                    token = startingToken,
                ),
            )

            return
        }

        if (existingFunctionOverloads == null) {
            functions[nameToken.value] = mapOf(signature to this)
        } else {
            functions[nameToken.value] = existingFunctionOverloads + mapOf(signature to this)
        }
    }

    private fun VariableDeclarationStatement.declareVariable(
        variables: MutableMap<String, VariableDeclarationStatement>,
        errors: MutableList<Report>,
    ) {
        val name = lhsToken.value
        val declaredVariable = variables[name]
        if (declaredVariable != null) {
            errors.add(
                Report(
                    type = Report.ReportType.ERROR,
                    message = "Redeclaration of the variable: $name. Use the assignment operator instead",
                    token = startingToken,
                ),
            )

            return
        }

        variables[name] = this
    }
}
