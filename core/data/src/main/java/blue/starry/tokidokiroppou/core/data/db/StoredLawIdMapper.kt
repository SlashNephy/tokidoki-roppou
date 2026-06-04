package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw

internal fun String.toStoredLawIdOrNull(): LawId? {
    return takeIf { it.isNotBlank() }
        ?.let { value -> PresetLaw.fromLegacyCodeName(value)?.id ?: LawId(value) }
}
