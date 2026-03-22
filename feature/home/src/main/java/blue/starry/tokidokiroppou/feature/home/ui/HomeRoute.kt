package blue.starry.tokidokiroppou.feature.home.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class HomeRoute(
    val lawCode: String? = null,
    val articleNumber: String? = null,
    val supplementaryProvisionLabel: String? = null,
) : NavKey
