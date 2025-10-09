package com.pointlessapps.granite.mica.compiler.model

import com.pointlessapps.granite.mica.model.Type
import kotlin.properties.Delegates

internal sealed interface CompilerInstruction

internal sealed class DestinationInstruction(val label: String) : CompilerInstruction {
    var index by Delegates.notNull<Int>()
}

internal class Label(val label: String) : CompilerInstruction
internal class Jump(label: String) : DestinationInstruction(label)
internal class JumpIfTrue(label: String) : DestinationInstruction(label)
internal class JumpIfFalse(label: String) : DestinationInstruction(label)

internal class Call(label: String, val genericType: Type?) : DestinationInstruction(label)
internal class CallBuiltin(val signature: String) : CompilerInstruction
internal object Return : CompilerInstruction

internal class Load(val identifier: String) : CompilerInstruction
internal class Store(val identifier: String) : CompilerInstruction
internal class StoreLocal(val identifier: String) : CompilerInstruction
internal object StoreAtIndex : CompilerInstruction

internal class CastAs(val type: Type) : CompilerInstruction

internal class Push(val value: Any?) : CompilerInstruction
internal object Pop : CompilerInstruction
internal object Dup : CompilerInstruction
internal object Rot2 : CompilerInstruction
internal object Rot3 : CompilerInstruction

internal class NewArray(val size: Int) : CompilerInstruction
internal class NewSet(val size: Int) : CompilerInstruction
internal class NewMap(val size: Int) : CompilerInstruction
internal class NewObject(
    val name: String,
    val parentType: Type?,
    val propertiesCount: Int,
) : CompilerInstruction

internal object AccessIndex : CompilerInstruction
internal class AccessBuiltinProperty(val type: Type) : CompilerInstruction

internal sealed interface UnaryInstruction : CompilerInstruction
internal object UnaryAdd : UnaryInstruction
internal object UnarySubtract : UnaryInstruction
internal object UnaryNot : UnaryInstruction

internal sealed interface BinaryInstruction : CompilerInstruction
internal object BinaryAdd : BinaryInstruction
internal object BinarySubtract : BinaryInstruction
internal object BinaryMultiply : BinaryInstruction
internal object BinaryDivide : BinaryInstruction
internal object BinaryModulo : BinaryInstruction
internal object BinaryExponent : BinaryInstruction

internal object BinaryOr : BinaryInstruction
internal object BinaryAnd : BinaryInstruction

internal object BinaryEquals : BinaryInstruction
internal object BinaryNotEquals : BinaryInstruction
internal object BinaryGraterThan : BinaryInstruction
internal object BinaryLessThan : BinaryInstruction
internal object BinaryGraterThanOrEquals : BinaryInstruction
internal object BinaryLessThanOrEquals : BinaryInstruction

internal object BinaryRange : BinaryInstruction

internal object Print : CompilerInstruction
internal object ReadInput : CompilerInstruction
