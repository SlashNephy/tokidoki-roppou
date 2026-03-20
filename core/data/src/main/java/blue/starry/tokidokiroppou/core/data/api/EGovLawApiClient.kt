package blue.starry.tokidokiroppou.core.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class AmendmentInfo(
    val lawNum: String,
    val promulgateDate: String,
)

@Singleton
class EGovLawApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLawData(lawId: String): String {
        val url = "$BASE_URL_V1/lawdata/$lawId"
        Timber.d("Fetching law data: %s", url)
        val response = httpClient.get(url)
        return response.bodyAsText()
    }

    suspend fun getLastAmendmentInfo(lawId: String): AmendmentInfo? {
        return try {
            val url = "$BASE_URL_V2/law_revisions/$lawId"
            Timber.d("Fetching law revisions: %s", url)
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val revisions = root["revisions"]?.jsonArray ?: return null

            val currentEnforced = revisions
                .map { it.jsonObject }
                .firstOrNull { it["current_revision_status"]?.jsonPrimitive?.content == "CurrentEnforced" }
                ?: return null

            val lawNum = currentEnforced["amendment_law_num"]?.jsonPrimitive?.content
                ?: return null
            val date = currentEnforced["amendment_promulgate_date"]?.jsonPrimitive?.content
                ?: return null

            AmendmentInfo(lawNum = lawNum, promulgateDate = date)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch law revisions for %s", lawId)
            null
        }
    }

    companion object {
        private const val BASE_URL_V1 = "https://laws.e-gov.go.jp/api/1"
        private const val BASE_URL_V2 = "https://laws.e-gov.go.jp/api/2"
    }
}
