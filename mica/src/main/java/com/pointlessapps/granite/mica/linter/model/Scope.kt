package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.helper.getMatchingTypeDeclaration
import com.pointlessapps.granite.mica.helper.isTypeParameter
import com.pointlessapps.granite.mica.helper.replaceTypeParameter
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.linter.mapper.toFunctionSignatures
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_AND_MEMBER
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.MEMBER_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

/**
 * Maps a function name with its arity to a map of overloads and their return types.
 */
internal typealias FunctionOverloads = MutableMap<String, MutableMap<List<FunctionOverload.Parameter>, FunctionOverload>>

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
    private val types: MutableMap<String, CustomType> = mutableMapOf()
    private val typeProperties: MutableMap<Type, Map<String, Type>> = mutableMapOf()
    private val variables: VariableDeclarations = mutableMapOf()
    private val functions: FunctionOverloads = mutableMapOf()
    private val functionSignatures: MutableSet<String> = mutableSetOf()

    internal fun addFunctions(functions: FunctionOverloads) {
        this.functions.putAll(functions)
        this.functionSignatures.addAll(functions.toFunctionSignatures())
    }

    internal fun addTypeProperties(typeProperties: Map<Type, Map<String, Type>>) {
        this.typeProperties.putAll(typeProperties)
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
        isVararg: Boolean,
        typeParameterConstraint: Type?,
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
                isVararg = isVararg,
                typeParameterConstraint = typeParameterConstraint,
                parameters = parameters,
                returnType = returnType,
                accessType = MEMBER_ONLY,
            )
        }

        val signature = getSignature(name, parameters, false, isVararg)
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
        val functionOverloadParameters = parameters.mapIndexed { index, type ->
            Resolver.SUBTYPE_MATCH.of(
                type = type,
                vararg = isVararg && index == parameters.lastIndex,
            )
        }
        functions.getOrPut(
            key = name,
            defaultValue = ::mutableMapOf,
        )[functionOverloadParameters] = FunctionOverload(
            typeParameterConstraint = typeParameterConstraint,
            parameters = functionOverloadParameters,
            getReturnType = { typeArg, _ ->
                if (returnType.isTypeParameter()) {
                    typeArg?.let(returnType::replaceTypeParameter) ?: UndefinedType
                } else {
                    returnType
                }
            },
            accessType = accessType,
        )
    }

    fun getMatchingFunctionDeclaration(
        name: String,
        arguments: List<Type>,
    ): FunctionOverload? {
        val allFunctions = buildMap {
            traverse {
                it.functions.forEach { (name, functions) ->
                    getOrPut(
                        key = name,
                        defaultValue = ::mutableMapOf,
                    ).putAll(functions)
                }
            }
        }

        return allFunctions.getMatchingFunctionDeclaration(name, arguments)
    }

    fun getFunctionOverloadsSignatures(
        name: String,
    ): List<String> {
        val allFunctions = buildMap {
            traverse {
                it.functions.forEach { (name, functions) ->
                    getOrPut(
                        key = name,
                        defaultValue = ::mutableMapOf,
                    ).putAll(functions)
                }
            }
        }

        return allFunctions[name]?.values?.flatMap {
            val isVararg = it.parameters.lastOrNull()?.vararg == true
            val parameters = it.parameters.map(FunctionOverload.Parameter::type)
            when (it.accessType) {
                MEMBER_ONLY -> listOf(getSignature(name, parameters, true, isVararg))
                GLOBAL_ONLY -> listOf(getSignature(name, parameters, false, isVararg))
                GLOBAL_AND_MEMBER -> listOf(
                    getSignature(name, parameters, false, isVararg),
                    getSignature(name, parameters, true, isVararg),
                )
            }
        }.orEmpty()
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

        val type = CustomType(name)
        types[name] = type
        typeProperties[type] = properties
    }

    fun declareGenericType() {
        types[EmptyCustomType.name] = EmptyCustomType
    }

    fun undeclareGenericType() {
        types.remove(EmptyCustomType.name)
    }

    fun getType(name: String): CustomType? {
        traverse { if (it.types.containsKey(name)) return it.types[name] }
        return null
    }

    fun getMatchingTypeProperty(type: Type, propertyName: String): Type? {
        val allTypeProperties = buildMap {
            traverse {
                it.typeProperties.forEach { (receiverType, properties) ->
                    getOrPut(
                        key = receiverType,
                        defaultValue = ::mutableMapOf,
                    ).putAll(properties)
                }
            }
        }

        return allTypeProperties.getMatchingTypeDeclaration(type, propertyName)
    }
}
