package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object ArrayIndexExpressionExecutor {

    fun execute(arrayValue: Variable<*>, arrayIndex: Variable<*>): Variable<*> {
        val index = (arrayIndex.value as Long).toInt()
        val element = when (arrayValue.type) {
            is ArrayType -> (arrayValue.value as List<*>)[index] as Variable<*>
            CharRangeType -> CharType.toVariable((arrayValue.value as CharRange)[index])
            IntRangeType -> IntType.toVariable((arrayValue.value as IntRange)[index])
            RealRangeType -> RealType.toVariable((arrayValue.value as ClosedDoubleRange)[index])
            StringType -> CharType.toVariable((arrayValue.value as String)[index])
            else -> throw IllegalStateException("Unsupported type: ${arrayValue.type}")
        }

        return element
    }

    private operator fun CharRange.get(index: Int): Char {
        if (index < 0 || index > endInclusive - start) {
            throw IndexOutOfBoundsException("index: $index, length: ${endInclusive - start}")
        }

        return start + index
    }

    private operator fun IntRange.get(index: Int): Int {
        if (index < 0 || index > endInclusive - start) {
            throw IndexOutOfBoundsException("index: $index, length: ${endInclusive - start}")
        }

        return start + index
    }

    private operator fun ClosedDoubleRange.get(index: Int): Double {
        if (index < 0 || index > endInclusive - start) {
            throw IndexOutOfBoundsException("index: $index, length: ${endInclusive - start}")
        }

        return start + index
    }
}
