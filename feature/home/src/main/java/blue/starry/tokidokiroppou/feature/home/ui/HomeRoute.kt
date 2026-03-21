package blue.starry.tokidokiroppou.feature.home.ui

import kotlinx.serialization.Serializable

@Serializable
data class HomeRoute(
    val lawCode: String? = null,
    val articleNumber: String? = null,
    val supplementaryProvisionLabel: String? = null,
)
