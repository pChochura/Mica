package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

internal fun List<Type>.commonSupertype(): Type {
    if (isEmpty()) return UndefinedType

    var commonSupertypes = first().superTypes
    for (i in 1 until size) {
        val currentTypeSupertypes = get(i).superTypes
        commonSupertypes = commonSupertypes.intersect(currentTypeSupertypes)
    }

    return commonSupertypes.firstOrNull() ?: UndefinedType
}

internal fun commonSupertype(vararg types: Type): Type {
    if (types.isEmpty()) return UndefinedType

    var commonSupertypes = types.first().superTypes
    for (i in 1 until types.size) {
        val currentTypeSupertypes = types[i].superTypes
        commonSupertypes = commonSupertypes.intersect(currentTypeSupertypes)
    }

    return commonSupertypes.firstOrNull() ?: UndefinedType
}

internal fun Type.replaceTypeParameter(type: Type): Type = when (this) {
    is ArrayType -> ArrayType(elementType.replaceTypeParameter(type))
    is SetType -> SetType(elementType.replaceTypeParameter(type))
    is MapType -> MapType(keyType.replaceTypeParameter(type), valueType.replaceTypeParameter(type))
    is EmptyCustomType -> type
    else -> this
}

internal fun Type.isTypeParameter(): Boolean = when (this) {
    is ArrayType -> elementType.isTypeParameter()
    is SetType -> elementType.isTypeParameter()
    is MapType -> keyType.isTypeParameter() || valueType.isTypeParameter()
    is EmptyCustomType -> true
    else -> false
}

internal fun Type.inferTypeParameter(type: Type): Type? = when (this) {
    is ArrayType -> type.superTypes.filterIsInstance<ArrayType>().firstOrNull()
        ?.let { elementType.inferTypeParameter(it.elementType) }

    is SetType -> type.superTypes.filterIsInstance<SetType>().firstOrNull()
        ?.let { elementType.inferTypeParameter(it.elementType) }

    is MapType -> type.superTypes.filterIsInstance<MapType>().firstOrNull()?.let { mapType ->
        val keyType = if (keyType.isTypeParameter()) {
            keyType.inferTypeParameter(mapType.keyType)
        } else {
            null
        }
        val valueType = if (valueType.isTypeParameter()) {
            valueType.inferTypeParameter(mapType.valueType)
        } else {
            null
        }
        if (keyType == valueType) keyType
        else if (keyType != null && valueType == null) keyType
        else if (keyType == null) valueType
        else null
    }

    is EmptyCustomType -> type
    else -> null
}
