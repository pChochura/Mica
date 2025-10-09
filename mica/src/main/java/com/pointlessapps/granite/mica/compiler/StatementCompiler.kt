package com.pointlessapps.granite.mica.compiler

import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.LoopInStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.compiler.errors.CompileTimeException
import com.pointlessapps.granite.mica.compiler.helper.uniqueId
import com.pointlessapps.granite.mica.compiler.model.AccessIndex
import com.pointlessapps.granite.mica.compiler.model.BinaryAdd
import com.pointlessapps.granite.mica.compiler.model.BinaryAnd
import com.pointlessapps.granite.mica.compiler.model.BinaryDivide
import com.pointlessapps.granite.mica.compiler.model.BinaryExponent
import com.pointlessapps.granite.mica.compiler.model.BinaryLessThan
import com.pointlessapps.granite.mica.compiler.model.BinaryModulo
import com.pointlessapps.granite.mica.compiler.model.BinaryMultiply
import com.pointlessapps.granite.mica.compiler.model.BinaryOr
import com.pointlessapps.granite.mica.compiler.model.BinarySubtract
import com.pointlessapps.granite.mica.compiler.model.CallBuiltin
import com.pointlessapps.granite.mica.compiler.model.CastAs
import com.pointlessapps.granite.mica.compiler.model.CompilerContext
import com.pointlessapps.granite.mica.compiler.model.CompilerInstruction
import com.pointlessapps.granite.mica.compiler.model.Dup
import com.pointlessapps.granite.mica.compiler.model.Jump
import com.pointlessapps.granite.mica.compiler.model.JumpIfFalse
import com.pointlessapps.granite.mica.compiler.model.Label
import com.pointlessapps.granite.mica.compiler.model.Load
import com.pointlessapps.granite.mica.compiler.model.NewObject
import com.pointlessapps.granite.mica.compiler.model.Print
import com.pointlessapps.granite.mica.compiler.model.Push
import com.pointlessapps.granite.mica.compiler.model.ReadInput
import com.pointlessapps.granite.mica.compiler.model.Return
import com.pointlessapps.granite.mica.compiler.model.Rot2
import com.pointlessapps.granite.mica.compiler.model.Store
import com.pointlessapps.granite.mica.compiler.model.StoreAtIndex
import com.pointlessapps.granite.mica.compiler.model.StoreLocal
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.AndEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.DivideEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.ExponentEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.MinusEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.ModuloEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.MultiplyEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.OrEquals
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type.PlusEquals
import com.pointlessapps.granite.mica.model.UndefinedType

internal fun traverseStatement(
    statement: Statement,
    context: CompilerContext,
): List<CompilerInstruction> = with(statement) {
    when (this) {
        is ExpressionStatement -> compile(context)
        is BreakStatement -> compile(context)
        is AssignmentStatement -> compile(context)
        is VariableDeclarationStatement -> compile(context)
        is FunctionDeclarationStatement -> compile(context)
        is LoopIfStatement -> compile(context)
        is LoopInStatement -> compile(context)
        is ReturnStatement -> compile(context)
        is TypeDeclarationStatement -> compile(context)
        is UserInputCallStatement -> compile(context)
        is UserOutputCallStatement -> compile(context)
    }
}

private fun ExpressionStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = expression.unfoldExpression(context, keepReturnValue = false)

private fun BreakStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = listOf(
    Jump(
        requireNotNull(
            value = context.removeLastLoopLabel(),
            lazyMessage = { "Break statement can only be used inside loops" },
        ),
    ),
)

