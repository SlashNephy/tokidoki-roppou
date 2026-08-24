package blue.starry.tokidokiroppou.core.data.worker

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class ArticleWidgetRefresherTest {
    @Test
    fun refreshReturnsNoWidgetPlacedAndDoesNotTouchArticleOrUpdaterWhenNoWidgetIsPlaced() = runTest {
        val lawRepository = FakeLawRepository(articleToReturn = sampleArticle)
        val widgetUpdater = FakeArticleWidgetUpdater(hasPlacedWidget = false)
        val refresher = ArticleWidgetRefresher(
            lawRepository = lawRepository,
            settingsRepository = FakeApplicationSettingsRepository(sampleSettings),
            widgetUpdater = widgetUpdater,
        )

        val outcome = refresher.refresh()

        assertIs<ArticleWidgetRefresher.Outcome.NoWidgetPlaced>(outcome)
        assertFalse(lawRepository.getRandomArticleCalled)
        assertFalse(widgetUpdater.updateAllCalled)
    }

    @Test
    fun refreshReturnsArticleNotFoundAndDoesNotCallUpdateAllWhenLotteryFails() = runTest {
        val lawRepository = FakeLawRepository(articleToReturn = null)
        val widgetUpdater = FakeArticleWidgetUpdater(hasPlacedWidget = true)
        val refresher = ArticleWidgetRefresher(
            lawRepository = lawRepository,
            settingsRepository = FakeApplicationSettingsRepository(sampleSettings),
            widgetUpdater = widgetUpdater,
        )

        val outcome = refresher.refresh()

        assertIs<ArticleWidgetRefresher.Outcome.ArticleNotFound>(outcome)
        // 仕様上最も重要な保証: 抽選失敗時は state を更新しない (updateAll を呼ばない)
        assertFalse(widgetUpdater.updateAllCalled)
    }

    @Test
    fun refreshUpdatesWidgetAndReturnsUpdatedWhenArticleIsFound() = runTest {
        val lawRepository = FakeLawRepository(articleToReturn = sampleArticle)
        val widgetUpdater = FakeArticleWidgetUpdater(hasPlacedWidget = true)
        val refresher = ArticleWidgetRefresher(
            lawRepository = lawRepository,
            settingsRepository = FakeApplicationSettingsRepository(sampleSettings),
            widgetUpdater = widgetUpdater,
        )

        val outcome = refresher.refresh()

        assertIs<ArticleWidgetRefresher.Outcome.Updated>(outcome)
        assertTrue(widgetUpdater.updateAllCalled)
        assertEquals(sampleArticle, widgetUpdater.lastUpdatedArticle)
    }

    @Test
    fun refreshPassesEnabledLawIdsAndExcludeSupplementaryProvisionsFromSettingsToLawRepository() = runTest {
        val settings = sampleSettings.copy(
            enabledLawIds = setOf(LawId("129AC0000000089")),
            excludeSupplementaryProvisions = true,
        )
        val lawRepository = FakeLawRepository(articleToReturn = sampleArticle)
        val refresher = ArticleWidgetRefresher(
            lawRepository = lawRepository,
            settingsRepository = FakeApplicationSettingsRepository(settings),
            widgetUpdater = FakeArticleWidgetUpdater(hasPlacedWidget = true),
        )

        refresher.refresh()

        assertEquals(setOf(LawId("129AC0000000089")), lawRepository.lastLawIds)
        assertEquals(true, lawRepository.lastExcludeSupplementaryProvisions)
    }

    private companion object {
        val sampleArticle = Article(
            lawId = LawId("129AC0000000089"),
            articleNumber = "1",
            articleTitle = "第一条",
            articleCaption = "",
            paragraphs = listOf(Article.Paragraph(number = 1, text = "本文")),
        )
        val sampleSettings = ApplicationSettings()
    }

    private class FakeLawRepository(
        private val articleToReturn: Article?,
    ) : LawRepository {
        var getRandomArticleCalled: Boolean = false
            private set
        var lastLawIds: Set<LawId>? = null
            private set
        var lastExcludeSupplementaryProvisions: Boolean? = null
            private set

        override suspend fun getArticles(lawId: LawId): List<Article> = emptyList()

        override suspend fun getStructuredContent(lawId: LawId): List<LawContentItem> = emptyList()

        override suspend fun getRandomArticle(lawIds: Set<LawId>, excludeSupplementaryProvisions: Boolean): Article? {
            getRandomArticleCalled = true
            lastLawIds = lawIds
            lastExcludeSupplementaryProvisions = excludeSupplementaryProvisions
            return articleToReturn
        }

        override suspend fun getArticle(
            lawId: LawId,
            articleNumber: String,
            supplementaryProvisionLabel: String?,
        ): Article? = null

        override suspend fun getRelatedArticles(article: Article): List<Article> = emptyList()

        override suspend fun getLawMetadata(lawId: LawId): LawMetadata? = null

        override fun observeLawMetadata(): Flow<Map<LawId, LawMetadata>> = emptyFlow()

        override suspend fun searchArticles(query: String): Map<LawId, List<Article>> = emptyMap()

        override suspend fun refreshLawId(lawId: LawId): Boolean = false
    }

    private class FakeApplicationSettingsRepository(
        private val settings: ApplicationSettings,
    ) : ApplicationSettingsRepository {
        override fun observe(): Flow<ApplicationSettings> = emptyFlow()

        override suspend fun get(): ApplicationSettings = settings

        override suspend fun setNotificationIntervalMinutes(minutes: Int) = Unit

        override suspend fun setNotificationEnabled(enabled: Boolean) = Unit

        override suspend fun setLawEnabled(lawId: LawId, enabled: Boolean) = Unit

        override suspend fun setUseHalfWidthParentheses(enabled: Boolean) = Unit

        override suspend fun setExcludeSupplementaryProvisions(enabled: Boolean) = Unit

        override suspend fun setWidgetUpdateIntervalMinutes(minutes: Int) = Unit
    }

    private class FakeArticleWidgetUpdater(
        private val hasPlacedWidget: Boolean,
    ) : ArticleWidgetUpdater {
        var updateAllCalled: Boolean = false
            private set
        var lastUpdatedArticle: Article? = null
            private set

        override suspend fun updateAll(article: Article) {
            updateAllCalled = true
            lastUpdatedArticle = article
        }

        override suspend fun rerenderAll() = Unit

        override suspend fun hasPlacedWidget(): Boolean = hasPlacedWidget
    }
}
