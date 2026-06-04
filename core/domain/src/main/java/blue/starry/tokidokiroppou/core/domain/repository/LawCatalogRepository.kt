package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlinx.coroutines.flow.Flow

interface LawCatalogRepository {
    fun observeLaws(): Flow<List<Law>>

    suspend fun searchEGovLaws(query: String): List<Law>

    suspend fun addLaw(law: Law, enableNotification: Boolean)

    suspend fun removeAddedLaw(lawId: LawId)

    suspend fun getLaw(lawId: LawId): Law?
}
