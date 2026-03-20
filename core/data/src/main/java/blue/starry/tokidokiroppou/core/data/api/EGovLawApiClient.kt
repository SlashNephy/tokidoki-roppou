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

data class LawRevisionInfo(
    val lawNum: String,
    val promulgationDate: String,
    val amendmentLawNum: String?,
    val amendmentDate: String?,
)

@Singleton
class EGovLawApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLawData(lawId: String): String {
        val url = "$BASE_URL/law_data/$lawId"
        Timber.d("Fetching law data: %s", url)
        val response = httpClient.get(url)
        return response.bodyAsText()
    }

    suspend fun getLawRevisionInfo(lawId: String): LawRevisionInfo? {
        return try {
            val url = "$BASE_URL/law_revisions/$lawId"
            Timber.d("Fetching law revisions: %s", url)
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject

            val lawInfo = root["law_info"]?.jsonObject
            val lawNum = lawInfo?.get("law_num")?.jsonPrimitive?.content ?: return null
            val promulgationDate = lawInfo["promulgation_date"]?.jsonPrimitive?.content

            val revisions = root["revisions"]?.jsonArray
            val currentEnforced = revisions
                ?.map { it.jsonObject }
                ?.firstOrNull { it["current_revision_status"]?.jsonPrimitive?.content == "CurrentEnforced" }

            val amendmentLawNum = currentEnforced?.get("amendment_law_num")?.jsonPrimitive?.content
            val amendmentDate = currentEnforced?.get("amendment_promulgate_date")?.jsonPrimitive?.content

            LawRevisionInfo(
                lawNum = amendmentLawNum ?: lawNum,
                promulgationDate = promulgationDate ?: "",
                amendmentLawNum = amendmentLawNum,
                amendmentDate = if (amendmentLawNum != null) amendmentDate else null,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch law revisions for %s", lawId)
            null
        }
    }

    companion object {
        private const val BASE_URL = "https://laws.e-gov.go.jp/api/2"
    }
}
