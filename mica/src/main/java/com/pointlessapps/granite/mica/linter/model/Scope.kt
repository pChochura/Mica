package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toFunctionSignatures
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

/**
 * Maps a function name with its arity to a map of overloads and their return types.
 */
internal typealias FunctionOverloads = MutableMap<Pair<String, Int>, MutableMap<List<FunctionOverload.Parameter>, FunctionOverload>>

/**
 * Maps the name of the variable to its type.
 */
internal typealias VariableDeclarations = MutableMap<String, Type>

/**
 * Maps the name of the type to its type and a map of its properties.
 */
internal typealias TypeDeclarations = MutableMap<String, Pair<CustomType, Map<String, Type>>>

/**
 * A scope that holds all of the current variables and functions.
 * It can be created inside of a function or a block.
 */
internal data class Scope(
    val scopeType: ScopeType,
    val parent: Scope?,
) {
    private val types: TypeDeclarations = parent?.types?.toMutableMap() ?: mutableMapOf()
    private val functions: FunctionOverloads = parent?.functions?.toMutableMap() ?: mutableMapOf()
    private val variables: VariableDeclarations =
        parent?.variables?.toMutableMap() ?: mutableMapOf()

    private val functionSignatures: MutableSet<String> =
        functions.toFunctionSignatures().toMutableSet()

    internal fun addFunctions(functions: FunctionOverloads) {
        this@Scope.functions.putAll(functions)
        this@Scope.functionSignatures.addAll(functions.toFunctionSignatures())
    }

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
        accessType: FunctionOverload.AccessType,
    ) {
        if (!scopeType.allowFunctions) {
            addError(
                message = "Function declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        if (scopeType is ScopeType.Type) {
            return requireNotNull(parent).declareFunction(
                startingToken = startingToken,
                name = name,
                parameters = parameters,
                returnType = returnType,
                accessType = FunctionOverload.AccessType.MEMBER_ONLY,
            )
        }

        val signature = "$name(${parameters.joinToString { it.name }})"
        if (functionSignatures.contains(signature)) {
            addError(
                message = "Redeclaration of the function: $signature",
                token = startingToken,
            )

            return
        }

        // TODO consider adding a keyword `exact` to indicate exact matching
        functionSignatures.add(signature)
        val functionOverloadParameters = parameters.map {
            FunctionOverload.Parameter(
                type = it,
                resolver = FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH,
            )
        }
        functions.getOrPut(
            key = name to parameters.size,
            defaultValue = ::mutableMapOf,
        )[functionOverloadParameters] = FunctionOverload(
            parameters = functionOverloadParameters,
            getReturnType = { returnType },
            accessType = accessType,
        )
    }

    fun getMatchingFunctionDeclaration(
        name: String,
        arguments: List<Type>,
    ) = functions.getMatchingFunctionDeclaration(name, arguments)

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

    fun getVariable(name: String) = variables[name]
    fun containsVariable(name: String) = variables.containsKey(name)

    fun declareType(
        startingToken: Token,
        name: String,
        properties: Map<String, Type>,
    ) {
        if (!scopeType.allowTypes) {
            addError(
                message = "Type declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        val declaredType = types[name]
        if (declaredType != null) {
            addError(
                message = "Redeclaration of the type: $name",
                token = startingToken,
            )

            return
        }

        types[name] = CustomType(name) to properties
    }

    fun getType(name: String) = types[name]
}
