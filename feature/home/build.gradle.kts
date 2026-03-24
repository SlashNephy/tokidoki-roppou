plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.compose")
    id("tokidokiroppou.hilt")
    id("tokidokiroppou.kotlin.serialization")
}

android {
    namespace = "blue.starry.tokidokiroppou.feature.home"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:ai"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.timber)
    debugImplementation(libs.compose.ui.tooling)
}
