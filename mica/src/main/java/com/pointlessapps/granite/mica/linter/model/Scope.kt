package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

/**
 * Maps a function name with its arity to a map of overloads and their return types.
 */
internal typealias FunctionOverloads = MutableMap<Pair<String, Int>, MutableMap<List<Type>, Type>>

/**
 * Maps the name of the variable to its type.
 */
internal typealias VariableDeclarations = MutableMap<String, Type>

/**
 * A scope that holds all of the current variables and functions.
 * It can be created inside of a function or a block.
 */
internal data class Scope(
    val scopeType: ScopeType,
    val parent: Scope?,
) {
    private val functionSignatures: MutableSet<String> = mutableSetOf()

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

    fun declareFunction(
        startingToken: Token,
        name: String,
        parameters: List<Type>,
        returnType: Type,
    ) {
        if (!scopeType.allowFunctions) {
            addError(
                message = "Function declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        val signature = "$name(${parameters.joinToString { it.name }})"
        if (functionSignatures.contains(signature)) {
            addError(
                message = "Redeclaration of the function: $signature",
                token = startingToken,
            )

            return
        }

        functionSignatures.add(signature)
        functions.getOrPut(
            key = name to parameters.size,
            defaultValue = ::mutableMapOf,
        )[parameters] = returnType
    }

    fun declareVariable(startingToken: Token, name: String, type: Type) {
        if (!scopeType.allowVariables) {
            addError(
                message = "Variable declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        val declaredVariable = variables[name]
        if (declaredVariable != null) {
            addError(
                message = "Redeclaration of the variable: $name",
                token = startingToken,
            )

            return
        }

        variables[name] = type
    }
}
