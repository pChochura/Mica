package com.pointlessapps.granite.mica.model

internal class OpenEndDoubleRange(
    start: Double,
    endExclusive: Double
) : OpenEndRange<Double> {
    private val _start = start
    private val _endExclusive = endExclusive
    override val start: Double get() = _start
    override val endExclusive: Double get() = _endExclusive

    override fun contains(value: Double): Boolean = value >= _start && value < _endExclusive
    override fun isEmpty(): Boolean = !(_start < _endExclusive)

    override fun equals(other: Any?): Boolean {
        return other is OpenEndDoubleRange && (isEmpty() && other.isEmpty() ||
                _start == other._start && _endExclusive == other._endExclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * _start.hashCode() + _endExclusive.hashCode()
    }

    override fun toString(): String = "$_start..<$_endExclusive"
}
