package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.builtins.builtinFunctionSignatures
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Token

/**
 * Maps function names to their overloads, where overloads are keyed by signature.
 */
internal typealias FunctionOverloads = MutableMap<String, FunctionDeclarationStatement>

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
    val functions: FunctionOverloads = parent?.functions?.toMutableMap() ?: mutableMapOf()
    val variables: VariableDeclarations = parent?.variables?.toMutableMap() ?: mutableMapOf()

    private val _reports: MutableList<Report> = mutableListOf()
    val reports: List<Report>
        get() = _reports.sorted()

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

    fun declareFunction(statement: FunctionDeclarationStatement, typeResolver: TypeResolver) {
        if (!scopeType.allowFunctions) {
            addError(
                message = "Function declaration is not allowed in this scope",
                token = statement.startingToken,
            )

            return
        }

        val signature = statement.getSignature(typeResolver)
        if (signature in builtinFunctionSignatures) {
            addError(
                message = "Redeclaration of the builtin function: $signature",
                token = statement.startingToken,
            )

            return
        }

        if (functions.containsKey(signature)) {
            addError(
                message = "Redeclaration of the function: $signature",
                token = statement.startingToken,
            )

            return
        }

        functions[signature] = statement
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
                message = "Redeclaration of the variable: $name",
                token = statement.startingToken,
            )

            return
        }

        variables[name] = statement
    }

    /**
     * Function signature in a format:
     * <function name>(<parameter type>, <parameter type>, ...)
     */
    private fun FunctionDeclarationStatement.getSignature(typeResolver: TypeResolver): String {
        return "${nameToken.value}(${
            parameters.joinToString { typeResolver.resolveExpressionType(it.typeExpression).name }
        })"
    }
}
