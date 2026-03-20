plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.kotlin.serialization")
}

android {
    namespace = "blue.starry.tokidokiroppou.core.domain"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
