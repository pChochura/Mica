package com.pointlessapps.granite.mica.builtins.properties

import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType

private val lengthProperty = BuiltinTypePropertyDeclaration(
    name = "length",
    receiverType = EmptyArrayType,
    returnType = IntType,
    execute = { receiver -> receiver.asArrayType().size.toLong() },
)

internal val arrayBuiltinTypeProperties = listOf(
    lengthProperty,
)
