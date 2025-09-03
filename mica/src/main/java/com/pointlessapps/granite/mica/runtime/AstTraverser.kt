package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
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
import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
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
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayIndexGetExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayIndexSetExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLengthExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteBinaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteCustomObjectPropertyAccessExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteFunctionCallExpression
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
import com.pointlessapps.granite.mica.runtime.model.IntVariable

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

        is ReturnStatement -> (1..context.scopeLevel).map { ExitScope }
            .plus(statement.returnExpression?.let(::unfoldExpression).orEmpty())
            .plus(ReturnFromFunction)

        is LoopIfStatement -> traverseLoopIfStatement(statement, context)
        is LoopInStatement -> traverseLoopInStatement(statement, context)
        is TypeDeclarationStatement -> traverseTypeDeclarationStatement(statement)
        is ExpressionStatement -> unfoldExpression(statement.expression, asStatement = true)
        is FunctionDeclarationStatement -> traverseFunctionDeclarationStatement(statement)
        is IfConditionStatement -> traverseIfConditionStatement(statement, context)
        is AssignmentStatement -> traverseAssignmentStatement(statement)
        is ArrayAssignmentStatement -> traverseArrayAssignmentStatement(statement)
        is VariableDeclarationStatement -> unfoldExpression(statement.rhs)
            .plus(ExecuteTypeExpression(statement.typeExpression))
            .plus(DeclareVariable(statement.lhsToken.value))

        is UserInputCallStatement -> listOf(
            AcceptInput,
            AssignVariable(statement.contentToken.value),
        )

        is UserOutputCallStatement -> unfoldExpression(statement.contentExpression)
            .plus(ExecuteFunctionCallExpression("toString", 1))
            .plus(Print)
    }

    private fun traverseTypeDeclarationStatement(
        statement: TypeDeclarationStatement,
    ): List<Instruction> {
        return buildList {
            add(DeclareType(statement.nameToken.value))

            val constructorId = uniqueId
            val constructorLabel = "Constructor_$constructorId"
            val endConstructorLabel = "EndConstructor_$constructorId"
            addAll(
                statement.properties.asReversed()
                    .map { ExecuteTypeExpression(it.typeExpression) },
            )
            add(
                DeclareFunction(
                    functionName = statement.nameToken.value,
                    parametersCount = statement.properties.size,
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
    }

    private fun traverseArrayAssignmentStatement(
        statement: ArrayAssignmentStatement,
    ): List<Instruction> = buildList {
        when (statement.equalSignToken) {
            is Token.Equals -> {
                addAll(unfoldExpression(SymbolExpression(statement.arraySymbolToken)))
                statement.indexExpressions.forEach { addAll(unfoldExpression(it.expression)) }
                addAll(unfoldExpression(statement.rhs))
            }

            is Token.PlusEquals -> {
                addAll(unfoldExpression(SymbolExpression(statement.arraySymbolToken)))
                statement.indexExpressions.forEach { addAll(unfoldExpression(it.expression)) }
                add(DuplicateLastStackItems(1 + statement.indexExpressions.size))
                add(ExecuteArrayIndexGetExpression(statement.indexExpressions.size))
                addAll(unfoldExpression(statement.rhs))
                add(ExecuteBinaryOperation(Token.Operator.Type.Add))
            }

            is Token.MinusEquals -> {
                addAll(unfoldExpression(SymbolExpression(statement.arraySymbolToken)))
                statement.indexExpressions.forEach { addAll(unfoldExpression(it.expression)) }
                add(DuplicateLastStackItems(1 + statement.indexExpressions.size))
                add(ExecuteArrayIndexGetExpression(statement.indexExpressions.size))
                addAll(unfoldExpression(statement.rhs))
                add(ExecuteBinaryOperation(Token.Operator.Type.Subtract))
            }

            else -> throw IllegalStateException("Unknown assignment operator")
        }

        add(ExecuteArrayIndexSetExpression(statement.indexExpressions.size))
        add(AssignVariable(statement.arraySymbolToken.value))
    }

    private fun traverseAssignmentStatement(
        statement: AssignmentStatement,
    ): List<Instruction> = buildList {
        when (statement.equalSignToken) {
            is Token.Equals -> addAll(unfoldExpression(statement.rhs))
            is Token.PlusEquals -> {
                addAll(unfoldExpression(SymbolExpression(statement.lhsToken)))
                addAll(unfoldExpression(statement.rhs))
                add(ExecuteBinaryOperation(Token.Operator.Type.Add))
            }

            is Token.MinusEquals -> {
                addAll(unfoldExpression(SymbolExpression(statement.lhsToken)))
                addAll(unfoldExpression(statement.rhs))
                add(ExecuteBinaryOperation(Token.Operator.Type.Subtract))
            }

            else -> throw IllegalStateException("Unknown assignment operator")
        }

        add(AssignVariable(statement.lhsToken.value))
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
            addAll(unfoldExpression(statement.ifConditionExpression))
            add(JumpIf(false, elseLoopLabel))
            add(Jump(loopBodyLabel))

            add(Label(startLoopLabel))
            addAll(unfoldExpression(statement.ifConditionExpression))
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

        add(PushToStack(IntVariable(0)))
        add(DuplicateLastStackItems(1))
        add(DeclareVariable(indexToken.value))

        add(Label(startLoopLabel))
        add(ExecuteExpression(SymbolExpression(indexToken)))
        addAll(unfoldExpression(statement.arrayExpression))
        add(ExecuteArrayLengthExpression)
        add(ExecuteBinaryOperation(Token.Operator.Type.LessThan))
        add(JumpIf(false, endLoopLabel))

        addAll(unfoldExpression(statement.arrayExpression))
        add(ExecuteExpression(SymbolExpression(indexToken)))
        add(ExecuteArrayIndexGetExpression(1))
        add(DuplicateLastStackItems(1))
        add(DeclareVariable(statement.symbolToken.value))

        add(DeclareScope)
        statement.loopBody.statements.forEach { addAll(traverseAst(it, loopContext)) }
        add(ExitScope)

        add(ExecuteExpression(SymbolExpression(indexToken)))
        add(PushToStack(IntVariable(1)))
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

        val defaultParametersCount = parametersCount - (
                statement.parameters
                    .indexOfFirst { it.defaultValueExpression != null }
                    .takeIf { it != -1 } ?: parametersCount
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
            add(DuplicateLastStackItems(defaultParametersCount - it))
            add(
                DeclareFunction(
                    functionName = statement.nameToken.value,
                    parametersCount = parametersCount - currentDefaultParametersCount,
                    label = "${functionBaseLabel}$currentDefaultParametersCount",
                ),
            )
        }

        add(
            DeclareFunction(
                functionName = statement.nameToken.value,
                parametersCount = parametersCount,
                label = "${functionBaseLabel}0",
            ),
        )
        add(Jump(endFunctionLabel))

        // Declare the default values for the parameters
        statement.parameters.takeLast(defaultParametersCount).forEachIndexed { index, function ->
            val currentDefaultParametersCount = defaultParametersCount - index
            if (function.defaultValueExpression != null) {
                add(Label("${functionBaseLabel}$currentDefaultParametersCount"))
                addAll(unfoldExpression(function.defaultValueExpression))
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

    private fun traverseIfConditionStatement(
        statement: IfConditionStatement,
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

        addAll(unfoldExpression(statement.ifConditionDeclaration.ifConditionExpression))
        val nextLabelForIf = when {
            !statement.elseIfConditionDeclarations.isNullOrEmpty() -> "${elseIfBaseLabel}0"
            statement.elseDeclaration != null -> elseLabel
            else -> endIfLabel
        }
        add(JumpIf(false, nextLabelForIf))
        add(DeclareScope)
        statement.ifConditionDeclaration.ifBody.statements.forEach {
            addAll(traverseAst(it, ifContext))
        }
        add(ExitScope)
        add(Jump(endIfLabel))

        statement.elseIfConditionDeclarations?.forEachIndexed { index, elseIf ->
            add(Label("${elseIfBaseLabel}$index"))
            addAll(unfoldExpression(elseIf.elseIfConditionExpression))
            val nextLabelForElseIf = when {
                index < statement.elseIfConditionDeclarations.lastIndex -> "${elseIfBaseLabel}${index + 1}"
                statement.elseDeclaration != null -> elseLabel
                else -> endIfLabel
            }
            add(JumpIf(false, nextLabelForElseIf))
            add(DeclareScope)
            elseIf.elseIfBody.statements.forEach { addAll(traverseAst(it, ifContext)) }
            add(ExitScope)
            add(Jump(endIfLabel))
        }

        add(Label(elseLabel))
        add(DeclareScope)
        statement.elseDeclaration?.elseBody?.statements?.forEach {
            addAll(traverseAst(it, ifContext))
        }
        add(ExitScope)
        add(Label(endIfLabel))
    }

    private fun unfoldExpression(
        expression: Expression,
        asStatement: Boolean = false,
    ): List<Instruction> = buildList {
        when (expression) {
            is BooleanLiteralExpression, is CharLiteralExpression,
            is NumberLiteralExpression, is StringLiteralExpression,
            is SymbolExpression,
                -> add(ExecuteExpression(expression))

            is ParenthesisedExpression -> addAll(unfoldExpression(expression.expression))
            is ArrayLiteralExpression -> {
                expression.elements.forEach { addAll(unfoldExpression(it)) }
                add(ExecuteArrayLiteralExpression(expression.elements.size))
            }

            is SetLiteralExpression -> {
                expression.elements.forEach { addAll(unfoldExpression(it)) }
                add(ExecuteSetLiteralExpression(expression.elements.size))
            }

            is UnaryExpression -> {
                addAll(unfoldExpression(expression.rhs))
                add(ExecuteUnaryOperation(expression.operatorToken.type))
            }

            is TypeCoercionExpression -> {
                addAll(unfoldExpression(expression.lhs))
                add(ExecuteTypeExpression(expression.typeExpression))
                add(ExecuteTypeCoercionExpression)
            }

            is MemberAccessExpression -> {
                addAll(unfoldExpression(expression.lhs))
                add(ExecuteCustomObjectPropertyAccessExpression(expression.propertySymbolToken.value))
            }

            is AffixAssignmentExpression ->
                addAll(unfoldAffixAssignmentExpression(expression, asStatement))

            is BinaryExpression -> addAll(unfoldBinaryExpression(expression))
            is ArrayIndexExpression -> {
                addAll(unfoldExpression(expression.arrayExpression))
                addAll(unfoldExpression(expression.indexExpression))
                add(ExecuteArrayIndexGetExpression(1))
            }

            is FunctionCallExpression -> {
                expression.arguments.forEach { addAll(unfoldExpression(it)) }
                add(
                    ExecuteFunctionCallExpression(
                        functionName = expression.nameToken.value,
                        argumentsCount = expression.arguments.size,
                    ),
                )
            }

            is EmptyExpression, is TypeExpression ->
                throw IllegalStateException("Such expression should not be unfolded")
        }
    }

    // Short-circuit the expression if the lhs of the condition is enough
    private fun unfoldBinaryExpression(
        expression: BinaryExpression,
    ): List<Instruction> = buildList {
        when (expression.operatorToken.type) {
            Token.Operator.Type.Or -> {
                val skipOrRhsLabel = "SkipOrRhs_$uniqueId"
                addAll(unfoldExpression(expression.lhs))
                add(DuplicateLastStackItems(1))
                add(JumpIf(true, skipOrRhsLabel))
                addAll(unfoldExpression(expression.rhs))
                add(ExecuteBinaryOperation(expression.operatorToken.type))
                add(Label(skipOrRhsLabel))
            }

            Token.Operator.Type.And -> {
                val skipAndRhsLabel = "SkipAndRhs_$uniqueId"
                addAll(unfoldExpression(expression.lhs))
                add(DuplicateLastStackItems(1))
                add(JumpIf(false, skipAndRhsLabel))
                addAll(unfoldExpression(expression.rhs))
                add(ExecuteBinaryOperation(expression.operatorToken.type))
                add(Label(skipAndRhsLabel))
            }

            else -> {
                addAll(unfoldExpression(expression.lhs))
                addAll(unfoldExpression(expression.rhs))
                add(ExecuteBinaryOperation(expression.operatorToken.type))
            }
        }
    }

    private fun unfoldAffixAssignmentExpression(
        expression: AffixAssignmentExpression,
        asStatement: Boolean,
    ): List<Instruction> = buildList {
        addAll(unfoldExpression(SymbolExpression(expression.symbolToken)))
        if (expression.indexExpressions.isNotEmpty()) {
            expression.indexExpressions.forEach { addAll(unfoldExpression(it.expression)) }
            add(DuplicateLastStackItems(1 + expression.indexExpressions.size))
            add(ExecuteArrayIndexGetExpression(expression.indexExpressions.size))
        }

        // Don't save the result onto the stack if it's an assignment statement
        if (expression is PostfixAssignmentExpression && !asStatement) add(SaveFromStack)

        add(PushToStack(IntVariable(1L)))
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

        if (expression.indexExpressions.isNotEmpty()) {
            add(ExecuteArrayIndexSetExpression(expression.indexExpressions.size))
        }

        add(AssignVariable(expression.symbolToken.value))
        if (!asStatement) add(RestoreToStack)
    }
}
