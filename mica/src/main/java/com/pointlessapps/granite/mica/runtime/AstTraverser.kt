package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
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
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteBinaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteFunctionCallExpression
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
    )

    private var uniqueId: Int = 0
        get() {
            field = field + 1
            return field
        }

    fun traverse(root: Root): List<Instruction> {
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
                instruction.index = requireNotNull(labelMap[instruction.label])
            } else if (instruction is JumpIf) {
                instruction.index = requireNotNull(labelMap[instruction.label])
            }
        }
    }

    private fun traverseAst(
        statement: Statement,
        context: TraversalContext,
    ): List<Instruction> = when (statement) {
        is BreakStatement -> listOf(
            ExitScope,
            Jump(requireNotNull(context.currentLoopEndLabel)),
        )

        is ReturnStatement -> (1..context.scopeLevel).map { ExitScope }
            .plus(statement.returnExpression?.let(::unfoldExpression).orEmpty())
            .plus(ReturnFromFunction)

        is LoopIfStatement -> traverseLoopIfStatement(statement, context)
        is TypeDeclarationStatement -> traverseTypeDeclarationStatement(statement)
        is ExpressionStatement -> unfoldExpression(statement.expression)
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

            val endConstructorLabel = "EndConstructor_$uniqueId"
            addAll(statement.properties.map { ExecuteTypeExpression(it.typeExpression) })
            add(DeclareFunction(statement.nameToken.value, statement.properties.size))
            add(Jump(endConstructorLabel))
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
                val endMemberFunctionLabel = "EndMemberFunction_$uniqueId"

                add(ExecuteTypeExpression(SymbolTypeExpression(statement.nameToken)))
                addAll(function.parameters.map { ExecuteTypeExpression(it.typeExpression) })
                add(DeclareFunction(function.nameToken.value, 1 + function.parameters.size))
                add(Jump(endMemberFunctionLabel))
                // All of the arguments are loaded onto the stack
                // Assign variables in the reverse order
                function.parameters.asReversed().forEach {
                    add(ExecuteTypeExpression(it.typeExpression))
                    add(DeclareVariable(it.nameToken.value))
                }
                add(DuplicateLastStackItems(1))
                add(ExecuteTypeExpression(SymbolTypeExpression(statement.nameToken)))
                add(DeclareVariable("this"))
                add(DeclareCustomObjectProperties)
                val functionContext = TraversalContext(
                    currentLoopEndLabel = null,
                    scopeLevel = 0,
                )
                function.body.forEach { addAll(traverseAst(it, functionContext)) }
                add(ReturnFromFunction)
                add(Label(endMemberFunctionLabel))
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
        val elseLoopLabel = "ElseLoop_$loopId"
        val endLoopLabel = "EndLoop_$loopId"
        val loopContext = TraversalContext(
            currentLoopEndLabel = endLoopLabel,
            scopeLevel = context.scopeLevel + 1,
        )

        add(Label(startLoopLabel))
        addAll(unfoldExpression(statement.ifConditionDeclaration.ifConditionExpression))
        add(JumpIf(false, elseLoopLabel))
        add(DeclareScope)
        statement.ifConditionDeclaration.ifBody
            .forEach { addAll(traverseAst(it, loopContext)) }
        add(ExitScope)
        add(Jump(startLoopLabel))
        add(Label(elseLoopLabel))
        add(DeclareScope)
        statement.elseDeclaration?.elseBody
            ?.forEach { addAll(traverseAst(it, loopContext)) }
        add(ExitScope)
        add(Label(endLoopLabel))
    }

    private fun traverseFunctionDeclarationStatement(
        statement: FunctionDeclarationStatement,
    ): List<Instruction> = buildList {
        val endFunctionLabel = "EndFunction_$uniqueId"

        addAll(statement.parameters.map { ExecuteTypeExpression(it.typeExpression) })
        add(DeclareFunction(statement.nameToken.value, statement.parameters.size))
        add(Jump(endFunctionLabel))
        // All of the arguments are loaded onto the stack
        // Assign variables in the reverse order
        statement.parameters.asReversed().forEach {
            add(ExecuteTypeExpression(it.typeExpression))
            add(DeclareVariable(it.nameToken.value))
        }
        val functionContext = TraversalContext(
            currentLoopEndLabel = null,
            scopeLevel = 0,
        )
        statement.body.forEach { addAll(traverseAst(it, functionContext)) }
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
        statement.ifConditionDeclaration.ifBody
            .forEach { addAll(traverseAst(it, ifContext)) }
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
            elseIf.elseIfBody.forEach { addAll(traverseAst(it, ifContext)) }
            add(ExitScope)
            add(Jump(endIfLabel))
        }

        add(Label(elseLabel))
        add(DeclareScope)
        statement.elseDeclaration?.elseBody
            ?.forEach { addAll(traverseAst(it, ifContext)) }
        add(ExitScope)
        add(Label(endIfLabel))
    }

    private fun unfoldExpression(
        expression: Expression,
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

            is UnaryExpression -> {
                addAll(unfoldExpression(expression.rhs))
                add(ExecuteUnaryOperation(expression.operatorToken.type))
            }

            is AffixAssignmentExpression -> addAll(unfoldAffixAssignmentExpression(expression))
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

            is EmptyExpression, is SymbolTypeExpression, is ArrayTypeExpression ->
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
    ): List<Instruction> = buildList {
        addAll(unfoldExpression(SymbolExpression(expression.symbolToken)))
        if (expression.indexExpressions.isNotEmpty()) {
            expression.indexExpressions.forEach { addAll(unfoldExpression(it.expression)) }
            add(DuplicateLastStackItems(1 + expression.indexExpressions.size))
            add(ExecuteArrayIndexGetExpression(expression.indexExpressions.size))
        }

        if (expression is PostfixAssignmentExpression) add(SaveFromStack)

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

        if (expression is PrefixAssignmentExpression) add(SaveFromStack)

        if (expression.indexExpressions.isNotEmpty()) {
            add(ExecuteArrayIndexSetExpression(expression.indexExpressions.size))
        }

        add(AssignVariable(expression.symbolToken.value))
        add(RestoreToStack)
    }
}
