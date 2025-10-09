package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.helper.getMatchingTypeDeclaration
import com.pointlessapps.granite.mica.helper.replaceTypeParameter
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.linter.mapper.toFunctionSignatures
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.GenericType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

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
    private val types: MutableMap<String, Type> = mutableMapOf()
    private val typeProperties: MutableMap<Type, Map<String, TypeProperty>> = mutableMapOf()
    private val variables: VariableDeclarations = mutableMapOf()
    private val functions: FunctionOverloads = mutableMapOf()
    private val functionSignatures: MutableSet<String> = mutableSetOf()

    internal fun addFunctions(functions: FunctionOverloads) {
        this.functions.putAll(functions)
        this.functionSignatures.addAll(functions.toFunctionSignatures())
    }

    internal fun addTypeProperties(typeProperties: Map<Type, Map<String, TypeProperty>>) {
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
        typeParameterConstraint: Type?,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        accessType: FunctionOverload.AccessType,
        overrideExisting: Boolean = false,
    ) {
        if (!scopeType.allowFunctions) {
            addError(
                message = "Function declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        if (scopeType is ScopeType.Type) {
            if (accessType == FunctionOverload.AccessType.GLOBAL_ONLY) {
                addError(
                    message = "Type function can only be declared as a member function",
                    token = startingToken,
                )

                return
            }

            return requireNotNull(
                value = parent,
                lazyMessage = { "Type cannot be a root level scope" },
            ).declareFunction(
                startingToken = startingToken,
                name = name,
                typeParameterConstraint = typeParameterConstraint,
                parameters = parameters,
                returnType = returnType,
                accessType = FunctionOverload.AccessType.MEMBER_ONLY,
                overrideExisting = overrideExisting,
            )
        }

        val signature = getSignature(name, parameters)
        if (!overrideExisting) {
            traverse {
                if (it.functionSignatures.contains(signature)) {
                    addError(
                        message = "Redeclaration of the function: $signature",
                        token = startingToken,
                    )

                    return
                }
            }
        }

        functionSignatures.add(signature)
        functions.getOrPut(
            key = name,
            defaultValue = ::mutableMapOf,
        )[parameters] = FunctionOverload(
            typeParameterConstraint = typeParameterConstraint,
            parameters = parameters,
            getReturnType = { typeArg, _ ->
                typeArg?.let(returnType::replaceTypeParameter) ?: returnType
            },
            accessType = accessType,
            isBuiltin = false,
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

    fun getFunctionOverloadsSignatures(name: String): List<String> {
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

        return allFunctions[name]?.values?.map {
            getSignature(
                name = name,
                parameters = it.parameters,
                accessType = it.accessType,
            )
        }.orEmpty()
    }

    fun declareVariable(
        startingToken: Token,
        name: String,
        type: Type,
        overrideExisting: Boolean = false,
    ) {
        if (!scopeType.allowVariables) {
            addError(
                message = "Variable declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        // Don't traverse, allow for overriding the parent scopes
        if (!overrideExisting && variables.containsKey(name)) {
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
        parentType: Type?,
        properties: Map<String, Type>,
        overrideExisting: Boolean = false,
    ) {
        if (!scopeType.allowTypes) {
            addError(
                message = "Type declaration is not allowed in this scope",
                token = startingToken,
            )

            return
        }

        if (!overrideExisting) {
            traverse {
                if (it.types.containsKey(name)) {
                    addError(
                        message = "Redeclaration of the type: $name",
                        token = startingToken,
                    )

                    return
                }
            }
        }

        val type = CustomType(name, parentType)
        types[name] = type
        typeProperties[type] = properties.mapValues {
            TypeProperty(
                name = it.key,
                receiverType = type,
                returnType = it.value,
                isBuiltin = false,
            )
        }
    }

    fun declareGenericType(parentType: Type = AnyType) {
        types[GenericType.NAME] = GenericType(parentType)
    }

    fun undeclareGenericType() {
        types.remove(GenericType.NAME)
    }

    fun getType(name: String): Type? {
        traverse { if (it.types.containsKey(name)) return it.types[name] }
        return null
    }

    fun getTypeProperties(type: Type): Map<String, TypeProperty>? {
        traverse { if (it.typeProperties.containsKey(type)) return it.typeProperties[type] }
        return null
    }

    fun getMatchingTypeProperty(type: Type, propertyName: String): TypeProperty? {
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
