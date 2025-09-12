package com.pointlessapps.granite.mica.helper

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
