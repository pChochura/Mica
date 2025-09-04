package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toFunctionSignatures
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import kotlin.collections.set

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
    private val types: TypeDeclarations = mutableMapOf()
    private val variables: VariableDeclarations = mutableMapOf()
    private val functions: FunctionOverloads = mutableMapOf()
    private val functionSignatures: MutableSet<String> = mutableSetOf()

    internal fun addFunctions(functions: FunctionOverloads) {
        this@Scope.functions.putAll(functions)
        this@Scope.functionSignatures.addAll(functions.toFunctionSignatures())
    }

    private inline fun traverse(callback: (Scope) -> Unit) {
        var currentScope: Scope? = this
        while (currentScope != null) {
            callback(currentScope)
            currentScope = currentScope.parent
        }
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
            return requireNotNull(
                value = parent,
                lazyMessage = { "Type cannot be a root level scope" },
            ).declareFunction(
                startingToken = startingToken,
                name = name,
                parameters = parameters,
                returnType = returnType,
                accessType = FunctionOverload.AccessType.MEMBER_ONLY,
            )
        }

        val signature = "$name(${parameters.joinToString { it.name }})"
        traverse {
            if (it.functionSignatures.contains(signature)) {
                addError(
                    message = "Redeclaration of the function: $signature",
                    token = startingToken,
                )

                return
            }
        }

        functionSignatures.add(signature)
        val functionOverloadParameters = parameters.map {
            FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH.of(it)
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
    ): FunctionOverload? {
        val allFunctions = buildMap {
            traverse {
                it.functions.forEach { (arity, functions) ->
                    getOrPut(
                        key = arity,
                        defaultValue = ::mutableMapOf,
                    ).putAll(functions)
                }
            }
        }

        return allFunctions.getMatchingFunctionDeclaration(name, arguments)
    }

    fun declareVariable(startingToken: Token, name: String, type: Type) {
        if (!scopeType.allowVariables) {
            addError(
                message = "Variable declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        // Don't traverse, allow for overriding the parent scopes
        if (variables.containsKey(name)) {
            addError(
                message = "Redeclaration of the variable: $name",
                token = startingToken,
            )

            return
        }

        variables[name] = type
    }

    fun getVariable(name: String): Type? {
        traverse { if (it.variables.containsKey(name)) return it.variables[name] }
        return null
    }

    fun containsVariable(name: String): Boolean {
        traverse { if (it.variables.containsKey(name)) return true }
        return false
    }

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

        traverse {
            if (it.types.containsKey(name)) {
                addError(
                    message = "Redeclaration of the type: $name",
                    token = startingToken,
                )

                return
            }
        }

        val typeSignature = properties.entries.joinToString(",") { "${it.key}:${it.value.name}" }
        types[name] = CustomType(typeSignature) to properties
    }

    fun getType(name: String): Pair<CustomType, Map<String, Type>>? {
        traverse { if (it.types.containsKey(name)) return it.types[name] }
        return null
    }
}
