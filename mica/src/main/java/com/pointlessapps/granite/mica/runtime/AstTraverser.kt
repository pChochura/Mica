package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.IfConditionExpression
import com.pointlessapps.granite.mica.ast.expressions.InterpolatedStringExpression
import com.pointlessapps.granite.mica.ast.expressions.MapLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.SetLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeCoercionExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.LoopInStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Token.AssignmentOperator.Type
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Instruction.AcceptInput
import com.pointlessapps.granite.mica.runtime.model.Instruction.AssignVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.CreateCustomObject
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareCustomObjectProperties
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareFunction
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareScope
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareType
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.DuplicateLastStackItems
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteAccessorGetExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteAccessorSetExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLengthExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteBinaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteFunctionCallExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteMapLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteSetLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteTypeCoercionExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteTypeExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteUnaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExitScope
import com.pointlessapps.granite.mica.runtime.model.Instruction.Jump
import com.pointlessapps.granite.mica.runtime.model.Instruction.JumpIf
import com.pointlessapps.granite.mica.runtime.model.Instruction.Label
import com.pointlessapps.granite.mica.runtime.model.Instruction.Print
import com.pointlessapps.granite.mica.runtime.model.Instruction.PushToStack
import com.pointlessapps.granite.mica.runtime.model.Instruction.RestoreToStack
import com.pointlessapps.granite.mica.runtime.model.Instruction.ReturnFromFunction
import com.pointlessapps.granite.mica.runtime.model.Instruction.SaveFromStack
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object AstTraverser {

    private class TraversalContext(
        val currentLoopEndLabel: String?,
        val scopeLevel: Int,
    ) {
        companion object {
            val EMPTY = TraversalContext(currentLoopEndLabel = null, scopeLevel = 0)
        }
    }

    private var uniqueId: Int = 0
        get() {
            field = field + 1
            return field
        }

    fun traverse(root: Root): List<Instruction> {
        uniqueId = 0

        val context = TraversalContext(currentLoopEndLabel = null, scopeLevel = 0)
        val instructions = root.statements.flatMap { traverseAst(it, context) }
        backPatch(instructions)

        return instructions
    }

    private fun backPatch(instructions: List<Instruction>) {
        val labelMap = mutableMapOf<String, Int>()
        instructions.forEachIndexed { index, instruction ->
            if (instruction is Label) {
                labelMap[instruction.label] = index
            }
        }

        instructions.onEach { instruction ->
            if (instruction is Jump) {
                instruction.index = requireNotNull(
                    value = labelMap[instruction.label],
                    lazyMessage = { "${instruction.label} was not found" },
                )
            } else if (instruction is JumpIf) {
                instruction.index = requireNotNull(
                    value = labelMap[instruction.label],
                    lazyMessage = { "${instruction.label} was not found" },
                )
            } else if (instruction is DeclareFunction) {
                instruction.index = requireNotNull(
                    value = labelMap[instruction.label],
                    lazyMessage = { "${instruction.label} was not found" },
                )
            }
        }
    }

    private fun traverseAst(
        statement: Statement,
        context: TraversalContext,
    ): List<Instruction> = when (statement) {
        is BreakStatement -> listOf(
            ExitScope,
            Jump(
                requireNotNull(
                    value = context.currentLoopEndLabel,
                    lazyMessage = { "Break statement can only be used inside loops" },
                ),
            ),
        )

        is ReturnStatement -> buildList {
            if (statement.returnExpression != null) {
                addAll(unfoldExpression(statement.returnExpression, false, context))
            }
            repeat(context.scopeLevel) { add(ExitScope) }
            add(ReturnFromFunction)
        }

        is LoopIfStatement -> traverseLoopIfStatement(statement, context)
        is LoopInStatement -> traverseLoopInStatement(statement, context)
        is TypeDeclarationStatement -> traverseTypeDeclarationStatement(statement)
        is ExpressionStatement -> unfoldExpression(statement.expression, true, context)
        is FunctionDeclarationStatement -> traverseFunctionDeclarationStatement(statement)
        is AssignmentStatement -> traverseAssignmentStatement(statement, context)
        is VariableDeclarationStatement -> buildList {
            addAll(unfoldExpression(statement.rhs, false, context))
            add(ExecuteTypeExpression(statement.typeExpression))
            add(DeclareVariable(statement.lhsToken.value))
        }

        is UserInputCallStatement -> listOf(
            AcceptInput,
            AssignVariable(statement.contentToken.value),
        )

        is UserOutputCallStatement -> buildList {
            addAll(unfoldExpression(statement.contentExpression, false, context))
            add(ExecuteFunctionCallExpression("toString", false, 1, true))
            add(Print)
        }
    }

    private fun traverseTypeDeclarationStatement(
        statement: TypeDeclarationStatement,
    ): List<Instruction> = buildList {
        val constructorId = uniqueId
        val constructorLabel = "Constructor_$constructorId"
        val endConstructorLabel = "EndConstructor_$constructorId"
        addAll(
            statement.properties.asReversed()
                .map { ExecuteTypeExpression(it.typeExpression) },
        )
        add(DeclareType(statement.nameToken.value))
        add(
            DeclareFunction(
                functionName = statement.nameToken.value,
                parametersCount = statement.properties.size,
                vararg = false,
                label = "Constructor_$constructorId",
            ),
        )
        add(Jump(endConstructorLabel))
        add(Label(constructorLabel))
        statement.properties.forEach { add(ExecuteTypeExpression(it.typeExpression)) }
        add(
            CreateCustomObject(
                typeName = statement.nameToken.value,
                propertyNames = statement.properties.map { it.nameToken.value },
            ),
        )
        add(ReturnFromFunction)
        add(Label(endConstructorLabel))

        statement.functions.forEach { function ->
            addAll(
                traverseFunctionDeclarationStatement(
                    statement = function,
                    typeParentNameToken = statement.nameToken,
                ),
            )
        }
    }

    private fun traverseAssignmentStatement(
        statement: AssignmentStatement,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        when (val token = statement.equalSignToken) {
            is Token.Equals -> {
                if (statement.accessorExpressions.isNotEmpty()) {
                    addAll(
                        unfoldExpression(
                            expression = SymbolExpression(statement.symbolToken),
                            asStatement = false,
                            context = context,
                        ),
                    )
                    statement.accessorExpressions.forEach {
                        when (it) {
                            is ArrayIndexAccessorExpression ->
                                addAll(unfoldExpression(it.indexExpression, false, context))

                            is PropertyAccessAccessorExpression ->
                                add(PushToStack(VariableType.Value(it.propertySymbolToken.value)))
                        }
                    }
                }
                addAll(unfoldExpression(statement.rhs, false, context))
            }

            is Token.AssignmentOperator -> {
                addAll(
                    unfoldExpression(
                        expression = SymbolExpression(statement.symbolToken),
                        asStatement = false,
                        context = context,
                    ),
                )
                if (statement.accessorExpressions.isNotEmpty()) {
                    statement.accessorExpressions.forEach {
                        when (it) {
                            is ArrayIndexAccessorExpression ->
                                addAll(unfoldExpression(it.indexExpression, false, context))

                            is PropertyAccessAccessorExpression ->
                                add(PushToStack(VariableType.Value(it.propertySymbolToken.value)))
                        }
                    }
                    add(DuplicateLastStackItems(1 + statement.accessorExpressions.size))
                    add(ExecuteAccessorGetExpression(statement.accessorExpressions.size))
                }
                addAll(unfoldExpression(statement.rhs, false, context))
                val operatorType = when (token.type) {
                    Type.PlusEquals -> Token.Operator.Type.Add
                    Type.MinusEquals -> Token.Operator.Type.Subtract
                    Type.MultiplyEquals -> Token.Operator.Type.Multiply
                    Type.DivideEquals -> Token.Operator.Type.Divide
                    Type.ModuloEquals -> Token.Operator.Type.Modulo
                    Type.ExponentEquals -> Token.Operator.Type.Exponent
                    Type.OrEquals -> Token.Operator.Type.Or
                    Type.AndEquals -> Token.Operator.Type.And
                }
                add(ExecuteBinaryOperation(operatorType))
            }

            else -> throw IllegalStateException("Unknown assignment operator")
        }

        if (statement.accessorExpressions.isNotEmpty()) {
            add(ExecuteAccessorSetExpression(statement.accessorExpressions.size))
        }
        add(AssignVariable(statement.symbolToken.value))
    }

    private fun traverseLoopIfStatement(
        statement: LoopIfStatement,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        val loopId = uniqueId
        val startLoopLabel = "Loop_$loopId"
        val loopBodyLabel = "LoopBody_$loopId"
        val elseLoopLabel = "ElseLoop_$loopId"
        val endLoopLabel = "EndLoop_$loopId"
        val loopContext = TraversalContext(
            currentLoopEndLabel = endLoopLabel,
            scopeLevel = context.scopeLevel + 1,
        )

        if (statement.ifConditionExpression != null) {
            // Compute the condition for the first time to check whether we have to
            // execute the else body
            addAll(unfoldExpression(statement.ifConditionExpression, false, context))
            add(JumpIf(false, elseLoopLabel))
            add(Jump(loopBodyLabel))

            add(Label(startLoopLabel))
            addAll(unfoldExpression(statement.ifConditionExpression, false, context))
            add(JumpIf(false, endLoopLabel))
        } else {
            add(Label(startLoopLabel))
        }

        add(Label(loopBodyLabel))
        add(DeclareScope)
        statement.loopBody.statements.forEach { addAll(traverseAst(it, loopContext)) }
        add(ExitScope)
        add(Jump(startLoopLabel))
        add(Label(elseLoopLabel))
        add(DeclareScope)
        statement.elseDeclaration?.elseBody?.statements?.forEach {
            addAll(traverseAst(it, loopContext))
        }
        add(ExitScope)
        add(Label(endLoopLabel))
    }

    private fun traverseLoopInStatement(
        statement: LoopInStatement,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        val loopId = uniqueId
        val startLoopLabel = "Loop_$loopId"
        val endLoopLabel = "EndLoop_$loopId"
        val loopContext = TraversalContext(
            currentLoopEndLabel = endLoopLabel,
            scopeLevel = context.scopeLevel + 1,
        )

        val indexToken = statement.indexToken ?: Token.Symbol(Location.EMPTY, "index_$loopId")

        add(PushToStack(VariableType.Value(0L)))
        add(AssignVariable(indexToken.value))

        add(Label(startLoopLabel))
        add(ExecuteExpression(SymbolExpression(indexToken)))
        addAll(unfoldExpression(statement.arrayExpression, false, context))
        add(ExecuteArrayLengthExpression)
        add(ExecuteBinaryOperation(Token.Operator.Type.LessThan))
        add(JumpIf(false, endLoopLabel))

        addAll(unfoldExpression(statement.arrayExpression, false, context))
        add(ExecuteExpression(SymbolExpression(indexToken)))
        add(ExecuteAccessorGetExpression(1))
        add(AssignVariable(statement.symbolToken.value))

        add(DeclareScope)
        statement.loopBody.statements.forEach { addAll(traverseAst(it, loopContext)) }
        add(ExitScope)

        add(ExecuteExpression(SymbolExpression(indexToken)))
        add(PushToStack(VariableType.Value(1L)))
        add(ExecuteBinaryOperation(Token.Operator.Type.Add))
        add(AssignVariable(indexToken.value))

        add(Jump(startLoopLabel))
        add(Label(endLoopLabel))
    }

    private fun traverseFunctionDeclarationStatement(
        statement: FunctionDeclarationStatement,
        typeParentNameToken: Token.Symbol? = null,
    ): List<Instruction> = buildList {
        val functionId = uniqueId
        val functionBaseLabel = "Function_${functionId}_"
        val endFunctionLabel = "EndFunction_$functionId"

        val parametersCount = statement.parameters.size + if (typeParentNameToken != null) 1 else 0

        // Use the actual parameters count for default parameters
        val defaultParametersCount = statement.parameters.size - (
                statement.parameters
                    .indexOfFirst { it.defaultValueExpression != null }
                    .takeIf { it != -1 } ?: statement.parameters.size
                )

        addAll(
            statement.parameters.asReversed()
                .map { ExecuteTypeExpression(it.typeExpression) },
        )

        if (typeParentNameToken != null) {
            add(ExecuteTypeExpression(SymbolTypeExpression(typeParentNameToken)))
        }

        // Declare functions default parameters
        repeat(defaultParametersCount) {
            val currentDefaultParametersCount = defaultParametersCount - it
            add(DuplicateLastStackItems(currentDefaultParametersCount))
            add(
                DeclareFunction(
                    functionName = statement.nameToken.value,
                    parametersCount = parametersCount - currentDefaultParametersCount,
                    vararg = false,
                    label = "${functionBaseLabel}$currentDefaultParametersCount",
                ),
            )
        }

        val isVararg = statement.parameters.lastOrNull()?.varargToken != null
        add(
            DeclareFunction(
                functionName = statement.nameToken.value,
                parametersCount = parametersCount,
                vararg = isVararg,
                label = "${functionBaseLabel}0",
            ),
        )
        add(Jump(endFunctionLabel))

        // Declare the default values for the parameters
        statement.parameters.takeLast(defaultParametersCount).forEachIndexed { index, function ->
            val currentDefaultParametersCount = defaultParametersCount - index
            if (function.defaultValueExpression != null) {
                add(Label("${functionBaseLabel}$currentDefaultParametersCount"))
                addAll(
                    unfoldExpression(
                        expression = function.defaultValueExpression,
                        asStatement = false,
                        context = TraversalContext.EMPTY,
                    ),
                )
            }
        }

        add(Label("${functionBaseLabel}0"))

        // All of the arguments are loaded onto the stack
        // Assign variables in the reverse order
        statement.parameters.asReversed().forEach {
            add(ExecuteTypeExpression(it.typeExpression))
            add(DeclareVariable(it.nameToken.value))
        }

        // Declare the properties of the type as variables and provide the this variable
        if (typeParentNameToken != null) {
            add(DuplicateLastStackItems(1))
            add(ExecuteTypeExpression(SymbolTypeExpression(typeParentNameToken)))
            add(DeclareVariable("this"))
            add(DeclareCustomObjectProperties)
        }

        statement.body.forEach { addAll(traverseAst(it, TraversalContext.EMPTY)) }
        add(ReturnFromFunction)
        add(Label(endFunctionLabel))
    }

    /**
     * If [asStatement] is true, make sure there are no left values on the stack
     */
    private fun unfoldExpression(
        expression: Expression,
        asStatement: Boolean,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        when (expression) {
            is BooleanLiteralExpression, is CharLiteralExpression,
            is NumberLiteralExpression, is StringLiteralExpression,
            is SymbolExpression,
                -> if (!asStatement) add(ExecuteExpression(expression))

            is InterpolatedStringExpression -> {
                expression.expressions.forEach {
                    addAll(unfoldExpression(it, asStatement, context))
                }
                if (!asStatement && expression.expressions.singleOrNull() !is StringLiteralExpression) {
                    add(ExecuteArrayLiteralExpression(expression.expressions.size))
                    add(PushToStack(VariableType.Value("")))
                    add(ExecuteFunctionCallExpression("join", false, 2, true))
                }
            }

            is ParenthesisedExpression ->
                addAll(unfoldExpression(expression.expression, asStatement, context))

            is ArrayLiteralExpression -> {
                expression.elements.forEach { addAll(unfoldExpression(it, asStatement, context)) }
                if (!asStatement) add(ExecuteArrayLiteralExpression(expression.elements.size))
            }

            is SetLiteralExpression -> {
                expression.elements.forEach { addAll(unfoldExpression(it, asStatement, context)) }
                if (!asStatement) add(ExecuteSetLiteralExpression(expression.elements.size))
            }

            is MapLiteralExpression -> {
                expression.keyValuePairs.forEach {
                    addAll(unfoldExpression(it.valueExpression, asStatement, context))
                    addAll(unfoldExpression(it.keyExpression, asStatement, context))
                }
                if (!asStatement) add(ExecuteMapLiteralExpression(expression.keyValuePairs.size))
            }

            is UnaryExpression -> {
                addAll(unfoldExpression(expression.rhs, asStatement, context))
                if (!asStatement) add(ExecuteUnaryOperation(expression.operatorToken.type))
            }

            is TypeCoercionExpression -> {
                addAll(unfoldExpression(expression.lhs, asStatement, context))
                if (!asStatement) {
                    add(ExecuteTypeExpression(expression.typeExpression))
                    add(ExecuteTypeCoercionExpression)
                }
            }

            is MemberAccessExpression -> {
                addAll(unfoldExpression(expression.symbolExpression, asStatement, context))
                expression.accessorExpressions.forEach {
                    when (it) {
                        is ArrayIndexAccessorExpression ->
                            addAll(unfoldExpression(it.indexExpression, false, context))

                        is PropertyAccessAccessorExpression ->
                            add(PushToStack(VariableType.Value(it.propertySymbolToken.value)))
                    }
                }
                if (!asStatement) {
                    add(ExecuteAccessorGetExpression(expression.accessorExpressions.size))
                }
            }

            is AffixAssignmentExpression ->
                addAll(unfoldAffixAssignmentExpression(expression, asStatement, context))

            is BinaryExpression -> addAll(unfoldBinaryExpression(expression, asStatement, context))

            is IfConditionExpression ->
                addAll(unfoldIfConditionExpression(expression, asStatement, context))

            is FunctionCallExpression -> {
                if (expression.typeArgument != null) add(ExecuteTypeExpression(expression.typeArgument))
                expression.arguments.forEach { addAll(unfoldExpression(it, false, context)) }
                add(
                    ExecuteFunctionCallExpression(
                        functionName = expression.nameToken.value,
                        hasTypeArgument = expression.typeArgument != null,
                        argumentsCount = expression.arguments.size,
                        keepReturnValue = !asStatement,
                    ),
                )
            }

            is TypeExpression -> throw IllegalStateException("Such expression should not be unfolded")
        }
    }

    /**
     * Short-circuit the expression if the lhs of the condition is enough
     * If [asStatement] is true, make sure there are no left values on the stack
     */
    private fun unfoldBinaryExpression(
        expression: BinaryExpression,
        asStatement: Boolean,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        when (expression.operatorToken.type) {
            Token.Operator.Type.Or -> {
                val skipOrRhsLabel = "SkipOrRhs_$uniqueId"
                addAll(unfoldExpression(expression.lhs, false, context))
                if (!asStatement) add(DuplicateLastStackItems(1))
                add(JumpIf(true, skipOrRhsLabel))
                addAll(unfoldExpression(expression.rhs, asStatement, context))
                if (!asStatement) add(ExecuteBinaryOperation(expression.operatorToken.type))
                add(Label(skipOrRhsLabel))
            }

            Token.Operator.Type.And -> {
                val skipAndRhsLabel = "SkipAndRhs_$uniqueId"
                addAll(unfoldExpression(expression.lhs, false, context))
                if (!asStatement) add(DuplicateLastStackItems(1))
                add(JumpIf(false, skipAndRhsLabel))
                addAll(unfoldExpression(expression.rhs, asStatement, context))
                if (!asStatement) add(ExecuteBinaryOperation(expression.operatorToken.type))
                add(Label(skipAndRhsLabel))
            }

            else -> {
                addAll(unfoldExpression(expression.lhs, asStatement, context))
                addAll(unfoldExpression(expression.rhs, asStatement, context))
                if (!asStatement) add(ExecuteBinaryOperation(expression.operatorToken.type))
            }
        }
    }

    /**
     * If [asStatement] is true, make sure there are no left values on the stack
     */
    private fun unfoldAffixAssignmentExpression(
        expression: AffixAssignmentExpression,
        asStatement: Boolean,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        addAll(unfoldExpression(SymbolExpression(expression.symbolToken), false, context))
        if (expression.accessorExpressions.isNotEmpty()) {
            expression.accessorExpressions.forEach {
                when (it) {
                    is ArrayIndexAccessorExpression ->
                        addAll(unfoldExpression(it.indexExpression, false, context))

                    is PropertyAccessAccessorExpression ->
                        add(PushToStack(VariableType.Value(it.propertySymbolToken.value)))
                }
            }
            add(DuplicateLastStackItems(1 + expression.accessorExpressions.size))
            add(ExecuteAccessorGetExpression(expression.accessorExpressions.size))
        }

        // Don't save the result onto the stack if it's an assignment statement
        if (expression is PostfixAssignmentExpression && !asStatement) add(SaveFromStack)

        add(PushToStack(VariableType.Value(1L)))
        add(
            ExecuteBinaryOperation(
                when (expression.operatorToken) {
                    is Token.Increment -> Token.Operator.Type.Add
                    is Token.Decrement -> Token.Operator.Type.Subtract
                    else -> throw IllegalStateException("Unknown prefix assignment operator")
                }
            ),
        )

        if (expression is PrefixAssignmentExpression && !asStatement) add(SaveFromStack)

        if (expression.accessorExpressions.isNotEmpty()) {
            add(ExecuteAccessorSetExpression(expression.accessorExpressions.size))
        }

        add(AssignVariable(expression.symbolToken.value))
        if (!asStatement) add(RestoreToStack)
    }


    private fun unfoldIfConditionExpression(
        expression: IfConditionExpression,
        asStatement: Boolean,
        context: TraversalContext,
    ): List<Instruction> = buildList {
        val ifId = uniqueId
        val elseIfBaseLabel = "ElseIf_${ifId}_"
        val elseLabel = "Else_$ifId"
        val endIfLabel = "EndIf_$ifId"
        val ifContext = TraversalContext(
            currentLoopEndLabel = context.currentLoopEndLabel,
            scopeLevel = context.scopeLevel + 1,
        )

        addAll(
            unfoldExpression(
                expression = expression.ifConditionDeclaration.ifConditionExpression,
                asStatement = false,
                context = context,
            ),
        )
        val nextLabelForIf = when {
            !expression.elseIfConditionDeclarations.isNullOrEmpty() -> "${elseIfBaseLabel}0"
            expression.elseDeclaration != null -> elseLabel
            else -> endIfLabel
        }
        add(JumpIf(false, nextLabelForIf))
        add(DeclareScope)
        expression.ifConditionDeclaration.ifBody.statements.let {
            it.forEachIndexed { index, statement ->
                // If it's an if expression, leave the last expression result on the stack
                if (!asStatement && statement is ExpressionStatement && index == it.lastIndex) {
                    addAll(unfoldExpression(statement.expression, false, ifContext))
                } else {
                    addAll(traverseAst(statement, ifContext))
                }
            }
        }
        add(ExitScope)
        add(Jump(endIfLabel))

        expression.elseIfConditionDeclarations?.forEachIndexed { index, elseIf ->
            add(Label("${elseIfBaseLabel}$index"))
            addAll(unfoldExpression(elseIf.elseIfConditionExpression, false, context))
            val nextLabelForElseIf = when {
                index < expression.elseIfConditionDeclarations.lastIndex -> "${elseIfBaseLabel}${index + 1}"
                expression.elseDeclaration != null -> elseLabel
                else -> endIfLabel
            }
            add(JumpIf(false, nextLabelForElseIf))
            add(DeclareScope)
            elseIf.elseIfBody.statements.let {
                it.forEachIndexed { index, statement ->
                    // If it's an if expression, leave the last expression result on the stack
                    if (!asStatement && statement is ExpressionStatement && index == it.lastIndex) {
                        addAll(unfoldExpression(statement.expression, false, ifContext))
                    } else {
                        addAll(traverseAst(statement, ifContext))
                    }
                }
            }
            add(ExitScope)
            add(Jump(endIfLabel))
        }

        add(Label(elseLabel))
        add(DeclareScope)
        expression.elseDeclaration?.elseBody?.statements?.let {
            it.forEachIndexed { index, statement ->
                // If it's an if expression, leave the last expression result on the stack
                if (!asStatement && statement is ExpressionStatement && index == it.lastIndex) {
                    addAll(unfoldExpression(statement.expression, false, ifContext))
                } else {
                    addAll(traverseAst(statement, ifContext))
                }
            }
        }
        add(ExitScope)
        add(Label(endIfLabel))
    }
}
