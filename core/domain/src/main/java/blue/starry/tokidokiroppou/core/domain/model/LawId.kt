package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class LawId(val value: String) {
    init {
        require(value.isNotBlank()) { "LawId must not be blank." }
    }

    override fun toString(): String {
        return value
    }
}
