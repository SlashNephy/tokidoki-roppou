package blue.starry.tokidokiroppou

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.feature.home.ui.HomeRoute
import blue.starry.tokidokiroppou.feature.home.ui.HomeScreen
import blue.starry.tokidokiroppou.feature.settings.ui.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    val label: String,
) {
    HOME(
        route = HomeRoute(),
        icon = Icons.Default.Home,
        label = "ホーム",
    ),
    SETTINGS(
        route = SettingsRoute,
        icon = Icons.Default.Settings,
        label = "設定",
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    // コールドスタート: Intent から初期ルートを決定 (NavHost 構築前に読み取る)
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

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ウォームスタート: アプリが既に起動中に通知をタップした場合
    val currentNavController by rememberUpdatedState(navController)
    DisposableEffect(activity) {
        val listener = Consumer<Intent> { intent ->
            val lawCode = intent.getStringExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
                ?: return@Consumer
            val articleNumber = intent.getStringExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
                ?: return@Consumer
            intent.removeExtra(ArticleNotificationSender.EXTRA_LAW_CODE)
            intent.removeExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER)
            currentNavController.navigate(HomeRoute(lawCode, articleNumber)) {
                popUpTo(currentNavController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ときどき六法") },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hasRoute(destination.route::class) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