private fun AssignmentStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = when (equalSignToken) {
    is Token.Equals -> buildList {
        if (accessorExpressions.isNotEmpty()) add(Push(symbolToken.value))
        accessorExpressions.dropLast(1).forEach {
            when (it) {
                is PropertyAccessAccessorExpression -> add(Push(it.propertySymbolToken.value))
                is ArrayIndexAccessorExpression ->
                    addAll(it.indexExpression.unfoldExpression(context, keepReturnValue = true))
            }
            add(AccessIndex)
        }
        if (accessorExpressions.isNotEmpty()) {
            when (val accessor = accessorExpressions.last()) {
                is PropertyAccessAccessorExpression -> add(Push(accessor.propertySymbolToken.value))
                is ArrayIndexAccessorExpression ->
                    addAll(accessor.indexExpression.unfoldExpression(context, true))
            }
            addAll(rhs.unfoldExpression(context, keepReturnValue = true))
            add(StoreAtIndex)
        } else {
            addAll(rhs.unfoldExpression(context, keepReturnValue = true))
            add(
                if (context.containsVariable(symbolToken.value)) {
                    Store(symbolToken.value)
                } else {
                    StoreLocal(symbolToken.value)
                },
            )
            context.declareVariable(
                name = symbolToken.value,
                type = context.resolveExpressionType(rhs),
            )
        }
    }

    is Token.AssignmentOperator -> buildList {
        val hasAccessors = accessorExpressions.isNotEmpty()
        if (hasAccessors) {
            add(Push(symbolToken.value))
        } else {
            add(Load(symbolToken.value))
        }
        accessorExpressions.forEachIndexed { index, it ->
            // Use the value of the symbol right before the last accessor to have it on the stack
            // to be used in the assignment.
            if (index == accessorExpressions.lastIndex) add(Dup)

            when (it) {
                is PropertyAccessAccessorExpression -> add(Push(it.propertySymbolToken.value))
                is ArrayIndexAccessorExpression ->
                    addAll(it.indexExpression.unfoldExpression(context, keepReturnValue = true))
            }
            add(AccessIndex)
        }
        addAll(rhs.unfoldExpression(context, keepReturnValue = true))
        add(
            when (equalSignToken.type) {
                PlusEquals -> BinaryAdd
                MinusEquals -> BinarySubtract
                MultiplyEquals -> BinaryMultiply
                DivideEquals -> BinaryDivide
                ModuloEquals -> BinaryModulo
                ExponentEquals -> BinaryExponent
                OrEquals -> BinaryOr
                AndEquals -> BinaryAnd
            },
        )

        if (hasAccessors) {
            when (val accessor = accessorExpressions.last()) {
                is PropertyAccessAccessorExpression -> add(Push(accessor.propertySymbolToken.value))
                is ArrayIndexAccessorExpression ->
                    addAll(accessor.indexExpression.unfoldExpression(context, true))
            }
            add(Rot2)
            add(StoreAtIndex)
        } else {
            add(Store(symbolToken.value))
        }
    }

    else -> throw CompileTimeException { "Unknown assignment operator: $equalSignToken" }
}

private fun VariableDeclarationStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    val expressionType = context.resolveExpressionType(typeExpression)
    addAll(rhs.unfoldExpression(context, keepReturnValue = true))
    add(CastAs(expressionType))
    add(StoreLocal(lhsToken.value))
    context.declareVariable(name = lhsToken.value, type = expressionType)
}

private fun FunctionDeclarationStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    val endFunctionLabel = "EndFunction_$uniqueId"

    val defaultParametersCount = parameters.count { it.defaultValueExpression != null }
    val returnType = returnTypeExpression?.let(context::resolveExpressionType) ?: UndefinedType
    val typeParameterConstraint = typeParameterConstraint?.let(context::resolveExpressionType)
    val parameterDeclarations = parameters.map {
        FunctionOverload.Parameter(
            type = context.resolveExpressionType(it.typeExpression),
            vararg = it.varargToken != null,
            resolver = if (it.exclamationMarkToken != null) {
                FunctionOverload.Parameter.Resolver.EXACT_MATCH
            } else {
                FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH
            },
        )
    }
    var temp = defaultParametersCount
    do {
        context.declareFunction(
            name = nameToken.value,
            typeParameterConstraint = typeParameterConstraint,
            parameters = parameterDeclarations
                .subList(0, parameterDeclarations.size - temp),
            returnType = returnType,
        )
    } while (temp-- > 0)
    add(Jump(endFunctionLabel))

    // Declare the default values for the parameters
    parameters.takeLast(defaultParametersCount).forEachIndexed { index, parameter ->
        val currentDefaultParametersCount = defaultParametersCount - index
        if (parameter.defaultValueExpression != null) {
            val signature = getSignature(
                name = nameToken.value,
                parameters = parameterDeclarations
                    .subList(0, parameterDeclarations.size - currentDefaultParametersCount),
            )
            add(Label(signature))
            addAll(parameter.defaultValueExpression.unfoldExpression(context, true))
        }
    }

    add(Label(getSignature(nameToken.value, parameterDeclarations)))

    // All of the arguments are loaded onto the stack
    // Assign variables in the reverse order
    parameters.asReversed().forEach {
        val type = context.resolveExpressionType(it.typeExpression)
        add(CastAs(type))
        add(StoreLocal(it.nameToken.value))
        context.declareVariable(name = it.nameToken.value, type = type)
    }

    body.forEach { addAll(traverseStatement(it, context)) }
    add(Return)
    add(Label(endFunctionLabel))
}

