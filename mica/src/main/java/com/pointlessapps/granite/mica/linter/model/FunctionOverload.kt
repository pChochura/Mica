package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.model.Type

internal data class FunctionOverload(
    val parameters: List<Parameter>,
    val getReturnType: (Type?, List<Type>) -> Type,
    val accessType: AccessType,
) {
    enum class AccessType {
        /**
         * The function can only be executed as a member of a type, i.e. type.method()
         */
        MEMBER_ONLY,

        /**
         * The function can only be executed as a global function, i.e. method(arg)
         */
        GLOBAL_ONLY,

        /**
         * The function can be executed as a member or
         * a global function, i.e. type.method() or method(type)
         */
        GLOBAL_AND_MEMBER;

        fun allowMemberFunctionCalls() = this != GLOBAL_ONLY
    }

    data class Parameter(
        val type: Type,
        val resolver: Resolver,
        val vararg: Boolean,
    ) {
        enum class Resolver {
            /**
             * Matches the types exactly, i.e. `[int]` does not match `[any]`
             */
            EXACT_MATCH,

            /**
             * Matches the types in a shallow matter. Arrays and sets are compared
             * only at the first level, i.e. `[[int]]` matches `[any]`
             */
            SHALLOW_MATCH,

            /**
             * Matches the types in a default way - the type must be a subtype
             * of the other, i.e. charRange matches `[char]`
             */
            SUBTYPE_MATCH
        }

        companion object {
            fun Resolver.of(type: Type, vararg: Boolean = false) = Parameter(type, this, vararg)
        }
    }
}
