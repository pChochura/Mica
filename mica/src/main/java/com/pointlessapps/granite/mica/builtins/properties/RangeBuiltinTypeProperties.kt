package com.pointlessapps.granite.mica.builtins.properties

import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType

private val startIntRangeProperty = BuiltinTypePropertyDeclaration(
    name = "start",
    receiverType = IntRangeType,
    returnType = IntType,
    execute = { receiver -> receiver.asIntRangeType().start },
)

private val endIntRangeProperty = BuiltinTypePropertyDeclaration(
    name = "end",
    receiverType = IntRangeType,
    returnType = IntType,
    execute = { receiver -> receiver.asIntRangeType().endInclusive },
)

private val startRealRangeProperty = BuiltinTypePropertyDeclaration(
    name = "start",
    receiverType = RealRangeType,
    returnType = RealType,
    execute = { receiver -> receiver.asRealRangeType().start },
)

private val endRealRangeProperty = BuiltinTypePropertyDeclaration(
    name = "end",
    receiverType = RealRangeType,
    returnType = RealType,
    execute = { receiver -> receiver.asRealRangeType().endInclusive },
)

internal val rangeBuiltinTypeProperties = listOf(
    startIntRangeProperty,
    endIntRangeProperty,
    startRealRangeProperty,
    endRealRangeProperty,
)