private fun LoopIfStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    val loopId = uniqueId
    val startLoopLabel = "Loop_$loopId"
    val loopBodyLabel = "LoopBody_$loopId"
    val elseLoopLabel = "ElseLoop_$loopId"
    val endLoopLabel = "EndLoop_$loopId"

    if (ifConditionExpression != null) {
        addAll(ifConditionExpression.unfoldExpression(context, keepReturnValue = true))
        add(JumpIfFalse(elseLoopLabel))
        add(Jump(loopBodyLabel))

        add(Label(startLoopLabel))
        addAll(ifConditionExpression.unfoldExpression(context, keepReturnValue = true))
        add(JumpIfFalse(endLoopLabel))
    } else {
        add(Label(startLoopLabel))
    }

    add(Label(loopBodyLabel))
    context.addLoopLabel(endLoopLabel)
    loopBody.statements.forEach { addAll(traverseStatement(it, context)) }
    context.removeLastLoopLabel()
    add(Jump(startLoopLabel))

    add(Label(elseLoopLabel))
    context.addLoopLabel(endLoopLabel)
    elseDeclaration?.elseBody?.statements?.forEach { addAll(traverseStatement(it, context)) }
    context.removeLastLoopLabel()
    add(Label(endLoopLabel))
}

private fun LoopInStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    val loopId = uniqueId
    val startLoopLabel = "Loop_$loopId"
    val endLoopLabel = "EndLoop_$loopId"

    val arrayToken = Token.Symbol(Location.EMPTY, "array_$loopId")
    addAll(arrayExpression.unfoldExpression(context, keepReturnValue = true))
    add(CastAs(ArrayType(AnyType)))
    add(StoreLocal(arrayToken.value))
    context.declareVariable(
        name = symbolToken.value,
        type = context.resolveExpressionType(arrayExpression)
            .superTypes.filterIsInstance<ArrayType>().first().elementType,
    )

    val indexToken = indexToken ?: Token.Symbol(Location.EMPTY, "index_$loopId")
    add(Push(0L))
    add(StoreLocal(indexToken.value))
    context.declareVariable(indexToken.value, IntType)

    add(Label(startLoopLabel))
    add(Load(indexToken.value))
    add(Load(arrayToken.value))
    add(CallBuiltin("length([any])"))
    add(BinaryLessThan)
    add(JumpIfFalse(endLoopLabel))

    add(Load(arrayToken.value))
    add(Load(indexToken.value))
    add(AccessIndex)
    add(StoreLocal(symbolToken.value))

    context.addLoopLabel(endLoopLabel)
    loopBody.statements.forEach { addAll(traverseStatement(it, context)) }
    context.removeLastLoopLabel()

    // index++
    add(Load(indexToken.value))
    add(Push(1L))
    add(BinaryAdd)
    add(StoreLocal(indexToken.value))
    add(Jump(startLoopLabel))

    add(Label(endLoopLabel))
}

private fun ReturnStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    returnExpression?.let { addAll(it.unfoldExpression(context, keepReturnValue = true)) }
    add(Return)
}

private fun TypeDeclarationStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    val constructorId = uniqueId
    val endConstructorLabel = "EndConstructor_$constructorId"

    val propertiesTypes = properties.associate {
        it.nameToken.value to context.resolveExpressionType(it.typeExpression)
    }
    val parentType = parentTypeExpression?.let(context::resolveExpressionType)
    val type = context.declareType(
        name = nameToken.value,
        parentType = parentType,
        properties = propertiesTypes,
    )
    val parameters = propertiesTypes
        .map { FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH.of(it.value) }
    context.declareFunction(
        name = nameToken.value,
        typeParameterConstraint = null,
        parameters = parameters,
        returnType = type,
    )
    functions.forEach {
        addAll(
            it.copy(
                parameters = listOf(
                    FunctionParameterDeclarationStatement(
                        varargToken = null,
                        nameToken = Token.Symbol(nameToken.location, "this"),
                        colonToken = Token.Colon(Location.EMPTY),
                        typeExpression = SymbolTypeExpression(nameToken, null, null),
                        exclamationMarkToken = null,
                        equalsToken = null,
                        defaultValueExpression = null,
                    ),
                ) + it.parameters,
            ).compile(context),
        )
    }
    add(Jump(endConstructorLabel))

    val signature = getSignature(
        name = nameToken.value,
        parameters = parameters,
    )
    add(Label(signature))
    propertiesTypes.forEach { add(Push(it.key)) }
    add(
        NewObject(
            name = nameToken.value,
            parentType = parentType,
            propertiesCount = propertiesTypes.size,
        )
    )
    add(Return)
    add(Label(endConstructorLabel))
}

private fun UserInputCallStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    add(ReadInput)
    add(Store(contentToken.value))
    context.declareVariable(contentToken.value, StringType)
}

private fun UserOutputCallStatement.compile(
    context: CompilerContext,
): List<CompilerInstruction> = buildList {
    addAll(contentExpression.unfoldExpression(context, keepReturnValue = true))
    add(Print)
}
