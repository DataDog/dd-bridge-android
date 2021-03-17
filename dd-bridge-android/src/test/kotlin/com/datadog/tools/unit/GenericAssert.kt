package com.datadog.tools.unit

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions

class GenericAssert(actual: Any) :
    AbstractAssert<GenericAssert, Any>(actual, GenericAssert::class.java) {

    fun hasField(name: String, nestedAssert: (GenericAssert) -> Unit = {}): GenericAssert {
        val field: Any = actual.getFieldValue(name)
        nestedAssert(GenericAssert(field))
        return this
    }
    fun <F> hasFieldEqualTo(name: String, expected: F): GenericAssert {
        val field: Any? = actual.getFieldValue(name)
        Assertions.assertThat(field).isEqualTo(expected)
        return this
    }

    companion object {
        fun assertThat(actual: Any): GenericAssert {
            return GenericAssert(actual)
        }
    }
}
