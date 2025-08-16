package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
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
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Instruction.AcceptInput
import com.pointlessapps.granite.mica.runtime.model.Instruction.AssignVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.DeclareVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.Jump
import com.pointlessapps.granite.mica.runtime.model.Instruction.JumpIfFalse
import com.pointlessapps.granite.mica.runtime.model.Instruction.Label
import com.pointlessapps.granite.mica.runtime.model.Instruction.Print
import com.pointlessapps.granite.mica.runtime.model.Instruction.ReturnFromFunction

internal object AstTraverser {

    private data class TraversalContext(
        val currentLoopEndLabel: String?,
    )

    private var uniqueId: Int = 0
        get() {
            field = field + 1
            return field
        }

    fun traverse(root: Root): Pair<Int, List<Instruction>> {
        val context = TraversalContext(currentLoopEndLabel = null)
        var startingIndex = 0
        var entryPointFound = false
        val instructions = root.statements.flatMap {
            val traversedInstructions = traverseAst(it, context)
            if (it !is FunctionDeclarationStatement) {
                entryPointFound = true
            }
            if (!entryPointFound) {
                startingIndex += traversedInstructions.size
            }
            traversedInstructions
        }
        backPatch(instructions)

        return startingIndex to instructions
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
            } else if (instruction is JumpIfFalse) {
                instruction.index = requireNotNull(labelMap[instruction.label])
            }
        }
    }

    private fun traverseAst(
        statement: Statement,
        context: TraversalContext,
    ): List<Instruction> = when (statement) {
        is BreakStatement -> listOf(Jump(requireNotNull(context.currentLoopEndLabel)))
        is ReturnStatement -> listOfNotNull(
            statement.returnExpression?.let(Instruction::ExecuteExpression),
            ReturnFromFunction,
        )

        is LoopIfStatement -> {
            val loopId = uniqueId
            val startLoopLabel = "Loop_$loopId"
            val elseLoopLabel = "ElseLoop_$loopId"
            val endLoopLabel = "EndLoop_$loopId"

            val loopContext = context.copy(currentLoopEndLabel = endLoopLabel)
            buildList {
                add(Label(startLoopLabel))
                add(ExecuteExpression(statement.ifConditionDeclaration.ifConditionExpression))
                add(JumpIfFalse(elseLoopLabel))
                addAll(
                    statement.ifConditionDeclaration.ifBody
                        .flatMap { traverseAst(it, loopContext) },
                )
                add(Jump(startLoopLabel))
                add(Label(elseLoopLabel))
                addAll(
                    statement.elseDeclaration?.elseBody
                        ?.flatMap { traverseAst(it, loopContext) }.orEmpty(),
                )
                add(Label(endLoopLabel))
            }
        }

        is FunctionCallStatement -> listOf(ExecuteExpression(statement.functionCallExpression))
        is FunctionDeclarationStatement -> {
            val functionContext = TraversalContext(currentLoopEndLabel = null)
            buildList {
                // TODO add a signature label
                add(Label(statement.nameToken.value))
                addAll(
                    // All of the arguments are loaded onto the stack
                    // Assign variables in the reverse order
                    statement.parameters.asReversed().map {
                        DeclareVariable(it.nameToken.value)
                    },
                )
                addAll(statement.body.flatMap { traverseAst(it, functionContext) })
                add(ReturnFromFunction)
            }
        }

        is IfConditionStatement -> {
            val ifId = uniqueId
            val elseIfBaseLabel = "ElseIf_${ifId}_"
            val elseLabel = "Else_${ifId}"
            val endIfLabel = "EndIf_${ifId}"

            buildList {
                add(ExecuteExpression(statement.ifConditionDeclaration.ifConditionExpression))
                val nextLabelForIf = when {
                    !statement.elseIfConditionDeclarations.isNullOrEmpty() -> "${elseIfBaseLabel}0"
                    statement.elseDeclaration != null -> elseLabel
                    else -> endIfLabel
                }
                add(JumpIfFalse(nextLabelForIf))
                addAll(
                    statement.ifConditionDeclaration.ifBody
                        .flatMap { traverseAst(it, context) },
                )
                add(Jump(endIfLabel))

                statement.elseIfConditionDeclarations?.forEachIndexed { index, elseIf ->
                    add(Label("${elseIfBaseLabel}$index"))
                    add(ExecuteExpression(elseIf.elseIfConditionExpression))
                    val nextLabelForElseIf = when {
                        index < statement.elseIfConditionDeclarations.lastIndex -> "${elseIfBaseLabel}${index + 1}"
                        statement.elseDeclaration != null -> elseLabel
                        else -> endIfLabel
                    }
                    add(JumpIfFalse(nextLabelForElseIf))
                    addAll(elseIf.elseIfBody.flatMap { traverseAst(it, context) })
                    add(Jump(endIfLabel))
                }

                add(Label(elseLabel))
                addAll(
                    statement.elseDeclaration?.elseBody
                        ?.flatMap { traverseAst(it, context) }.orEmpty(),
                )
                add(Label(endIfLabel))
            }
        }

        is AssignmentStatement -> listOf(
            ExecuteExpression(statement.rhs),
            AssignVariable(statement.lhsToken.value),
        )

        is VariableDeclarationStatement -> listOf(
            ExecuteExpression(statement.typeExpression),
            ExecuteExpression(statement.rhs),
            DeclareVariable(statement.lhsToken.value),
        )

        is ExpressionStatement -> listOf(
            ExecuteExpression(statement.expression),
        )

        is UserInputCallStatement -> listOf(
            AcceptInput,
            AssignVariable(statement.contentToken.value),
        )

        is UserOutputCallStatement -> listOf(
            ExecuteExpression(statement.contentExpression),
            Print,
        )
    }
}
