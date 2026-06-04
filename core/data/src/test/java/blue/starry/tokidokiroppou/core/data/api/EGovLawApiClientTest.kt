package blue.starry.tokidokiroppou.core.data.api

import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.test.runTest

class EGovLawApiClientTest {
    @Test
    fun searchLawsReturnsEmptyListForBlankQueryWithoutNetwork() = runTest {
        val apiClient = EGovLawApiClient(
            HttpClient(
                MockEngine {
                    fail("Blank query must not send a network request.")
                },
            ),
        )

        val laws = apiClient.searchLaws(" \n\t ")

        assertEquals(emptyList(), laws)
    }

    @Test
    fun parseKeywordSearchResponseUsesLawInfoAndRevisionInfoAndDeduplicatesByLawId() {
        val body = """
            {
              "total_count": 3,
              "items": [
                {
                  "law_info": {
                    "law_id": "129AC0000000089",
                    "law_num": "明治二十九年法律第八十九号"
                  },
                  "revision_info": {
                    "law_title": "民法",
                    "category": "民事"
                  },
                  "sentences": []
                },
                {
                  "law_info": {
                    "law_id": "129AC0000000089",
                    "law_num": "明治二十九年法律第八十九号"
                  },
                  "revision_info": {
                    "law_title": "重複した民法"
                  }
                },
                {
                  "law_info": {
                    "law_id": "999AC0000000001",
                    "law_num": "令和八年法律第一号"
                  },
                  "revision_info": {}
                }
              ]
            }
        """.trimIndent()

        val laws = parseKeywordSearchResponse(body)

        assertEquals(
            listOf(
                Law(
                    id = LawId("129AC0000000089"),
                    displayName = "民法",
                    lawNum = "明治二十九年法律第八十九号",
                    category = LawCategory.OTHERS,
                    isPreset = false,
                    isAdded = false,
                ),
                Law(
                    id = LawId("999AC0000000001"),
                    displayName = "999AC0000000001",
                    lawNum = "令和八年法律第一号",
                    category = LawCategory.OTHERS,
                    isPreset = false,
                    isAdded = false,
                ),
            ),
            laws,
        )
    }
}
