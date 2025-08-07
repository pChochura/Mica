package com.pointlessapps.granite.mica.model

internal class OpenEndFloatRange(
    start: Float,
    endExclusive: Float,
) : OpenEndRange<Float> {
    private val _start = start
    private val _endExclusive = endExclusive
    override val start: Float get() = _start
    override val endExclusive: Float get() = _endExclusive

    override fun contains(value: Float): Boolean = value >= _start && value < _endExclusive
    override fun isEmpty(): Boolean = !(_start < _endExclusive)

    override fun equals(other: Any?): Boolean {
        return other is OpenEndFloatRange && (isEmpty() && other.isEmpty() ||
                _start == other._start && _endExclusive == other._endExclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * _start.hashCode() + _endExclusive.hashCode()
    }

    override fun toString(): String = "$_start..<$_endExclusive"
}
