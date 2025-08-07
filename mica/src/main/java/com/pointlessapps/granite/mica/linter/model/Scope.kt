package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.model.Token

/**
 * Maps function names to their overloads, where overloads are keyed by signature.
 */
internal typealias FunctionOverloads = MutableMap<String, Map<String, FunctionDeclarationStatement>>

/**
 * Maps the name of the variable to the variable declaration statement.
 */
internal typealias VariableDeclarations = MutableMap<String, VariableDeclarationStatement>

/**
 * A scope that holds all of the current variables and functions.
 * It can be created inside of a function or a block.
 */
internal data class Scope(
    val scopeType: ScopeType,
    val parent: Scope?,
) {
    val functions: FunctionOverloads = parent?.functions ?: mutableMapOf()
    val variables: VariableDeclarations = parent?.variables ?: mutableMapOf()

    private val _reports: MutableList<Report> = mutableListOf()
    val reports: List<Report>
        get() = _reports.sorted()

    // TODO rethink the design
    var controlFlowBreakValue: Any? = null

    fun addReports(reports: List<Report>) {
        this._reports.addAll(reports)
    }

    fun addError(message: String, token: Token) {
        _reports.add(
            Report(
                type = Report.ReportType.ERROR,
                message = message,
                token = token,
            ),
        )
    }

    fun addWarning(message: String, token: Token) {
        _reports.add(
            Report(
                type = Report.ReportType.WARNING,
                message = message,
                token = token,
            ),
        )
    }

    fun declareFunction(statement: FunctionDeclarationStatement) {
        if (!scopeType.allowFunctions) {
            addError(
                message = "Function declaration is not allowed in this scope",
                token = statement.startingToken,
            )

            return
        }

        val existingFunctionOverloads = functions[statement.nameToken.value]
        if (existingFunctionOverloads != null && existingFunctionOverloads.containsKey(statement.signature)) {
            addError(
                message = "Redeclaration of the function: ${statement.signature}",
                token = statement.startingToken,
            )

            return
        }

        if (existingFunctionOverloads == null) {
            functions[statement.nameToken.value] = mapOf(statement.signature to statement)
        } else {
            functions[statement.nameToken.value] =
                existingFunctionOverloads + mapOf(statement.signature to statement)
        }
    }

    fun declareVariable(statement: VariableDeclarationStatement) {
        if (!scopeType.allowVariables) {
            addError(
                message = "Variable declaration is not allowed in this scope",
                token = statement.startingToken,
            )

            return
        }

        val name = statement.lhsToken.value
        val declaredVariable = variables[name]
        if (declaredVariable != null) {
            addError(
                message = "Redeclaration of the variable: $name. Use the assignment operator instead",
                token = statement.startingToken,
            )

            return
        }

        variables[name] = statement
    }
}
