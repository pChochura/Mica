package com.pointlessapps.granite.mica.compiler.model

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.builtins.builtinTypeProperties
import com.pointlessapps.granite.mica.builtins.functions.BuiltinFunctionDeclaration
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.model.TypeProperty
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

internal class CompilerContext {

    private val emptyToken = Token.Invalid(Location.EMPTY, "")
    private val loopLabelStack: MutableList<String> = mutableListOf()

    private val globalScope: Scope = Scope(ScopeType.Root, null).apply {
        addFunctions(
            builtinFunctions.groupingBy(BuiltinFunctionDeclaration::name)
                .aggregate { _, acc: MutableMap<List<FunctionOverload.Parameter>, FunctionOverload>?, element, first ->
                    val overload = FunctionOverload(
                        typeParameterConstraint = element.typeParameterConstraint,
                        parameters = element.parameters,
                        getReturnType = element.getReturnType,
                        accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
                        isBuiltin = true,
                    )
                    if (first) {
                        mutableMapOf(element.parameters to overload)
                    } else {
                        requireNotNull(acc).apply { put(element.parameters, overload) }
                    }
                }.toMutableMap(),
        )
        addTypeProperties(
            builtinTypeProperties.groupingBy { it.receiverType }
                .aggregate { _, acc: MutableMap<String, TypeProperty>?, element, first ->
                    val typeProperty = TypeProperty(
                        name = element.name,
                        receiverType = element.receiverType,
                        returnType = element.returnType,
                        hasDefaultValue = false,
                        isBuiltin = true,
                    )
                    if (first) {
                        mutableMapOf(element.name to typeProperty)
                    } else {
                        requireNotNull(acc).apply { put(element.name, typeProperty) }
                    }
                },
        )
    }
    private val typeResolver = TypeResolver(globalScope)

    fun removeLastLoopLabel(): String? = loopLabelStack.removeLastOrNull()
    fun addLoopLabel(label: String) {
        loopLabelStack.add(label)
    }

    fun containsVariable(name: String) = globalScope.containsVariable(name)
    fun declareVariable(
        name: String,
        type: Type,
    ) = globalScope.declareVariable(
        startingToken = emptyToken,
        name = name,
        type = type,
        overrideExisting = true,
    )

    fun declareFunction(
        name: String,
        typeParameterConstraint: Type?,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
    ) = globalScope.declareFunction(
        startingToken = emptyToken,
        name = name,
        typeParameterConstraint = typeParameterConstraint,
        parameters = parameters,
        returnType = returnType,
        accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
        overrideExisting = true,
    )

    fun declareType(
        name: String,
        parentType: Type?,
        properties: Map<String, Pair<Type, Boolean>>,
    ): Type {
        globalScope.declareType(
            startingToken = emptyToken,
            name = name,
            parentType = parentType,
            properties = properties,
            overrideExisting = true,
        )

        return requireNotNull(
            value = globalScope.getType(name),
            lazyMessage = { "No type found for $name" },
        )
    }

    fun getMatchingFunctionDeclaration(name: String, argumentTypes: List<Type>) = requireNotNull(
        value = globalScope.getMatchingFunctionDeclaration(name, argumentTypes),
        lazyMessage = { "No matching function found for $name" },
    )

    fun getMatchingTypeProperty(type: Type, property: String) = requireNotNull(
        value = globalScope.getMatchingTypeProperty(type, property),
        lazyMessage = { "No matching type property found for $type.$property" },
    )

    fun getTypeProperties(type: Type) = requireNotNull(
        value = globalScope.getTypeProperties(type),
        lazyMessage = { "No properties found for $type" },
    )

    fun resolveExpressionType(expression: Expression): Type =
        typeResolver.resolveExpressionType(expression)
}
