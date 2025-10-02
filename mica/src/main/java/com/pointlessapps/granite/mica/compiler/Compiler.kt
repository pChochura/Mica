package com.pointlessapps.granite.mica.compiler

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.compiler.model.CompilerContext
import com.pointlessapps.granite.mica.compiler.model.CompilerInstruction
import com.pointlessapps.granite.mica.compiler.model.DestinationInstruction
import com.pointlessapps.granite.mica.compiler.model.Label

internal object Compiler {
    fun compile(root: Root): List<CompilerInstruction> {
        val context = CompilerContext()
        val instructions = root.statements.flatMap { traverseStatement(it, context) }

        return backPatch(instructions)
    }

    private fun backPatch(instructions: List<CompilerInstruction>): List<CompilerInstruction> {
        val labelMap = mutableMapOf<String, Int>()
        val result = mutableListOf<CompilerInstruction>()
        var currentOffset = 0
        for (instruction in instructions) {
            if (instruction is Label) {
                labelMap[instruction.label] = currentOffset
            } else {
                result.add(instruction)
                currentOffset++
            }
        }

        result.forEach { instruction ->
            if (instruction is DestinationInstruction) {
                instruction.index = requireNotNull(
                    value = labelMap[instruction.label],
                    lazyMessage = { "${instruction.label} was not found" },
                )
            }
        }

        return result
    }
}
