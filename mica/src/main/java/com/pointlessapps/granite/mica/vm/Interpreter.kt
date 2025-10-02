package com.pointlessapps.granite.mica.vm

import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.builtins.builtinTypeProperties
import com.pointlessapps.granite.mica.compiler.errors.RunTimeException
import com.pointlessapps.granite.mica.compiler.helper.compareTo
import com.pointlessapps.granite.mica.compiler.helper.pow
import com.pointlessapps.granite.mica.compiler.model.AccessBuiltinProperty
import com.pointlessapps.granite.mica.compiler.model.AccessIndex
import com.pointlessapps.granite.mica.compiler.model.BinaryAdd
import com.pointlessapps.granite.mica.compiler.model.BinaryAnd
import com.pointlessapps.granite.mica.compiler.model.BinaryDivide
import com.pointlessapps.granite.mica.compiler.model.BinaryEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryExponent
import com.pointlessapps.granite.mica.compiler.model.BinaryGraterThan
import com.pointlessapps.granite.mica.compiler.model.BinaryGraterThanOrEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryInstruction
import com.pointlessapps.granite.mica.compiler.model.BinaryLessThan
import com.pointlessapps.granite.mica.compiler.model.BinaryLessThanOrEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryModulo
import com.pointlessapps.granite.mica.compiler.model.BinaryMultiply
import com.pointlessapps.granite.mica.compiler.model.BinaryNotEquals
import com.pointlessapps.granite.mica.compiler.model.BinaryOr
import com.pointlessapps.granite.mica.compiler.model.BinaryRange
import com.pointlessapps.granite.mica.compiler.model.BinarySubtract
import com.pointlessapps.granite.mica.compiler.model.Call
import com.pointlessapps.granite.mica.compiler.model.CallBuiltin
import com.pointlessapps.granite.mica.compiler.model.CastAs
import com.pointlessapps.granite.mica.compiler.model.CompilerInstruction
import com.pointlessapps.granite.mica.compiler.model.Dup
import com.pointlessapps.granite.mica.compiler.model.Jump
import com.pointlessapps.granite.mica.compiler.model.JumpIfFalse
import com.pointlessapps.granite.mica.compiler.model.JumpIfTrue
import com.pointlessapps.granite.mica.compiler.model.Label
import com.pointlessapps.granite.mica.compiler.model.Load
import com.pointlessapps.granite.mica.compiler.model.NewArray
import com.pointlessapps.granite.mica.compiler.model.NewMap
import com.pointlessapps.granite.mica.compiler.model.NewObject
import com.pointlessapps.granite.mica.compiler.model.NewSet
import com.pointlessapps.granite.mica.compiler.model.Pop
import com.pointlessapps.granite.mica.compiler.model.Print
import com.pointlessapps.granite.mica.compiler.model.Push
import com.pointlessapps.granite.mica.compiler.model.ReadInput
import com.pointlessapps.granite.mica.compiler.model.Return
import com.pointlessapps.granite.mica.compiler.model.Rot2
import com.pointlessapps.granite.mica.compiler.model.Rot3
import com.pointlessapps.granite.mica.compiler.model.Store
import com.pointlessapps.granite.mica.compiler.model.StoreAtIndex
import com.pointlessapps.granite.mica.compiler.model.StoreLocal
import com.pointlessapps.granite.mica.compiler.model.UnaryAdd
import com.pointlessapps.granite.mica.compiler.model.UnaryInstruction
import com.pointlessapps.granite.mica.compiler.model.UnaryNot
import com.pointlessapps.granite.mica.compiler.model.UnarySubtract
import com.pointlessapps.granite.mica.linter.mapper.getSignature
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asCharType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.asSetType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.Type
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.pow

