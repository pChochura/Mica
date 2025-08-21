package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
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
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Instruction.AcceptInput
import com.pointlessapps.granite.mica.runtime.model.Instruction.AssignVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareFunction
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareScope
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.DuplicateLastStackItem
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayIndexExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLiteralExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteBinaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteFunctionCallExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteUnaryOperation
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExitScope
import com.pointlessapps.granite.mica.runtime.model.Instruction.Jump
import com.pointlessapps.granite.mica.runtime.model.Instruction.JumpIf
import com.pointlessapps.granite.mica.runtime.model.Instruction.Label
import com.pointlessapps.granite.mica.runtime.model.Instruction.Print
import com.pointlessapps.granite.mica.runtime.model.Instruction.PushToStack
import com.pointlessapps.granite.mica.runtime.model.Instruction.ReturnFromFunction
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

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

        is ReturnStatement ->
            (1..context.scopeLevel).map { ExitScope }
                .plus(statement.returnExpression?.let(::unfoldExpression).orEmpty())
                .plus(ReturnFromFunction)

        is LoopIfStatement -> traverseLoopIfStatement(statement, context)
        is FunctionCallStatement -> unfoldExpression(statement.functionCallExpression)
        is FunctionDeclarationStatement -> traverseFunctionDeclarationStatement(statement)
        is IfConditionStatement -> traverseIfConditionStatement(statement, context)
        is AssignmentStatement -> unfoldExpression(statement.rhs)
            .plus(AssignVariable(statement.lhsToken.value))

        is VariableDeclarationStatement -> unfoldExpression(statement.rhs)
            .plus(PushToStack(statement.typeExpression.toType().toVariable(null)))
            .plus(DeclareVariable(statement.lhsToken.value))

        is ExpressionStatement -> unfoldExpression(statement.expression)

        is UserInputCallStatement -> listOf(
            AcceptInput,
            AssignVariable(statement.contentToken.value),
        )

        is UserOutputCallStatement -> unfoldExpression(statement.contentExpression).plus(Print)
    }

    private fun traverseLoopIfStatement(
        statement: LoopIfStatement,
        context: TraversalContext,
    ): List<Instruction> {
        val loopId = uniqueId
        val startLoopLabel = "Loop_$loopId"
        val elseLoopLabel = "ElseLoop_$loopId"
        val endLoopLabel = "EndLoop_$loopId"

        val loopContext = TraversalContext(
            currentLoopEndLabel = endLoopLabel,
            scopeLevel = context.scopeLevel + 1,
        )
        return buildList {
            add(Label(startLoopLabel))
            addAll(unfoldExpression(statement.ifConditionDeclaration.ifConditionExpression))
            add(JumpIf(false, elseLoopLabel))
            add(DeclareScope)
            addAll(
                statement.ifConditionDeclaration.ifBody
                    .flatMap { traverseAst(it, loopContext) },
            )
            add(ExitScope)
            add(Jump(startLoopLabel))
            add(Label(elseLoopLabel))
            add(DeclareScope)
            addAll(
                statement.elseDeclaration?.elseBody
                    ?.flatMap { traverseAst(it, loopContext) }.orEmpty(),
            )
            add(ExitScope)
            add(Label(endLoopLabel))
        }
    }

    private fun traverseFunctionDeclarationStatement(
        statement: FunctionDeclarationStatement,
    ): List<Instruction> {
        val functionContext = TraversalContext(
            currentLoopEndLabel = null,
            scopeLevel = 0,
        )
        return buildList {
            val endFunctionLabel = "EndFunction_$uniqueId"

            addAll(
                statement.parameters.map {
                    PushToStack(it.typeExpression.toType().toVariable(null))
                },
            )
            add(DeclareFunction(statement.nameToken.value, statement.parameters.size))
            add(Jump(endFunctionLabel))
            addAll(
                // All of the arguments are loaded onto the stack
                // Assign variables in the reverse order
                statement.parameters.asReversed().flatMap {
                    listOf(
                        PushToStack(it.typeExpression.toType().toVariable(null)),
                        DeclareVariable(it.nameToken.value),
                    )
                },
            )
            addAll(statement.body.flatMap { traverseAst(it, functionContext) })
            add(ReturnFromFunction)
            add(Label(endFunctionLabel))
        }
    }

    private fun traverseIfConditionStatement(
        statement: IfConditionStatement,
        context: TraversalContext,
    ): List<Instruction> {
        val ifId = uniqueId
        val elseIfBaseLabel = "ElseIf_${ifId}_"
        val elseLabel = "Else_$ifId"
        val endIfLabel = "EndIf_$ifId"

        val ifContext = TraversalContext(
            currentLoopEndLabel = context.currentLoopEndLabel,
            scopeLevel = context.scopeLevel + 1,
        )
        return buildList {
            addAll(unfoldExpression(statement.ifConditionDeclaration.ifConditionExpression))
            val nextLabelForIf = when {
                !statement.elseIfConditionDeclarations.isNullOrEmpty() -> "${elseIfBaseLabel}0"
                statement.elseDeclaration != null -> elseLabel
                else -> endIfLabel
            }
            add(JumpIf(false, nextLabelForIf))
            add(DeclareScope)
            addAll(
                statement.ifConditionDeclaration.ifBody
                    .flatMap { traverseAst(it, ifContext) },
            )
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
                addAll(elseIf.elseIfBody.flatMap { traverseAst(it, ifContext) })
                add(ExitScope)
                add(Jump(endIfLabel))
            }

            add(Label(elseLabel))
            add(DeclareScope)
            addAll(
                statement.elseDeclaration?.elseBody
                    ?.flatMap { traverseAst(it, ifContext) }.orEmpty(),
            )
            add(ExitScope)
            add(Label(endIfLabel))
        }
    }

    private fun unfoldExpression(expression: Expression): List<Instruction> = when (expression) {
        is BooleanLiteralExpression, is CharLiteralExpression,
        is NumberLiteralExpression, is StringLiteralExpression,
        is SymbolExpression,
            -> listOf(ExecuteExpression(expression))

        is ParenthesisedExpression -> unfoldExpression(expression.expression)
        is ArrayLiteralExpression -> expression.elements.flatMap(::unfoldExpression)
            .plus(ExecuteArrayLiteralExpression(expression.elements.size))

        is UnaryExpression -> unfoldExpression(expression.rhs)
            .plus(ExecuteUnaryOperation(expression.operatorToken.type))

        // Short-circuit the expression if the lhs of the condition is enough
        is BinaryExpression -> if (expression.operatorToken.type == Token.Operator.Type.Or) {
            val skipOrRhsLabel = "SkipOrRhs_$uniqueId"
            unfoldExpression(expression.lhs)
                .plus(DuplicateLastStackItem)
                .plus(JumpIf(true, skipOrRhsLabel))
                .plus(unfoldExpression(expression.rhs))
                .plus(ExecuteBinaryOperation(expression.operatorToken.type))
                .plus(Label(skipOrRhsLabel))
        } else if (expression.operatorToken.type == Token.Operator.Type.And) {
            val skipAndRhsLabel = "SkipAndRhs_$uniqueId"
            unfoldExpression(expression.lhs)
                .plus(DuplicateLastStackItem)
                .plus(JumpIf(false, skipAndRhsLabel))
                .plus(unfoldExpression(expression.rhs))
                .plus(ExecuteBinaryOperation(expression.operatorToken.type))
                .plus(Label(skipAndRhsLabel))
        } else {
            unfoldExpression(expression.lhs)
                .plus(unfoldExpression(expression.rhs))
                .plus(ExecuteBinaryOperation(expression.operatorToken.type))
        }

        is ArrayIndexExpression -> unfoldExpression(expression.arrayExpression)
            .plus(unfoldExpression(expression.indexExpression))
            .plus(ExecuteArrayIndexExpression)

        is FunctionCallExpression -> expression.arguments.flatMap(::unfoldExpression)
            .plus(
                ExecuteFunctionCallExpression(
                    functionName = expression.nameToken.value,
                    argumentsCount = expression.arguments.size,
                ),
            )

        is EmptyExpression, is SymbolTypeExpression, is ArrayTypeExpression ->
            throw IllegalStateException("Such expression should not be unfolded")
    }
}
