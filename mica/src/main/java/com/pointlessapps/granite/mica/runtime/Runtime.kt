package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.FunctionCall
import com.pointlessapps.granite.mica.runtime.model.FunctionDefinition
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
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteFunctionCallExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecutePropertyAccessExpression
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
import com.pointlessapps.granite.mica.runtime.model.VariableScope
import com.pointlessapps.granite.mica.runtime.model.VariableType
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    private lateinit var onOutputCallback: (String) -> Unit
    private lateinit var onInputCallback: suspend () -> String

    private lateinit var instructions: List<Instruction>
    var index = 0

    val typeDeclarations = mutableMapOf<String, Type>()
    val functionDeclarations = builtinFunctions.groupingBy { it.name to it.parameters.size }
        .aggregate { _, acc: MutableMap<List<FunctionOverload.Parameter>, FunctionDefinition>?, element, first ->
            val overload = FunctionDefinition.BuiltinFunction(element)
            if (first) {
                mutableMapOf(element.parameters to overload)
            } else {
                requireNotNull(acc).apply { put(element.parameters, overload) }
            }
        }.toMutableMap()

    val functionCallStack = mutableListOf<FunctionCall>()
    val stack = mutableListOf<VariableType>()
    var savedFromStack: VariableType? = null

    val variableScopeStack = mutableListOf(VariableScope(mutableMapOf(), null))
    val variableScope
        get() = variableScopeStack.last()

    suspend fun execute(
        onOutputCallback: (String) -> Unit,
        onInputCallback: suspend () -> String,
    ) {
        this.onOutputCallback = onOutputCallback
        this.onInputCallback = onInputCallback

        instructions = AstTraverser.traverse(rootAST)
        while (index < instructions.size) {
            if (!coroutineContext.isActive) return
            executeNextInstruction()
            index++
        }
    }

    private suspend fun executeNextInstruction() {
        when (val instruction = instructions[index]) {
            is Label -> {} // NOOP

            is DeclareFunction -> executeDeclareFunction(instruction)
            is ExecuteFunctionCallExpression -> executeFunctionCallExpression(instruction)
            is ReturnFromFunction -> executeReturnFromFunction()

            is AcceptInput -> executeAcceptInput(onInputCallback)
            is Print -> onOutputCallback(executePrint())

            is CreateCustomObject -> executeCreateCustomObject(instruction)
            is DeclareCustomObjectProperties -> executeDeclareCustomObjectProperties()

            is DuplicateLastStackItems -> stack.addAll(
                stack.subList(
                    fromIndex = stack.size - instruction.count,
                    toIndex = stack.size,
                ),
            )

            is PushToStack -> stack.add(instruction.value)
            is RestoreToStack -> stack.add(
                requireNotNull(
                    value = savedFromStack,
                    lazyMessage = { "No value to restore" },
                ),
            )

            is SaveFromStack -> savedFromStack = requireNotNull(
                value = stack.lastOrNull(),
                lazyMessage = { "No value to save" },
            )

            is Jump -> index = instruction.index - 1
            is JumpIf -> executeJumpIf(instruction)

            is ExecuteExpression -> executeExpression(instruction)
            is ExecutePropertyAccessExpression -> executePropertyAccess(instruction)
            is ExecuteArrayIndexGetExpression -> executeArrayIndexGetExpression(instruction)
            is ExecuteArrayIndexSetExpression -> executeArrayIndexSetExpression(instruction)
            is ExecuteArrayLengthExpression -> executeArrayLengthExpression()
            is ExecuteArrayLiteralExpression -> executeArrayLiteralExpression(instruction)
            is ExecuteSetLiteralExpression -> executeSetLiteralExpression(instruction)

            is ExecuteUnaryOperation -> executeUnaryOperation(instruction)
            is ExecuteBinaryOperation -> executeBinaryOperation(instruction)

            is AssignVariable -> executeAssignVariable(instruction)
            is DeclareVariable -> executeDeclareVariable(instruction)

            is DeclareType -> executeDeclareType(instruction)
            is ExecuteTypeExpression -> executeTypeExpression(instruction)
            is ExecuteTypeCoercionExpression -> executeTypeCoercionExpression()

            is DeclareScope -> variableScopeStack.add(VariableScope.from(variableScope))
            is ExitScope -> variableScopeStack.removeLastOrNull()
        }
    }
}
