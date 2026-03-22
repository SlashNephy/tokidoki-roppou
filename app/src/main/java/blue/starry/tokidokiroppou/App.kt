package blue.starry.tokidokiroppou

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.feature.collection.ui.CollectionRoute
import blue.starry.tokidokiroppou.feature.collection.ui.CollectionScreen
import blue.starry.tokidokiroppou.feature.home.ui.HomeRoute
import blue.starry.tokidokiroppou.feature.home.ui.HomeScreen
import blue.starry.tokidokiroppou.feature.laws.ui.LawsRoute
import blue.starry.tokidokiroppou.feature.laws.ui.LawsScreen
import blue.starry.tokidokiroppou.feature.settings.ui.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute : NavKey

enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    val label: String,
) {
    HOME(
        route = HomeRoute(),
        icon = Icons.Default.Home,
        label = "ホーム",
    ),
    LAWS(
        route = LawsRoute,
        icon = Icons.AutoMirrored.Filled.ListAlt,
        label = "法令一覧",
    ),
    COLLECTION(
        route = CollectionRoute,
        icon = Icons.Default.Bookmark,
        label = "コレクション",
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    // コールドスタート: Intent から初期ルートを決定 (バックスタック構築前に読み取る)
    val startRoute = remember {
        val intent = activity?.intent
        val lawCode = intent?.getStringExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
        val articleNumber = intent?.getStringExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
        if (lawCode != null && articleNumber != null) {
            intent.removeExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
            intent.removeExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
            HomeRoute(lawCode, articleNumber)
        } else {
            HomeRoute()
        }
    }

    val backStack = rememberNavBackStack(startRoute)

    // ウォームスタート: アプリが既に起動中に通知をタップした場合
    DisposableEffect(activity) {
        val listener = Consumer<Intent> { intent ->
            val lawCode = intent.getStringExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
                ?: return@Consumer
            val articleNumber = intent.getStringExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
                ?: return@Consumer
            intent.removeExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
            intent.removeExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
            // バックスタックをクリアしてホーム画面に遷移
            backStack.clear()
            backStack.add(HomeRoute(lawCode, articleNumber))
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    // 現在のルートを取得してタブ選択状態を判定
    // スタックのルート (最初の要素) で判定することで、詳細画面遷移中も元タブをハイライト
    val currentRoute = backStack.lastOrNull()
    val rootRoute = backStack.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ときどき六法") },
                actions = {
                    IconButton(
                        onClick = {
                            // 設定画面が既に表示中でなければ遷移
                            if (currentRoute !is SettingsRoute) {
                                backStack.add(SettingsRoute)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = rootRoute == destination.route ||
                        (destination == TopLevelDestination.HOME && rootRoute is HomeRoute)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                // タブ切り替え: バックスタックをクリアして選択先に遷移
                                backStack.clear()
                                backStack.add(destination.route)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size <= 1) {
                    activity?.finish()
                } else {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeRoute> { route ->
                    val isDetailMode = backStack.size > 1
                    HomeScreen(
                        lawCode = route.lawCode,
                        articleNumber = route.articleNumber,
                        supplementaryProvisionLabel = route.supplementaryProvisionLabel,
                        showRefreshFab = !isDetailMode,
                    )
                }
                entry<LawsRoute> {
                    LawsScreen(
                        onArticleClick = { lawCode, articleNumber, supplementaryProvisionLabel ->
                            // 条文クリック: 法令一覧をスタックに残して条文詳細画面に遷移
                            backStack.add(HomeRoute(lawCode.name, articleNumber, supplementaryProvisionLabel))
                        },
                    )
                }
                entry<CollectionRoute> {
                    CollectionScreen(
                        onArticleClick = { lawCode, articleNumber, supplementaryProvisionLabel ->
                            // 条文クリック: バックスタックをクリアしてホーム画面に遷移
                            backStack.clear()
                            backStack.add(HomeRoute(lawCode, articleNumber, supplementaryProvisionLabel))
                        },
                    )
                }
                entry<SettingsRoute> {
                    SettingsScreen()
                }
            },
        )
    }
}
