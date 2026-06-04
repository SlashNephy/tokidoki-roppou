package blue.starry.tokidokiroppou.core.data.api

import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawId
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val keywordSearchJson = Json { ignoreUnknownKeys = true }

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
            val lawNum = lawInfo?.get("law_num")?.jsonPrimitive?.contentOrNull ?: return null
            val promulgationDate = lawInfo["promulgation_date"]?.jsonPrimitive?.contentOrNull

            val revisions = root["revisions"]?.jsonArray
            val currentEnforced = revisions
                ?.map { it.jsonObject }
                ?.firstOrNull { it["current_revision_status"]?.jsonPrimitive?.contentOrNull == "CurrentEnforced" }

            val amendmentLawNum = currentEnforced?.get("amendment_law_num")?.jsonPrimitive?.contentOrNull
            val amendmentDate = currentEnforced?.get("amendment_promulgate_date")?.jsonPrimitive?.contentOrNull

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

    suspend fun searchLaws(query: String): List<Law> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return emptyList()
        }

        return try {
            Timber.d("Searching e-Gov laws: %s", trimmedQuery)
            val response = httpClient.get("$BASE_URL/keyword") {
                parameter("keyword", trimmedQuery)
            }
            val body = response.bodyAsText()
            parseKeywordSearchResponse(body)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search e-Gov laws for %s", trimmedQuery)
            emptyList()
        }
    }

    companion object {
        private const val BASE_URL = "https://laws.e-gov.go.jp/api/2"
    }
}

internal fun parseKeywordSearchResponse(body: String): List<Law> {
    val root = runCatching {
        keywordSearchJson.parseToJsonElement(body).asObjectOrNull()
    }.getOrNull() ?: return emptyList()
    val items = root["items"]?.asArrayOrNull() ?: return emptyList()

    return items.mapNotNull { item ->
        item.toLawOrNull()
    }.distinctBy { it.id }
}

private fun JsonElement.toLawOrNull(): Law? {
    val item = asObjectOrNull() ?: return null
    val lawInfo = item["law_info"]?.asObjectOrNull() ?: return null
    val revisionInfo = item["revision_info"]?.asObjectOrNull()
    val lawId = lawInfo["law_id"]?.asStringOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return Law(
        id = LawId(lawId),
        displayName = revisionInfo?.get("law_title")?.asStringOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: lawId,
        lawNum = lawInfo["law_num"]?.asStringOrNull(),
        category = LawCategory.OTHERS,
        isPreset = false,
        isAdded = false,
    )
}

private fun JsonElement.asObjectOrNull() = runCatching { jsonObject }.getOrNull()

private fun JsonElement.asArrayOrNull() = runCatching { jsonArray }.getOrNull()

private fun JsonElement.asStringOrNull() = runCatching { jsonPrimitive.contentOrNull }.getOrNull()
