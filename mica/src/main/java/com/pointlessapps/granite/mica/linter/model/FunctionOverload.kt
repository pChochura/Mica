package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.model.Type

internal data class FunctionOverload(
    val parameterTypes: List<Type>,
    val getReturnType: (List<Type>) -> Type,
    val accessType: AccessType,
) {
    enum class AccessType {
        MEMBER_ONLY, GLOBAL_ONLY, GLOBAL_AND_MEMBER;

        fun allowMemberFunctionCalls() = this != GLOBAL_ONLY
    }
}
