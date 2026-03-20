package blue.starry.tokidokiroppou.core.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EGovLawApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getLawData(lawId: String): String {
        val url = "$BASE_URL/lawdata/$lawId"
        Timber.d("Fetching law data: %s", url)
        val response = httpClient.get(url)
        return response.bodyAsText()
    }

    companion object {
        private const val BASE_URL = "https://laws.e-gov.go.jp/api/1"
    }
}