internal class Interpreter(
    private val onOutputCallback: (String) -> Unit,
    private val onInputCallback: suspend () -> String,
) {
    private val builtinFunctionDeclarations =
        builtinFunctions.associateBy { getSignature(it.name, it.parameters) }

    private val builtinTypePropertyDeclarations =
        builtinTypeProperties.associateBy { it.receiverType to it.name }

    private val variableStack = mutableListOf<MutableMap<String, Any>>(mutableMapOf())
    private fun getVariable(identifier: Any): Any? =
        variableStack.asReversed().firstOrNull { it.containsKey(identifier) }?.get(identifier)

    private val stack = mutableListOf<Any?>()
    private val functionCallStack = mutableListOf<Int>()

    private lateinit var instructions: List<CompilerInstruction>
    private var index = 0

    suspend fun interpret(instructions: List<CompilerInstruction>) {
        this.instructions = instructions
        this.index = 0

        while (index < instructions.size) {
            if (!coroutineContext.isActive) return
            executeNextInstruction()
            index++
        }
    }

    private suspend fun executeNextInstruction() {
        when (val instruction = instructions[index]) {
            is Label -> throw RunTimeException { "Labels are not supported at runtime" }
            is Jump -> index = instruction.index - 1
            is JumpIfFalse -> executeJumpIf(instruction.index - 1, false)
            is JumpIfTrue -> executeJumpIf(instruction.index - 1, true)
            is Call -> executeCall(instruction.index - 1)
            is CallBuiltin -> executeCallBuiltin(instruction)
            is Return -> executeReturn()
            is Print -> executePrint()
            is ReadInput -> executeReadInput()
            is Pop -> executePop()
            is Dup -> executeDup()
            is Rot2 -> executeRot2()
            is Rot3 -> executeRot3()
            is Push -> executePush(instruction)
            is Load -> executeLoad(instruction)
            is Store -> executeStore(instruction)
            is StoreLocal -> executeStoreLocal(instruction)
            is StoreAtIndex -> executeStoreAtIndex()
            is CastAs -> executeCastAs(instruction)
            is NewArray -> executeNewArray(instruction)
            is NewSet -> executeNewSet(instruction)
            is NewMap -> executeNewMap(instruction)
            is NewObject -> executeNewObject(instruction)
            is AccessIndex -> executeAccessIndex()
            is AccessBuiltinProperty -> executeAccessBuiltinProperty(instruction)
            is UnaryInstruction -> executeUnaryInstruction(instruction)
            is BinaryInstruction -> executeBinaryInstruction(instruction)
        }
    }

    private fun executeJumpIf(index: Int, instructionCondition: Boolean) {
        // Condition -> None
        val condition = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to compare" },
        )
        if (condition == instructionCondition) this.index = index
    }

    private fun executeCall(index: Int) {
        // Args, TypeArg? -> Args, TypeArg?
        functionCallStack.add(this.index)
        variableStack.add(mutableMapOf())
        this.index = index
    }

    private fun executeCallBuiltin(instruction: CallBuiltin) {
        // Args, TypeArg? -> Value?
        val function = requireNotNull(
            value = builtinFunctionDeclarations[instruction.signature],
            lazyMessage = { "Builtin function ${instruction.signature} not found" },
        )
        val typeArgument = if (function.typeParameterConstraint != null) {
            requireNotNull(
                value = stack.removeLastOrNull() as? Type,
                lazyMessage = { "No type argument to call ${function.name}" },
            )
        } else {
            null
        }
        val arguments = function.parameters.mapIndexed { index, _ ->
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "No argument $index to call ${function.name}" },
            )
        }.asReversed()

        // Add the result to the stack if it's not null
        function.execute(typeArgument, arguments)?.let(stack::add)
    }

    private fun executeReturn() {
        // None -> None
        variableStack.removeLastOrNull()
        index = requireNotNull(
            value = functionCallStack.removeLastOrNull(),
            lazyMessage = { "No function to return from" },
        )
    }

    private fun executePrint() {
        // Output -> None
        onOutputCallback(
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "No value to print" },
            ).asStringType(looseConversion = true),
        )
    }

    private suspend fun executeReadInput() {
        // None -> Input
        stack.add(onInputCallback())
    }

    private fun executePop() {
        // Item -> None
        requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to pop" },
        )
    }

    private fun executeDup() {
        // Item -> Item, Item
        stack.add(
            requireNotNull(
                value = stack.lastOrNull(),
                lazyMessage = { "No value to duplicate" },
            ),
        )
    }

    private fun executeRot2() {
        // Item1, Item2 -> Item2, Item1
        stack.add(
            index = stack.lastIndex - 1,
            element = requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "No value to swap" },
            ),
        )
    }

    private fun executeRot3() {
        // Item1, Item2, Item3 -> Item3, Item1, Item2
        stack.add(
            index = stack.lastIndex - 2,
            element = requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "No value to swap" },
            ),
        )
    }

    private fun executePush(instruction: Push) {
        // None -> Value
        stack.add(
            requireNotNull(
                value = instruction.value,
                lazyMessage = { "No value to push" },
            ),
        )
    }

    private fun executeLoad(instruction: Load) {
        // None -> Value
        stack.add(
            requireNotNull(
                value = getVariable(instruction.identifier),
                lazyMessage = { "Variable ${instruction.identifier} not found" },
            ),
        )
    }

    private fun executeStore(instruction: Store) {
        // Value -> None
        val value = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to store at ${instruction.identifier}" },
        )
        variableStack.asReversed().forEach {
            if (it.containsKey(instruction.identifier)) {
                it[instruction.identifier] = value
                return
            }
        }

        variableStack.last()[instruction.identifier] = value
    }

    private fun executeStoreLocal(instruction: StoreLocal) {
        // Value -> None
        val value = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to store at ${instruction.identifier}" },
        )
        variableStack.last()[instruction.identifier] = value
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeStoreAtIndex() {
        // Symbol, Index, Value -> None
        val value = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to store at index" },
        )
        val index = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No index to store at" },
        )
        val symbol = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No symbol to store at index" },
        )
        val variable = getVariable(symbol) ?: symbol
        when (variable) {
            is List<*> -> (variable as MutableList<Any?>)[index.asIntType().toInt()] = value
            is Map<*, *> -> (variable as MutableMap<Any?, Any?>)[index] = value
            else -> throw RunTimeException { "Cannot store at index $index in $variable" }
        }
    }

    private fun executeCastAs(instruction: CastAs) {
        // Value -> Value
        val value = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No value to cast" },
        )
        stack.add(value.asType(instruction.type))
    }

    private fun executeNewArray(instruction: NewArray) {
        // Item{size} -> Array
        (1..instruction.size).map {
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing value for array index $it" },
            )
        }.asReversed().let { stack.add(it.toMutableList()) }
    }

    private fun executeNewSet(instruction: NewSet) {
        // Item{size} -> Set
        (1..instruction.size).map {
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing value for set index $it" },
            )
        }.asReversed().let { stack.add(it.toMutableSet()) }
    }

    private fun executeNewMap(instruction: NewMap) {
        // (Key, Value){size} -> Map
        (1..instruction.size).map {
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing value for map key index $it" },
            ) to requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing value for map value index $it" },
            )
        }.asReversed().let { stack.add(it.toMap().toMutableMap()) }
    }

    private fun executeNewObject(instruction: NewObject) {
        // Value{propertiesCount}, Name{propertiesCount} -> Object
        val names = (1..instruction.propertiesCount).map {
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing name for map key index $it" },
            )
        }
        val values = (1..instruction.propertiesCount).map {
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Missing value for map key index $it" },
            )
        }
        names.zip(values).asReversed().let {
            stack.add(
                it.toMap().toMutableMap<Any, Any?>().apply {
                    this[CustomType.PROPERTY.NAME.value] = instruction.name
                    this[CustomType.PROPERTY.PARENT_TYPE.value] = instruction.parentType
                },
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeAccessIndex() {
        // Symbol, Index -> Value
        val index = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No index to access at" },
        )
        val symbol = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No symbol to access at index" },
        )
        val variable = getVariable(symbol) ?: symbol
        stack.add(
            when (variable) {
                is List<*> -> variable[index.asIntType().toInt()]
                is Map<*, *> -> variable[index]
                else -> variable.asArrayType()[index.asIntType().toInt()]
            },
        )
    }

    private fun executeAccessBuiltinProperty(instruction: AccessBuiltinProperty) {
        // Symbol, Property -> Value
        val property = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No property to access" },
        )
        val propertyDeclaration = requireNotNull(
            value = builtinTypePropertyDeclarations[instruction.type to property],
            lazyMessage = { "Builtin property $property not found" },
        )
        val symbol = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No symbol to access at property" },
        )
        val variable = getVariable(symbol) ?: symbol
        stack.add(propertyDeclaration.execute(variable))
    }

    private fun executeUnaryInstruction(instruction: UnaryInstruction) {
        // Item -> Item
        when (instruction) {
            UnaryAdd -> {} // Skip
            UnaryNot -> stack.add(
                !requireNotNull(
                    value = stack.removeLastOrNull() as Boolean,
                    lazyMessage = { "No value to negate" },
                ),
            )

            UnarySubtract -> stack.add(
                requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "No value to negate" },
                ).let { if (it is Long) -it else -(it as Double) },
            )
        }
    }

    private fun executeBinaryInstruction(instruction: BinaryInstruction) {
        // Item, Item -> Item
        val rhs = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No rhs value to add" },
        )
        val lhs = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "No lhs value to add" },
        )

        stack.add(
            when (instruction) {
                BinaryAdd -> when {
                    lhs is Long && rhs is Long -> lhs + rhs
                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true) + rhs.asRealType(true)

                    lhs is String || rhs is String ->
                        lhs.asStringType() + rhs.asStringType()

                    lhs is Char || rhs is Char ->
                        lhs.asCharType().toString() + lhs.asCharType()

                    lhs is List<*> || rhs is List<*> ->
                        lhs.asArrayType() + rhs.asArrayType()

                    lhs is Set<*> || rhs is Set<*> ->
                        lhs.asSetType() + rhs.asSetType()

                    lhs is Map<*, *> || rhs is Map<*, *> ->
                        lhs.asMapType() + rhs.asMapType()

                    else -> throw RunTimeException {
                        "+ is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinarySubtract -> when {
                    lhs is Long && rhs is Long -> lhs - rhs
                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true) - rhs.asRealType(true)

                    else -> throw RunTimeException {
                        "- is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryMultiply -> when {
                    lhs is Long && rhs is Long -> lhs * rhs
                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true) * rhs.asRealType(true)

                    else -> throw RunTimeException {
                        "* is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryDivide -> when {
                    lhs is Long && rhs is Long -> lhs / rhs
                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true) / rhs.asRealType(true)

                    else -> throw RunTimeException {
                        "/ is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryExponent -> when {
                    lhs is Long && rhs is Long -> if (rhs >= 0L) {
                        lhs.pow(rhs)
                    } else {
                        lhs.asRealType(true).pow(rhs.asRealType(true))
                    }

                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true).pow(rhs.asRealType(true))

                    else -> throw RunTimeException {
                        "^ is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryModulo -> when {
                    lhs is Long && rhs is Long -> lhs % rhs
                    lhs is Double || rhs is Double ->
                        lhs.asRealType(true) % rhs.asRealType(true)

                    else -> throw RunTimeException {
                        "% is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryAnd -> when {
                    lhs is Boolean && rhs is Boolean -> lhs && rhs
                    else -> throw RunTimeException {
                        "& is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryOr -> when {
                    lhs is Boolean && rhs is Boolean -> lhs || rhs
                    else -> throw RunTimeException {
                        "| is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }

                BinaryEquals -> lhs.compareTo(rhs) == 0
                BinaryNotEquals -> lhs.compareTo(rhs) != 0
                BinaryLessThan -> lhs.compareTo(rhs) < 0
                BinaryLessThanOrEquals -> lhs.compareTo(rhs) <= 0
                BinaryGraterThan -> lhs.compareTo(rhs) > 0
                BinaryGraterThanOrEquals -> lhs.compareTo(rhs) >= 0

                BinaryRange -> when {
                    lhs is Long && rhs is Long -> LongRange(lhs, rhs)
                    lhs is Double || rhs is Double ->
                        ClosedDoubleRange(lhs.asRealType(true), rhs.asRealType(true))

                    lhs is Char && rhs is Char -> CharRange(lhs, rhs)

                    else -> throw RunTimeException {
                        ".. is not applicable to ${lhs.toType()} and ${rhs.toType()}"
                    }
                }
            },
        )
    }
}
